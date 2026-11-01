package com.example.duonote

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SecurityActivity : FragmentActivity() {
    private lateinit var store: SecurityStore
    private lateinit var setupPin: EditText
    private lateinit var setupConfirmation: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        store = SecurityStore(this)
        setContentView(R.layout.activity_security)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.securityRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindStaticActions()
        renderState()
    }

    private fun bindStaticActions() {
        findViewById<View>(R.id.btnSecurityBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnCreatePin).setOnClickListener { createPin() }
        findViewById<View>(R.id.btnBiometric).setOnClickListener { toggleBiometric() }
        findViewById<View>(R.id.btnChangePin).setOnClickListener {
            startActivity(SecurityIntents.changePin(this))
        }
        findViewById<View>(R.id.btnLockNow).setOnClickListener { lockNow() }
        findViewById<View>(R.id.btnDisableSecurity).setOnClickListener { confirmDisable() }
        setupPin = findViewById(R.id.etSetupPin)
        setupConfirmation = findViewById(R.id.etSetupConfirmation)
    }

    private fun renderState() {
        val configured = store.hasPin()
        findViewById<View>(R.id.setupContent).visibility = if (configured) View.GONE else View.VISIBLE
        findViewById<View>(R.id.optionsContent).visibility = if (configured) View.VISIBLE else View.GONE
        findViewById<View>(R.id.setupHint).visibility = if (configured) View.GONE else View.VISIBLE
        findViewById<android.widget.TextView>(R.id.tvHeroTitle).setText(
            if (configured) R.string.security_protected_title else R.string.security_setup_title
        )
        findViewById<android.widget.TextView>(R.id.tvBiometricState).setText(
            if (!biometricAvailable()) R.string.security_biometric_unavailable
            else if (store.isBiometricEnabled()) R.string.security_biometric_enabled
            else R.string.security_biometric_hint
        )
        findViewById<View>(R.id.tvBiometricState).alpha = if (biometricAvailable()) 1f else .55f
        findViewById<View>(R.id.biometricToggle).isEnabled = biometricAvailable()
        findViewById<View>(R.id.biometricToggle).isSelected = store.isBiometricEnabled()
    }

    private fun createPin() {
        val pin = setupPin.text.toString()
        when {
            !pin.matches(Regex("\\d{4,6}")) -> setupPin.error = "Usa entre 4 y 6 dígitos"
            pin != setupConfirmation.text.toString() -> setupConfirmation.error = "Los PIN no coinciden"
            else -> {
                store.savePin(pin)
                store.setWidgetRevealed(false)
                NoteWidget.updateWidget(this)
                Toast.makeText(this, "Bloqueo activado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun toggleBiometric() {
        if (store.isBiometricEnabled()) {
            store.setBiometricEnabled(false)
            renderState()
        } else if (biometricAvailable()) {
            val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        store.setBiometricEnabled(true)
                        renderState()
                    }
                })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("Activar huella dactilar")
                .setSubtitle("Confirma tu identidad para usarla al desbloquear DuoNote")
                .setNegativeButtonText("Cancelar")
                .build())
        }
    }

    private fun biometricAvailable() =
        BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun lockNow() {
        store.setWidgetRevealed(false)
        AppSecuritySession.appUnlocked = false
        NoteWidget.updateWidget(this)
        startActivity(SecurityIntents.unlockApp(this))
        finish()
    }

    private fun confirmDisable() {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle("Desactivar protección")
            .setMessage("Tus notas dejarán de solicitar PIN o biometría. ¿Continuar?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desactivar") { _, _ ->
                store.clear()
                AppSecuritySession.appUnlocked = true
                NoteWidget.updateWidget(this)
                Toast.makeText(this, "Protección desactivada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }
}
