package com.cradleplatform.sms_relay.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.cradleplatform.sms_relay.R
import com.cradleplatform.sms_relay.dagger.MyApp
import com.cradleplatform.sms_relay.network.NetworkManager
import com.cradleplatform.sms_relay.network.VolleyRequests.Companion.TOKEN
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
        setUpTogglePasswordButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTogglePasswordButton() {
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val imageButton: ImageButton = findViewById<ImageButton>(R.id.togglePasswordVisibilityImageButton)
        imageButton.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.baseline_visibility_24, null
            )
        )

        imageButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    passwordEditText.inputType = InputType.TYPE_CLASS_TEXT
                    imageButton.contentDescription = resources.getString(R.string.hide_password)
                    imageButton.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources, R.drawable.baseline_visibility_off_24, null
                        )
                    )
                    passwordEditText.setSelection(passwordEditText.text.length)
                }

                MotionEvent.ACTION_UP -> {
                    passwordEditText.transformationMethod =
                        HideReturnsTransformationMethod.getInstance()
                    passwordEditText.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    imageButton.contentDescription = resources.getString(R.string.show_password)
                    imageButton.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources, R.drawable.baseline_visibility_24, null
                        )
                    )
                    passwordEditText.setSelection(passwordEditText.text.length)
                }
            }
            true
        }
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

            networkManager.authenticateTheUser(
                emailEditText.text.toString(),
                passwordEdittext.text.toString()
            ) {
                progressDialog.cancel()
                if (it) {
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
