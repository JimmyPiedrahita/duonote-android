package com.example.duonote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class BiometricActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        executor = ContextCompat.getMainExecutor(this)
        
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish() // Close if error or cancelled
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    val action = intent.getStringExtra("ACTION_ON_SUCCESS")
                    if (action == "TOGGLE_VISIBILITY") {
                        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
                        val isVisible = prefs.getBoolean("notes_visible", true)
                        prefs.edit().putBoolean("notes_visible", !isVisible).apply()
                        
                        val updateIntent = Intent(this@BiometricActivity, NoteWidget::class.java).apply {
                            this.action = "ACTION_UPDATE_WIDGET_UI"
                        }
                        sendBroadcast(updateIntent)
                    }
                    
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación requerida")
            .setSubtitle("Desbloquea para ver el contenido")
            .setNegativeButtonText("Cancelar")
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
}