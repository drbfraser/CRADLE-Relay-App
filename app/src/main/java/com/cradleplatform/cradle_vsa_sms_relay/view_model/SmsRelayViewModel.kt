package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import javax.inject.Inject

class SmsRelayViewModel(application: Application) :
    AndroidViewModel(application) {

    @Inject
    lateinit var repository: SmsRelayRepository

    private val relayEntity: LiveData<List<SmsRelayEntity>>

    fun getAllRelayEntities(): LiveData<List<SmsRelayEntity>> {
        return repository.relayEntities
    }

    init {
        (application as MyApp).component.inject(this)
        relayEntity = repository.relayEntities
    }

}