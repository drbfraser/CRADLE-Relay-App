package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cradleplatform.cradle_vsa_sms_relay.database.SmsReferralEntity
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.SMSHttpRequestRepository
import com.cradleplatform.smsrelay.database.ReferralRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback

class SMSHttpRequestViewModel(
    private val repository: SMSHttpRequestRepository
) : ViewModel() {

    private val httpsResponses = MutableLiveData<List<HTTPSResponse>>()
    val phoneNumberToRequestCounter = HashMap<String, SMSHttpRequest>()
    lateinit var referralRepository: ReferralRepository

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

    private fun updateSMSReferralRepository(smsHttpRequest: SMSHttpRequest, isResponseSuccessful: Boolean){
        val phoneNumber: String = smsHttpRequest.phoneNumber
        val requestCounter: String = smsHttpRequest.requestCounter
        val numMessages: Int = smsHttpRequest.numOfFragments
        viewModelScope.launch {
            for (i in 0 until numMessages) {
                var fragmentIdx = String.format("%03d", i)
                var referralEntity =
                    referralRepository.getSMSReferralEntity("$phoneNumber-$requestCounter-$fragmentIdx")
                referralEntity!!.numberOfTriesUploaded =+ 1
                referralEntity!!.isUploaded = isResponseSuccessful
                referralRepository.update(referralEntity!!)
            }
        }

    }

    // figure out how to change isUploaded for SMSReferral Entity for successful request
    // update database via ReferralRepository
    // everything else id potentially done
    // repeat for failures
    fun sendSMSHttpRequestToServer(smsHttpRequest: SMSHttpRequest) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.sendSMSHttpRequestToServer(smsHttpRequest)
                    .enqueue(object : Callback<HTTPSResponse> {
                        override fun onResponse(
                            call: Call<HTTPSResponse>,
                            response: retrofit2.Response<HTTPSResponse>
                        ) {
                            var isResponseSuccessful = response.isSuccessful
                            if (isResponseSuccessful) {
                                val httpsResponse = response.body()
                                if (httpsResponse != null) {
                                    synchronized(this@SMSHttpRequestViewModel) {
                                    httpsResponses.value?.toMutableList()?.let {
                                            httpsResponses.value = it + listOf(httpsResponse)
                                        }
                                    }
                                }
                                else {
                                    isResponseSuccessful = false
                                }
                            }
                            updateSMSReferralRepository(smsHttpRequest, isResponseSuccessful)
                        }

                        override fun onFailure(call: Call<HTTPSResponse>, t: Throwable){
                            updateSMSReferralRepository(smsHttpRequest, false)
                        }
                    })
            }
        }
    }
}
