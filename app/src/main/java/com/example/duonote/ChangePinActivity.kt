package com.example.duonote

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChangePinActivity : AppCompatActivity() {
    private lateinit var store: SecurityStore
    private lateinit var currentPin: EditText
    private lateinit var newPin: EditText
    private lateinit var confirmation: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SecurityStore(this)
        setContentView(R.layout.activity_change_pin)

        currentPin = findViewById(R.id.etCurrentPin)
        newPin = findViewById(R.id.etNewPin)
        confirmation = findViewById(R.id.etNewPinConfirmation)

        findViewById<android.view.View>(R.id.btnChangePinBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnSaveNewPin).setOnClickListener { saveNewPin() }
        findViewById<android.view.View>(R.id.btnCancelPinChange).setOnClickListener { finish() }
    }

    private fun saveNewPin() {
        when {
            !store.verifyPin(currentPin.text.toString()) ->
                currentPin.error = "El PIN actual no es correcto"
            !newPin.text.toString().matches(Regex("\\d{4,6}")) ->
                newPin.error = "Usa entre 4 y 6 dígitos"
            newPin.text.toString() != confirmation.text.toString() ->
                confirmation.error = "Los PIN no coinciden"
            else -> {
                store.savePin(newPin.text.toString())
                Toast.makeText(this, "PIN actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
