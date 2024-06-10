package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.app.Application
import android.provider.Telephony.Sms
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.SmsRelayEntity
import com.cradleplatform.cradle_vsa_sms_relay.repository.SmsRelayRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DetailsViewModel(application: Application): AndroidViewModel(application) {
    @Inject
    lateinit var repository: SmsRelayRepository

    fun getRelayEntity(id: String): LiveData<SmsRelayEntity>? {
        return repository.getRelayLiveData(id)
    }

    init {
        (application as MyApp).component.inject(this)
    }

}