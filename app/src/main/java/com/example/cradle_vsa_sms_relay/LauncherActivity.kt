package com.example.cradle_vsa_sms_relay

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class LauncherActivity : AppCompatActivity() {
    var authServer = "https://cmpt373.csil.sfu.ca:8048/api/user/auth";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        setupVolley()
    }

    private fun setupVolley() {

        val que= Volley.newRequestQueue(this);
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST,authServer,
            null,Response.Listener { response ->
                val sharedpref = this.getSharedPreferences("AUTH", Context.MODE_PRIVATE)
                val editer = sharedpref.edit()
                editer.putString("TOKEN",response.getString("token"))
                editer.putString("USER_ID",response.getString("userId"))
                editer.apply()
                startActivity()
            },
            Response.ErrorListener { error ->
                error.printStackTrace()

            } )
        que.add(jsonObjectRequest)
    }

    private fun startActivity() {
        val intent = Intent(this,MainActivity::class.java)
        startActivity(intent)
    }
}
