package com.cradleplatform.cradle_vsa_sms_relay.view_model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    // Tracks whether the stop-service confirmation dialog should be shown
    private val _showStopServiceDialog = MutableLiveData<Boolean>(false)
    val showStopServiceDialog: LiveData<Boolean> = _showStopServiceDialog

    // Tracks whether the SMS service is running
    private val _isServiceStarted = MutableLiveData<Boolean>(false)
    val isServiceStarted: LiveData<Boolean> = _isServiceStarted

    // Tracks the selected phone number filter
    private val _selectedPhoneNumber = MutableLiveData<String?>(null)
    val selectedPhoneNumber: LiveData<String?> = _selectedPhoneNumber

    // Tracks the selected filter type spinner index
    private val _selectedFilterIndex = MutableLiveData<Int>(0)
    val selectedFilterIndex: LiveData<Int> = _selectedFilterIndex

    fun getAllRelayRequests(): LiveData<List<RelayRequest>> {
        return repository.relayRequests
    }

    fun requestStopServiceDialog() {
        _showStopServiceDialog.value = true
    }

    fun dismissStopServiceDialog() {
        _showStopServiceDialog.value = false
    }

    fun setServiceStarted(started: Boolean) {
        _isServiceStarted.value = started
    }

    fun setSelectedPhoneNumber(phone: String?) {
        _selectedPhoneNumber.value = phone
    }

    fun setSelectedFilterIndex(index: Int) {
        _selectedFilterIndex.value = index
    }

    init {
        (application as MyApp).component.inject(this)
        relayRequest = repository.relayRequests
    }
}
