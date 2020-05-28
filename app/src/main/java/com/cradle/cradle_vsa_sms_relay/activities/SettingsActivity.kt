package com.cradle.cradle_vsa_sms_relay.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.SmsService
import com.cradle.cradle_vsa_sms_relay.SmsService.Companion.isServiceRunningInForeground


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
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
        overridePendingTransition(R.anim.nothing, R.anim.slide_up);
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val reuploadListKey = getString(R.string.reuploadListPrefKey)
            val reuploadSwitchKey = getString(R.string.reuploadSwitchPrefKey)
            val signoutKey = getString(R.string.signout)

            val pref = findPreference<ListPreference>(reuploadListKey)
            pref?.isVisible =
                PreferenceManager.getDefaultSharedPreferences(this.context)
                    .getBoolean(reuploadSwitchKey, false)
            findPreference<SwitchPreferenceCompat>(reuploadSwitchKey)?.setOnPreferenceClickListener { preference ->
                pref?.isVisible =
                    preference.sharedPreferences.getBoolean(reuploadSwitchKey, false)
                true
            }
            findPreference<Preference>(signoutKey)?.setOnPreferenceClickListener {
                AlertDialog.Builder(this.context!!).setTitle("Sign out?").setMessage("You will be required to sign in again")
                    .setPositiveButton("YES") { _, _ -> signout() }
                    .setNegativeButton("NO") { _, _ ->  }.show()
                true
            }

        }
        fun signout(){
            PreferenceManager.getDefaultSharedPreferences(this.context).edit().clear().apply()
            //stop the service if running.
            if (isServiceRunningInForeground(context!!,SmsService::class.java)) {
                val intent1 = Intent(context, SmsService::class.java)
                intent1.action = SmsService.STOP_SERVICE
                context?.let { ContextCompat.startForegroundService(it, intent1) }
            }
            val intent = Intent(this.context,LauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}
