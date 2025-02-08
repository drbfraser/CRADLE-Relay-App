package com.cradleplatform.cradle_vsa_sms_relay.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.cradleplatform.cradle_vsa_sms_relay.network.ApiService
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity

class DetailsViewModel : ViewModel() {
    private val apiService = ApiService.create()
    
    private val _messageDetails = MutableLiveData<SmsRelayEntity>()
    val messageDetails: LiveData<SmsRelayEntity> get() = _messageDetails

    fun getRelayEntity(requestId: Int, phoneNumber: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getMessageDetails(requestId, phoneNumber)
                if (response.isSuccessful) {
                    _messageDetails.postValue(response.body())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resendMessage(requestId: Int, phoneNumber: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val response = apiService.resendSms(requestId, phoneNumber)
                result.postValue(response.isSuccessful)
            } catch (e: Exception) {
                result.postValue(false)
            }
        }

        return result
    }
}
