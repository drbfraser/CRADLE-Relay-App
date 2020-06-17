package com.cradle.cradle_vsa_sms_relay.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.cradle.cradle_vsa_sms_relay.R
import com.cradle.cradle_vsa_sms_relay.service.SmsService.Companion.TOKEN
import com.cradle.cradle_vsa_sms_relay.service.SmsService.Companion.USER_ID
import com.cradle.cradle_vsa_sms_relay.dagger.MyApp
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import javax.inject.Inject

class LauncherActivity : AppCompatActivity() {
    var authServer = "https://cmpt373-lockdown.cs.surrey.sfu.ca/api/user/auth"

    @Inject
    lateinit var sharedPreferences: SharedPreferences




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        (application as MyApp).component.inject(this)

        checkForAuthentication()
        setupVolley()
    }

    private fun checkForAuthentication() {
        if (sharedPreferences.contains(TOKEN)){
            startActivity()
        }
    }

    private fun setupVolley() {

        val emailEditText = findViewById<TextView>(R.id.emailEditText)
        val passwordEdittext = findViewById<TextView>(R.id.passwordEditText)
        findViewById<MaterialButton>(R.id.loginButton).setOnClickListener {
            val jsonObject = JSONObject()
            jsonObject.put("email",emailEditText.text)
            jsonObject.put("password",passwordEdittext.text)
            val que= Volley.newRequestQueue(this)

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Logging In")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val jsonObjectRequest = JsonObjectRequest(Request.Method.POST,authServer,
                jsonObject,Response.Listener { response ->
                    val editer = sharedPreferences.edit()
                    editer.putString(TOKEN,response.getString("token"))
                    editer.putString(USER_ID,response.getString("userId"))
                    editer.putString("email",emailEditText.text.toString())
                    editer.apply()
                    progressDialog.cancel()
                    startActivity()
                },
                Response.ErrorListener { error ->
                    error.printStackTrace()
                    progressDialog.cancel()
                    findViewById<TextView>(R.id.invalidLoginText).visibility= View.VISIBLE
                } )
            que.add(jsonObjectRequest)
        }
    }

    private fun startActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
