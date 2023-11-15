package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.telephony.SmsManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsSenderEntity
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.SMSHttpRequestRepository
import com.cradleplatform.cradle_vsa_sms_relay.utilities.SMSFormatter
import com.cradleplatform.smsrelay.database.ReferralRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback

class SMSHttpRequestViewModel(
    private val repository: SMSHttpRequestRepository
) : ViewModel() {

    private val smsFormatter: SMSFormatter = SMSFormatter()
    private val httpsResponses = MutableLiveData<List<HTTPSResponse>>()
    val phoneNumberToRequestCounter = HashMap<String, SMSHttpRequest>()

    lateinit var referralRepository: ReferralRepository
    lateinit var smsManager: SmsManager

    var smsSenderTrackerHashMap = HashMap<String, SmsSenderEntity>()


    fun removeSMSHttpResponse(smsHTTPSResponse: HTTPSResponse) {
        synchronized(this@SMSHttpRequestViewModel) {
            httpsResponses.value?.toMutableList()?.let {
                httpsResponses.value?.toMutableList()?.let { httpsResponsesList ->
                    httpsResponsesList.remove(smsHTTPSResponse)
                    httpsResponses.value = httpsResponses.value?.filter { it != smsHTTPSResponse }
                }
            }
        }
    }

    private fun updateSMSReferralRepository(smsHttpRequest: SMSHttpRequest, isResponseSuccessful: Boolean) {
        val phoneNumber: String = smsHttpRequest.phoneNumber
        val requestCounter: String = smsHttpRequest.requestCounter
        val numMessages: Int = smsHttpRequest.numOfFragments
        viewModelScope.launch {
            for (i in 0 until numMessages) {
                var fragmentIdx = String.format("%03d", i)
                var referralEntity =
                    referralRepository.getSMSReferralEntity("$phoneNumber-$requestCounter-$fragmentIdx")
                referralEntity?.numberOfTriesUploaded = 1
                referralEntity?.isUploaded = isResponseSuccessful
                referralRepository.update(referralEntity!!)
            }
        }
    }

    private fun sendCallBackResponse(phoneNumber: String, encryptedMessage: String) {
        // sends the first part of the encrypted response back to the user
        smsManager.sendMultipartTextMessage(
            phoneNumber, null,
            smsManager.divideMessage(encryptedMessage),
            null, null
        )
    }


    fun sendSMSHttpRequestToServer(smsHttpRequest: SMSHttpRequest) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.sendSMSHttpRequestToServer(smsHttpRequest)
                    .enqueue(object : Callback<HTTPSResponse> {
                        override fun onResponse(
                            call: Call<HTTPSResponse>,
                            response: retrofit2.Response<HTTPSResponse>
                        ) {
                            if (response.isSuccessful) {
                                val httpsResponse = response.body()
                                if (httpsResponse != null) {
                                    synchronized(this@SMSHttpRequestViewModel) {
                                        // TO-DO: Storing HTTP responses to update UI later
                                        // This functionality will be added later
                                        // Update UI from here instead of using updateSMSReferralRepository
                                        // This will help add detailed error messages
                                        httpsResponses.value?.toMutableList()?.let {
                                            httpsResponses.value = it + listOf(httpsResponse)
                                        }

                                        val phoneNumber: String = smsHttpRequest.phoneNumber
                                        val requestCounter: String = smsHttpRequest.requestCounter

                                        val smsMessages = smsFormatter.formatSMS(httpsResponse.body, requestCounter.toLong())
                                        val firstMessage = smsMessages.removeAt(0)

                                        val smsSenderEntityId  = "$phoneNumber-$requestCounter"

                                        val smsSenderEntity = SmsSenderEntity(
                                            smsSenderEntityId,
                                            smsMessages,
                                            httpsResponse.code.toString(),
                                            phoneNumber,
                                            System.currentTimeMillis(),
                                            smsMessages.size,
                                            1)

                                        smsSenderTrackerHashMap[smsSenderEntityId] = smsSenderEntity

                                        sendCallBackResponse(phoneNumber, firstMessage)
                                    }
                                }
                            }
                            updateSMSReferralRepository(smsHttpRequest, response.isSuccessful)
                        }
                        override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                            updateSMSReferralRepository(smsHttpRequest, false)
                        }
                    })
            }
        }
    }
}
