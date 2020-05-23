package com.cradle.cradle_vsa_sms_relay.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cradle.cradle_vsa_sms_relay.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class SettingBottomSheetDiaglogFragment(context: Context) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.settings_activity,container)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var settingsFragment:SettingsFragment = SettingsFragment()
        childFragmentManager.beginTransaction().replace(R.id.settings,settingsFragment).commit()

    }

}