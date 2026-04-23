package com.example.duonote

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.util.concurrent.Executor
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rvNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var btnAddWidget: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var qrDataStore: QRDataStore
    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()
    private var notesListener: ValueEventListener? = null
    private var currentQrContent: String? = null
    
    private var isAuthenticating = false
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    companion object {
        var isUnlocked = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        qrDataStore = QRDataStore(this)

        // Check if user is logged in
        lifecycleScope.launch {
            val savedCode = qrDataStore.qrContent.first()
            if (savedCode.isNullOrEmpty()) {
                // Not logged in, redirect to login
                navigateToLogin()
                return@launch
            }
            
            setupBiometricPrompt()
            
            if (!isUnlocked) {
                showBiometricPrompt()
            } else {
                initViews()
                loadNotes(savedCode)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                         Toast.makeText(applicationContext, "Error de autenticación: $errString", Toast.LENGTH_SHORT).show()
                    }
                    finish() // Close app if authentication fails or is cancelled
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                    isAuthenticating = false
                    findViewById<View>(R.id.main).visibility = View.VISIBLE
                    
                    lifecycleScope.launch {
                        val savedCode = qrDataStore.qrContent.first()
                        initViews()
                        loadNotes(savedCode ?: "")
                    }
                    Toast.makeText(applicationContext, "Autenticación exitosa", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloqueo de DuoNote")
            .setSubtitle("Usa tu huella para acceder a tus notas")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun showBiometricPrompt() {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val isVisible = prefs.getBoolean("notes_visible", true)
        
        if (isVisible) {
            // Text is NOT hidden, app should be UNLOCKED automatically
            isUnlocked = true
            findViewById<View>(R.id.main).visibility = View.VISIBLE
            lifecycleScope.launch {
                val savedCode = qrDataStore.qrContent.first()
                initViews()
                loadNotes(savedCode ?: "")
            }
            return
        }

        // Can only show if hardware is available
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            findViewById<View>(R.id.main).visibility = View.INVISIBLE // Hide content while authenticating
            isAuthenticating = true
            biometricPrompt.authenticate(promptInfo)
        } else {
            // No biometric available, fallback to unlock state directly
            isUnlocked = true
            findViewById<View>(R.id.main).visibility = View.VISIBLE
            lifecycleScope.launch {
                val savedCode = qrDataStore.qrContent.first()
                initViews()
                loadNotes(savedCode ?: "")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isUnlocked && !isAuthenticating && this::biometricPrompt.isInitialized) {
            showBiometricPrompt()
        }
    }

    private fun initViews() {
        rvNotes = findViewById(R.id.rvNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        btnAddWidget = findViewById(R.id.btnAddWidget)
        btnLogout = findViewById(R.id.btnLogout)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)

        noteAdapter = NoteAdapter(notesList, 
            onNoteClick = { note -> toggleNoteCompletion(note) },
            onDeleteClick = { note -> deleteNote(note) },
            onCopyClick = { note -> copyNoteText(note) }
        )
        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = noteAdapter

        fabAddNote.setOnClickListener {
            val intent = Intent(this, DialogActivity::class.java)
            startActivity(intent)
        }

        btnAddWidget.setOnClickListener {
            addWidgetToHomeScreen()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        startFirebaseService()
    }

    private fun addWidgetToHomeScreen() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetProvider = ComponentName(this, NoteWidget::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
        } else {
            // Fallback for older devices
            Toast.makeText(this, "Para agregar el widget, mantén presionado en la pantalla de inicio y selecciona Widgets > DuoNote", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión? Tendrás que escanear el código QR nuevamente para conectarte.")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            // Deactivate session in Firebase
            currentQrContent?.let { qrContent ->
                val database = FirebaseDatabase.getInstance()
                val sessionRef = database.getReference("Connections").child(qrContent).child("sesion_activa")
                sessionRef.setValue(false)
            }

            // Clear local data
            qrDataStore.clearQRContent()
            
            // Stop Firebase service
            val serviceIntent = Intent(this@MainActivity, FirebaseListenerService::class.java)
            stopService(serviceIntent)

            // Navigate to login
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadNotes(qrContent: String) {
        if (currentQrContent == qrContent) return

        currentQrContent?.let { oldQr ->
            notesListener?.let { listener ->
                FirebaseDatabase.getInstance().getReference("Connections").child(oldQr).child("Notes")
                    .removeEventListener(listener)
            }
        }

        currentQrContent = qrContent
        progressBar.visibility = View.VISIBLE
        
        val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes")

        notesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notesList.clear()
                for (notaSnapshot in snapshot.children) {
                    val text = notaSnapshot.child("Text").getValue(String::class.java)
                    val id = notaSnapshot.key
                    val timestamp = notaSnapshot.child("Timestamp").getValue(Long::class.java)
                    val isCompleted = notaSnapshot.child("IsCompleted").getValue(Boolean::class.java)
                    notesList.add(Note(id, text, timestamp, isCompleted))
                }
                // Sort by timestamp descending (newest first)
                notesList.sortByDescending { it.timestamp ?: 0L }
                noteAdapter.notifyDataSetChanged()
                
                progressBar.visibility = View.GONE
                fabAddNote.visibility = View.VISIBLE
                
                // Show/hide empty state
                if (notesList.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvNotes.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvNotes.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error al cargar notas: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(notesListener!!)
    }

    private fun deleteNote(note: Note) {
        val index = notesList.indexOfFirst { it.id == note.id }
        if (index != -1) {
            notesList.removeAt(index)
            noteAdapter.notifyItemRemoved(index)

            val qrContent = currentQrContent ?: return
            val noteId = note.id ?: return
            val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes").child(noteId)
            dbRef.removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show()
                    updateEmptyState()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al eliminar nota", Toast.LENGTH_SHORT).show()
                    loadNotes(qrContent)
                }
        }
    }

    private fun toggleNoteCompletion(note: Note) {
        val index = notesList.indexOfFirst { it.id == note.id }
        if (index != -1) {
            val newStatus = !(note.isCompleted ?: false)
            val updatedNote = note.copy(isCompleted = newStatus)
            notesList[index] = updatedNote
            noteAdapter.notifyItemChanged(index)

            val qrContent = currentQrContent ?: return
            val noteId = note.id ?: return
            val dbRef = FirebaseDatabase.getInstance().getReference("Connections").child(qrContent).child("Notes").child(noteId)
            
            dbRef.child("IsCompleted").setValue(newStatus)
                .addOnFailureListener {
                    if (index < notesList.size && notesList[index].id == noteId) {
                        notesList[index] = note
                        noteAdapter.notifyItemChanged(index)
                    }
                    Toast.makeText(this, "Error al actualizar estado", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun copyNoteText(note: Note) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied note", note.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Nota copiada", Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState() {
        if (notesList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvNotes.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvNotes.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        startFirebaseService()
    }

    private fun startFirebaseService() {
        val serviceIntent = Intent(this, FirebaseListenerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
