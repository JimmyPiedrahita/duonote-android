package com.example.harmoninote

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var btnScanQR: Button
    private lateinit var tvQRResult: TextView
    private lateinit var rvNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var qrDataStore: QRDataStore
    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()
    private var notesListener: ValueEventListener? = null
    private var currentQrContent: String? = null

    // Launcher para solicitar permiso de cámara (No es estrictamente necesario para GmsBarcodeScanning, pero se mantiene por si acaso)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQRScanner()
        } else {
            // GmsBarcodeScanning maneja sus propios permisos, intentamos escanear de todos modos
            startQRScanner()
        }
    }

    private fun handleQrResult(qrContent: String) {
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
                // Reiniciar el servicio para que escuche el nuevo QR
                restartFirebaseService()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this@MainActivity, "Error al activar sesión: ${exception.message}", Toast.LENGTH_SHORT).show()
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
        rvNotes = findViewById(R.id.rvNotes)
        fabAddNote = findViewById(R.id.fabAddNote)

        // Configurar RecyclerView
        noteAdapter = NoteAdapter(notesList, 
            onNoteClick = { note -> toggleNoteCompletion(note) },
            onNoteDoubleClick = { note -> deleteNote(note) },
            onCopyClick = { note -> copyNoteText(note) }
        )
        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = noteAdapter

        // Cargar el último QR escaneado si existe
        lifecycleScope.launch {
            qrDataStore.qrContent.collect { savedContent ->
                savedContent?.let {
                    tvQRResult.text = "Último QR guardado: $it"
                    loadNotes(it)
                }
            }
        }

        // Configurar el botón de escaneo
        btnScanQR.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        // Configurar FAB
        fabAddNote.setOnClickListener {
            val intent = Intent(this, DialogActivity::class.java)
            startActivity(intent)
        }

        // Iniciar servicio de Firebase
        startFirebaseService()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadNotes(qrContent: String) {
        if (currentQrContent == qrContent) return

        // Remover listener anterior si existe
        currentQrContent?.let { oldQr ->
            notesListener?.let { listener ->
                FirebaseDatabase.getInstance().getReference("Connections").child(oldQr).child("Notes")
                    .removeEventListener(listener)
            }
        }

        currentQrContent = qrContent
        val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes")

        notesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notesList.clear()
                for (notaSnapshot in snapshot.children) {
                    val text = notaSnapshot.child("Text").getValue(String::class.java)
                    val id = notaSnapshot.key
                    val timestamp = notaSnapshot.child("TimeStamp").getValue(Long::class.java)
                    val isCompleted = notaSnapshot.child("IsCompleted").getValue(Boolean::class.java)
                    notesList.add(Note(id, text, timestamp, isCompleted))
                }
                noteAdapter.notifyDataSetChanged()
                fabAddNote.visibility = View.VISIBLE
                rvNotes.visibility = View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error al cargar notas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(notesListener!!)
    }

    private fun deleteNote(note: Note) {
        val qrContent = currentQrContent ?: return
        val noteId = note.id ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes").child(noteId)
        dbRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar nota", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleNoteCompletion(note: Note) {
        val qrContent = currentQrContent ?: return
        val noteId = note.id ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes").child(noteId)
        
        dbRef.child("IsCompleted").setValue(!(note.isCompleted ?: false))
    }

    private fun copyNoteText(note: Note) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied note", note.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Nota copiada", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que el servicio esté corriendo cuando la app vuelve al primer plano
        startFirebaseService()
    }


    private fun startFirebaseService() {
        val serviceIntent = Intent(this, FirebaseListenerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun restartFirebaseService() {
        // Detener el servicio actual
        val serviceIntent = Intent(this, FirebaseListenerService::class.java)
        stopService(serviceIntent)

        // Esperar un momento y reiniciar
        android.os.Handler(mainLooper).postDelayed({
            ContextCompat.startForegroundService(this, serviceIntent)
        }, 500)
    }

    private fun checkCameraPermissionAndScan() {
        // GmsBarcodeScanning maneja los permisos automáticamente, pero mantenemos la verificación por buenas prácticas
        // o si decidimos volver a una implementación manual.
        // Para GmsBarcodeScanning, simplemente llamamos a startQRScanner()
        startQRScanner()
    }

    private fun startQRScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom() // Habilitar auto-zoom para mejorar la detección
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null) {
                    handleQrResult(rawValue)
                } else {
                    Toast.makeText(this, "No se pudo leer el contenido del QR", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCanceledListener {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al escanear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}