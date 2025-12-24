package com.example.duonote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_QR_RESULT = "qr_result"
        private const val TAG = "QRScannerActivity"
    }
    
    private lateinit var previewView: PreviewView
    private lateinit var btnCancel: MaterialButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    
    private var isScanning = true
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Se requiere permiso de cámara para escanear QR", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        previewView = findViewById(R.id.previewView)
        btnCancel = findViewById(R.id.btnCancel)
        
        btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        // Configurar ML Kit Barcode Scanner (Bundled)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        checkCameraPermission()
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isScanning) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        
                        @androidx.camera.core.ExperimentalGetImage
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { value ->
                                            if (isScanning) {
                                                isScanning = false
                                                handleQrResult(value)
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error scanning barcode", e)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error binding camera use cases", e)
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun handleQrResult(qrContent: String) {
        runOnUiThread {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_QR_RESULT, qrContent)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
