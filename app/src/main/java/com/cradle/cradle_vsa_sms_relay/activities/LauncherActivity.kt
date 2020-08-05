package com.cradle.cradle_vsa_sms_relay.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.cradle.cradle_vsa_sms_relay.network.NetworkManager
import com.cradle.neptune.network.VolleyRequests.Companion.TOKEN
import com.google.android.material.button.MaterialButton
import javax.inject.Inject

class LauncherActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var networkManager: NetworkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as MyApp).component.inject(this)
        checkForAuthentication()
        setContentView(R.layout.activity_launcher)
        setupLogin()
    }

    private fun checkForAuthentication() {
        if (sharedPreferences.contains(TOKEN)) {
            startActivity()
        }
    }

    private fun setupLogin() {

        val emailEditText = findViewById<TextView>(R.id.emailEditText)
        val passwordEdittext = findViewById<TextView>(R.id.passwordEditText)
        findViewById<MaterialButton>(R.id.loginButton).setOnClickListener {

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Logging In")
            progressDialog.setCancelable(false)
            progressDialog.show()

            networkManager.authenticateTheUser(emailEditText.text.toString(),
                passwordEdittext.text.toString()){
                progressDialog.cancel()
                if (it){
                    startActivity()
                } else {
                    findViewById<TextView>(R.id.invalidLoginText).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
