package com.example.duonote

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var btnScanQR: Button
    private lateinit var btnConnect: Button
    private lateinit var etCode: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var qrDataStore: QRDataStore

    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrContent = result.data?.getStringExtra(QRScannerActivity.EXTRA_QR_RESULT)
            if (qrContent != null) {
                connectWithCode(qrContent)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(this, "Se requiere permiso de cámara para escanear QR", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        qrDataStore = QRDataStore(this)
        
        // Check if user is already logged in
        lifecycleScope.launch {
            val savedCode = qrDataStore.qrContent.first()
            if (!savedCode.isNullOrEmpty()) {
                // User is already connected, go to MainActivity
                navigateToMain()
                return@launch
            }
            
            // Show login screen
            setContentView(R.layout.activity_login)
            initViews()
        }
    }

    private fun initViews() {
        btnScanQR = findViewById(R.id.btnScanQR)
        btnConnect = findViewById(R.id.btnConnect)
        etCode = findViewById(R.id.etCode)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        btnScanQR.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        btnConnect.setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Ingresa un código de conexión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectWithCode(code)
        }
    }

    private fun connectWithCode(code: String) {
        showLoading(true)
        tvStatus.text = "Conectando..."
        tvStatus.visibility = View.VISIBLE

        // Save code and activate session in Firebase
        lifecycleScope.launch {
            qrDataStore.saveQRContent(code)
        }

        val database = FirebaseDatabase.getInstance()
        val sessionRef = database.getReference("Connections").child(code).child("sesion_activa")
        
        sessionRef.setValue(true)
            .addOnSuccessListener {
                showLoading(false)
                tvStatus.text = "¡Conectado!"
                Toast.makeText(this, "Conexión exitosa", Toast.LENGTH_SHORT).show()
                
                // Start Firebase service
                val serviceIntent = Intent(this, FirebaseListenerService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
                
                // Navigate to main
                navigateToMain()
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                tvStatus.text = "Error: ${exception.message}"
                Toast.makeText(this, "Error al conectar: ${exception.message}", Toast.LENGTH_SHORT).show()
                
                // Clear saved code on failure
                lifecycleScope.launch {
                    qrDataStore.clearQRContent()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnScanQR.isEnabled = !show
        btnConnect.isEnabled = !show
        etCode.isEnabled = !show
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startQRScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java)
        qrScannerLauncher.launch(intent)
    }
}
