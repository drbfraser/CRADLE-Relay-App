package com.example.cradle_vsa_sms_relay.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.cradle_vsa_sms_relay.R
import com.example.cradle_vsa_sms_relay.SmsService.Companion.AUTH
import com.example.cradle_vsa_sms_relay.SmsService.Companion.TOKEN
import com.example.cradle_vsa_sms_relay.SmsService.Companion.USER_ID
import org.json.JSONObject

class LauncherActivity : AppCompatActivity() {
    var authServer = "https://cmpt373.csil.sfu.ca:8048/api/user/auth";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        checkForAuthentication();
        setupVolley()
    }

    private fun checkForAuthentication() {
        val sharedpref = this.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
        if (sharedpref.contains(TOKEN)){
            startActivity()
        }
    }

    private fun setupVolley() {

        val emailEditText = findViewById<TextView>(R.id.emailEditText)
        val passwordEdittext = findViewById<TextView>(R.id.passwordEditText)
        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val jsonObject = JSONObject();
            jsonObject.put("email",emailEditText.text)
            jsonObject.put("password",passwordEdittext.text)
            val que= Volley.newRequestQueue(this);

            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Logging In")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val jsonObjectRequest = JsonObjectRequest(Request.Method.POST,authServer,
                jsonObject,Response.Listener { response ->
                    val sharedpref = this.getSharedPreferences(AUTH, Context.MODE_PRIVATE)
                    val editer = sharedpref.edit()
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
                    startActivity()
                } )
            que.add(jsonObjectRequest)
        }
    }

    private fun startActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
