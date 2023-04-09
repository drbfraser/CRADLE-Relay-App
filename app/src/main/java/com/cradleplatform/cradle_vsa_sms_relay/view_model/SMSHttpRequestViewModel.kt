package com.cradleplatform.cradle_vsa_sms_relay.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cradleplatform.cradle_vsa_sms_relay.model.HTTPSResponse
import com.cradleplatform.cradle_vsa_sms_relay.model.SMSHttpRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.SMSHttpRequestRepository
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
                                synchronized(this@SMSHttpRequestViewModel) {
                                    val httpsResponse = response.body()
                                    if (httpsResponse != null) {
                                        httpsResponses.value?.toMutableList()?.let {
                                            httpsResponses.value = it + listOf(httpsResponse)
                                        }
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<HTTPSResponse>, t: Throwable) {
                            TODO("Not yet implemented")
                        }
                    })
            }
        }
    }
}
