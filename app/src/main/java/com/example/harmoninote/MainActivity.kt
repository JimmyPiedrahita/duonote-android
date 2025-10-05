package com.example.harmoninote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var btnScanQR: Button
    private lateinit var tvQRResult: TextView
    private lateinit var qrDataStore: QRDataStore

    // Launcher para solicitar permiso de cámara
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(this, "Permiso de cámara necesario para escanear QR", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher para el escáner de QR
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        } else {
            val qrContent = result.contents
            tvQRResult.text = "Resultado: $qrContent"
            Toast.makeText(this, "Código QR escaneado: $qrContent", Toast.LENGTH_LONG).show()

            // Guardar en DataStore
            lifecycleScope.launch {
                qrDataStore.saveQRContent(qrContent)
                Toast.makeText(this@MainActivity, "QR guardado en DataStore", Toast.LENGTH_SHORT).show()
            }

            // Actualizar sesion_activa en Firebase Realtime Database
            val database = FirebaseDatabase.getInstance()
            val sessionRef = database.getReference("Connections").child(qrContent).child("sesion_activa")
            sessionRef.setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Sesión activada en Firebase", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this@MainActivity, "Error al activar sesión: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar DataStore
        qrDataStore = QRDataStore(this)

        // Inicializar vistas
        btnScanQR = findViewById(R.id.btnScanQR)
        tvQRResult = findViewById(R.id.tvQRResult)

        // Cargar el último QR escaneado si existe
        lifecycleScope.launch {
            qrDataStore.qrContent.collect { savedContent ->
                savedContent?.let {
                    tvQRResult.text = "Último QR guardado: $it"
                }
            }
        }

        // Configurar el botón de escaneo
        btnScanQR.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        val serviceIntent = Intent(this, FirebaseListenerService::class.java)
        startForegroundService(serviceIntent)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya otorgado, iniciar escáner
                startQRScanner()
            }
            else -> {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startQRScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Escanea un código QR")
        options.setCameraId(0)  // Usar cámara trasera
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)

        qrScannerLauncher.launch(options)
    }
}