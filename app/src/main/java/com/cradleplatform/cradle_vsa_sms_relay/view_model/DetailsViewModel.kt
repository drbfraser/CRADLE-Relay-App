package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import javax.inject.Inject

class DetailsViewModel(application: Application): AndroidViewModel(application) {
    @Inject
    lateinit var repository: SmsRelayRepository

    fun getRelayEntity(requestId: Int, phoneNumber: String): LiveData<RelayRequest>? {
        return repository.getRelayRequestLiveData(requestId, phoneNumber)
    }

    init {
        (application as MyApp).component.inject(this)
    }
}
