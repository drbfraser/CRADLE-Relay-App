package com.cradle.cradle_vsa_sms_relay.fragments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.activities.LauncherActivity


class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val reuploadListKey = getString(R.string.reuploadListPrefKey)
            val reuploadSwitchKey = getString(R.string.reuploadSwitchPrefKey)
            val signoutKey = getString(R.string.signout)

            val pref = findPreference<ListPreference>(reuploadListKey)
            pref?.isEnabled =
                PreferenceManager.getDefaultSharedPreferences(this.context)
                    .getBoolean(reuploadSwitchKey, false)
            findPreference<SwitchPreferenceCompat>(reuploadSwitchKey)?.setOnPreferenceClickListener { preference ->
                pref?.isEnabled =
                    preference.sharedPreferences.getBoolean(reuploadSwitchKey, false)
                true
            }
            findPreference<Preference>(signoutKey)?.setOnPreferenceClickListener {
                AlertDialog.Builder(this.context!!).setTitle(signoutKey).setMessage("You will be required to sign in again")
                    .setPositiveButton("YES") { _, _ -> signout() }
                    .setNegativeButton("NO") { _, _ ->  }.show()
                true
            }

        }
        fun signout(){
            PreferenceManager.getDefaultSharedPreferences(this.context).edit().clear().apply()
            val intent = Intent(this.context,
                LauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }