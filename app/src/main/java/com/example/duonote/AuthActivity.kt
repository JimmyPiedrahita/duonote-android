package com.example.duonote

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AuthActivity : FragmentActivity() {
    private lateinit var securityStore: SecurityStore
    private lateinit var pinInput: EditText
    private lateinit var confirmationInput: EditText
    private lateinit var primaryButton: MaterialButton
    private lateinit var biometricButton: MaterialButton
    private lateinit var logoutButton: TextView
    private lateinit var eyebrow: TextView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var pinLabel: TextView
    private lateinit var confirmLabel: TextView

    private var mode: String = MODE_APP_ENTRY
    private var setupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        securityStore = SecurityStore(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_APP_ENTRY
        setupMode = intent.getBooleanExtra(EXTRA_SETUP, false)

        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.authRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()
        configureScreen()
        if (!setupMode) offerBiometric()
    }

    private fun bindViews() {
        pinInput = findViewById(R.id.etAuthPin)
        confirmationInput = findViewById(R.id.etAuthConfirmation)
        primaryButton = findViewById(R.id.btnAuthPrimary)
        biometricButton = findViewById(R.id.btnAuthBiometric)
        logoutButton = findViewById(R.id.btnAuthLogout)
        eyebrow = findViewById(R.id.tvAuthEyebrow)
        title = findViewById(R.id.tvAuthTitle)
        subtitle = findViewById(R.id.tvAuthSubtitle)
        pinLabel = findViewById(R.id.tvAuthPinLabel)
        confirmLabel = findViewById(R.id.tvAuthConfirmLabel)
    }

    private fun configureScreen() {
        if (setupMode) {
            eyebrow.text = "PROTECCIÓN DE ACCESO"
            title.text = "Crea tu PIN"
            subtitle.text = "Protege tus notas con un código de 4 a 6 dígitos."
            pinLabel.text = "CREA UN PIN"
            confirmLabel.text = "CONFIRMA TU PIN"
            primaryButton.text = "Guardar PIN"
            primaryButton.setIconResource(R.drawable.ic_lock)
            biometricButton.visibility = View.GONE
            logoutButton.visibility = View.GONE
            primaryButton.setOnClickListener { savePin() }
        } else {
            eyebrow.text = "APP BLOQUEADA"
            title.text = "Bienvenido de nuevo"
            subtitle.text = "Ingresa tu PIN para acceder a tus notas."
            pinLabel.visibility = View.GONE
            confirmLabel.visibility = View.GONE
            confirmationInput.visibility = View.GONE
            primaryButton.text = "Desbloquear"
            primaryButton.setIconResource(R.drawable.ic_input_arrow)
            primaryButton.setOnClickListener { verifyPin() }
            logoutButton.visibility = if (mode == MODE_APP_ENTRY) View.VISIBLE else View.GONE
            logoutButton.setOnClickListener { confirmLogout() }
            biometricButton.setOnClickListener { offerBiometric() }
            updateBiometricButton()
        }
    }

    private fun updateBiometricButton() {
        val visible = securityStore.isBiometricEnabled() && biometricAvailable()
        biometricButton.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun savePin() {
        val pin = pinInput.text.toString()
        when {
            !pin.matches(Regex("\\d{4,6}")) ->
                pinInput.error = "Usa entre 4 y 6 dígitos"
            pin != confirmationInput.text.toString() ->
                confirmationInput.error = "Los PIN no coinciden"
            else -> {
                securityStore.savePin(pin)
                securityStore.setWidgetRevealed(false)
                NoteWidget.updateWidget(this)
                finishSuccess(revealWidget = false)
            }
        }
    }

    private fun verifyPin() {
        if (securityStore.verifyPin(pinInput.text.toString())) {
            finishSuccess(revealWidget = mode == MODE_WIDGET_REVEAL)
        } else {
            pinInput.error = "PIN incorrecto"
        }
    }

    private fun offerBiometric() {
        if (!securityStore.isBiometricEnabled() || !biometricAvailable()) {
            updateBiometricButton()
            return
        }

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

    private fun biometricAvailable() =
        BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Cerrar sesión")
            .setMessage("¿Quieres cerrar la sesión de DuoNote en este dispositivo?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ -> performLogout() }
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            QRDataStore(this@AuthActivity).clearQRContent()
            securityStore.clear()
            AppSecuritySession.appUnlocked = false
            stopService(Intent(this@AuthActivity, FirebaseListenerService::class.java))
            NoteWidget.updateWidget(this@AuthActivity)

            val intent = Intent(this@AuthActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
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
