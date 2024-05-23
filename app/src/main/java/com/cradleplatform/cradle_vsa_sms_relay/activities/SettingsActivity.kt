package com.cradleplatform.cradle_vsa_sms_relay.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.model.UrlManager
import com.cradleplatform.cradle_vsa_sms_relay.network.VolleyRequests
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService
import com.cradleplatform.cradle_vsa_sms_relay.service.SmsService.Companion.isServiceRunningInForeground
import javax.inject.Inject


class SettingsActivity : AppCompatActivity() {
    @Inject
    lateinit var urlManager: UrlManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        (application as MyApp).component.inject(this)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            onBackPressed()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.nothing, R.anim.slide_up)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val reuploadListKey = getString(R.string.reuploadListPrefKey)
            val reuploadSwitchKey = getString(R.string.reuploadSwitchPrefKey)
            val signoutKey = getString(R.string.signout)
            val syncNowkey = getString(R.string.sync_now_key)
            val accountSettingsKey = getString(R.string.key_account_settings)

            val defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.requireContext())

            val isLoggedIn = defaultSharedPreferences.contains(VolleyRequests.TOKEN)

            // show/ hide pref on default
            val syncNowPref = findPreference<Preference>(syncNowkey)
            syncNowPref?.isVisible = defaultSharedPreferences.getBoolean(reuploadSwitchKey, true)

            val listPref = findPreference<ListPreference>(reuploadListKey)
            listPref?.isVisible = defaultSharedPreferences.getBoolean(reuploadSwitchKey, true)

            // setting values based on switch changes
            findPreference<SwitchPreferenceCompat>(reuploadSwitchKey)?.setOnPreferenceClickListener { preference ->
                listPref?.isVisible =
                    preference.sharedPreferences?.getBoolean(reuploadSwitchKey, false) ?: false
                syncNowPref?.isVisible = PreferenceManager.getDefaultSharedPreferences(this.requireContext())
                    .getBoolean(reuploadSwitchKey, false)
                true
            }

            val signoutPref = findPreference<Preference>(signoutKey)
            findPreference<PreferenceCategory>(accountSettingsKey)?.isVisible = isLoggedIn
            signoutPref?.isVisible = isLoggedIn
            signoutPref?.setOnPreferenceClickListener {
                AlertDialog.Builder(this.requireContext()).setTitle("Sign out?")
                    .setMessage("You will be required to sign in again")
                    .setPositiveButton("YES") { _, _ -> signout() }
                    .setNegativeButton("NO") { _, _ -> }.show()
                true
            }


            syncNowPref?.setOnPreferenceClickListener {
                if (!isServiceRunningInForeground(this.requireContext(), SmsService::class.java)) {
                    Toast.makeText(
                        this.context,
                        getString(R.string.service_not_running_sync_toast),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val x = defaultSharedPreferences.getBoolean(syncNowkey, false)
                    // always changing the value so service can listen for sharedpref change
                    // solution for now, otherwise SettingActivity needs to know about service..
                    // todo better achitecture? maybe its ok for activity to know about service?
                    defaultSharedPreferences.edit().putBoolean(syncNowkey, !x).apply()
                }
                true
            }
        }

        fun signout() {
            PreferenceManager.getDefaultSharedPreferences(this.requireContext()).edit().clear().apply()
            // stop the service if running.
            if (isServiceRunningInForeground(
                    this.requireContext(),
                    SmsService::class.java
                )
            ) {
                val intent1 = Intent(context, SmsService::class.java)
                intent1.action = SmsService.STOP_SERVICE
                context?.let { ContextCompat.startForegroundService(it, intent1) }
            }
            val intent = Intent(this.context, LauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}
