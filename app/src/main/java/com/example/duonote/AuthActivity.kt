package com.example.duonote

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class AuthActivity : FragmentActivity() {
    private lateinit var securityStore: SecurityStore
    private lateinit var pinInput: EditText
    private var mode: String = MODE_APP_ENTRY
    private var setupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityStore = SecurityStore(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_APP_ENTRY
        setupMode = intent.getBooleanExtra(EXTRA_SETUP, false)
        showContent()
        if (!setupMode) offerBiometric()
    }

    private fun showContent() {
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val title = TextView(this).apply {
            text = if (setupMode) "Crear PIN de seguridad" else "Desbloquear DuoNote"
            textSize = 22f
        }
        container.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        if (setupMode) {
            val first = pinField("PIN de 4 dígitos")
            val confirmation = pinField("Confirmar PIN")
            container.addView(first)
            container.addView(confirmation)
            val save = Button(this).apply {
                text = "Guardar PIN"
                setOnClickListener {
                    val pin = first.text.toString()
                    if (!pin.matches(Regex("\\d{4}"))) {
                        first.error = "Usa exactamente 4 dígitos"
                    } else if (pin != confirmation.text.toString()) {
                        confirmation.error = "Los PIN no coinciden"
                    } else {
                        securityStore.savePin(pin)
                        securityStore.setWidgetRevealed(false)
                        finishSuccess(revealWidget = false)
                    }
                }
            }
            container.addView(save)
        } else {
            pinInput = pinField("PIN")
            container.addView(pinInput)
            val unlock = Button(this).apply {
                text = "Desbloquear con PIN"
                setOnClickListener { verifyPin() }
            }
            container.addView(unlock)
        }
        setContentView(container)
    }

    private fun pinField(hintText: String): EditText = EditText(this).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        maxLines = 1
    }

    private fun verifyPin() {
        if (securityStore.verifyPin(pinInput.text.toString())) {
            finishSuccess(revealWidget = mode == MODE_WIDGET_REVEAL)
        } else {
            pinInput.error = "PIN incorrecto"
        }
    }

    private fun offerBiometric() {
        val manager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    finishSuccess(revealWidget = mode == MODE_WIDGET_REVEAL)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(this@AuthActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear DuoNote")
            .setSubtitle("Usa tu huella o biometría")
            .setNegativeButtonText("Usar PIN")
            .build()
        prompt.authenticate(info)
    }

    private fun finishSuccess(revealWidget: Boolean) {
        AppSecuritySession.appUnlocked = !setupMode
        if (revealWidget) securityStore.setWidgetRevealed(true)
        if (setupMode || revealWidget) NoteWidget.updateWidget(this)
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_MODE = "auth_mode"
        const val EXTRA_SETUP = "auth_setup"
        const val MODE_APP_ENTRY = "app_entry"
        const val MODE_WIDGET_REVEAL = "widget_reveal"
    }
}
