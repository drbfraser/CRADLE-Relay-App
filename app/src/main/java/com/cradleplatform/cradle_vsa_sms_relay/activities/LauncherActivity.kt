package com.cradleplatform.cradle_vsa_sms_relay.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.cradleplatform.cradle_vsa_sms_relay.R
import com.cradleplatform.cradle_vsa_sms_relay.dagger.MyApp
import com.cradleplatform.cradle_vsa_sms_relay.managers.LoginManager
import com.cradleplatform.cradle_vsa_sms_relay.network.NetworkResult
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import javax.inject.Inject

class LauncherActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var loginManager: LoginManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as MyApp).component.inject(this)
        checkForAuthentication()
        setContentView(R.layout.activity_launcher)
        setupLogin()
        setUpTogglePasswordButton()
        setupSettings()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpTogglePasswordButton() {
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val imageButton: ImageButton = findViewById<ImageButton>(R.id.togglePasswordVisibilityImageButton)
        imageButton.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.baseline_visibility_24,
                null
            )
        )

        imageButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    passwordEditText.inputType = InputType.TYPE_CLASS_TEXT
                    imageButton.contentDescription = resources.getString(R.string.hide_password)
                    imageButton.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.baseline_visibility_off_24,
                            null
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
                            resources,
                            R.drawable.baseline_visibility_24,
                            null
                        )
                    )
                    passwordEditText.setSelection(passwordEditText.text.length)
                }
            }
            true
        }
    }

    private fun checkForAuthentication() {
        if (loginManager.isLoggedIn()) {
            startActivity()
        }
    }

    private fun setupLogin() {
        val emailEditText = findViewById<TextView>(R.id.emailEditText)
        val passwordEditText = findViewById<TextView>(R.id.passwordEditText)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        findViewById<MaterialButton>(R.id.loginButton).setOnClickListener {
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                progressBar.visibility = View.GONE
                val username = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                val result = loginManager.login(username, password)
                when (result) {
                    is NetworkResult.Success -> startActivity()
                    else -> findViewById<TextView>(R.id.invalidLoginText).visibility = View.VISIBLE
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

    private fun setupSettings() {
        val settingsButton = findViewById<ImageButton>(R.id.loginSettingsButton)
        settingsButton.setOnClickListener {
            startActivity(
                Intent(this, SettingsActivity::class.java),
                ActivityOptions.makeCustomAnimation(this, R.anim.slide_down, R.anim.nothing)
                    .toBundle()
            )
        }
    }
}
