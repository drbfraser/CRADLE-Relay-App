package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.RelayRequest
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import javax.inject.Inject

/**
 * view model for the main activity
 * provides live data object for the recycler view
 */

class SmsRelayViewModel(application: Application) :
    AndroidViewModel(application) {

    @Inject
    lateinit var repository: SmsRelayRepository

    private val relayRequest: LiveData<List<RelayRequest>>

    fun getAllRelayRequests(): LiveData<List<RelayRequest>> {
        return repository.relayRequests
    }

    init {
        (application as MyApp).component.inject(this)
        relayRequest = repository.relayRequests
    }
}
