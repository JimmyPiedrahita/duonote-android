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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rvNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var btnAddWidget: ImageButton
    private lateinit var btnSecurity: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var composerInput: EditText
    private lateinit var composerSend: ImageButton
    private lateinit var tvConnectionId: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var qrDataStore: QRDataStore
    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()
    private var notesListener: ValueEventListener? = null
    private var currentQrContent: String? = null
    private var appAuthInProgress = false
    private var focusComposerRequested = false

    private val appAuthLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        appAuthInProgress = false
        if (result.resultCode == RESULT_OK) continueLoading()
        else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        focusComposerRequested = intent.action == ACTION_FOCUS_COMPOSER
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

            val securityStore = SecurityStore(this@MainActivity)
            if (securityStore.hasPin() && !securityStore.isWidgetRevealed() &&
                !AppSecuritySession.appUnlocked && !appAuthInProgress) {
                appAuthInProgress = true
                appAuthLauncher.launch(SecurityIntents.unlockApp(this@MainActivity))
            } else {
                continueLoading(savedCode)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun continueLoading(savedCode: String? = null) {
        lifecycleScope.launch {
            val code = savedCode ?: qrDataStore.qrContent.first() ?: return@launch
            initViews()
            loadNotes(code)
            focusComposerIfRequested()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_FOCUS_COMPOSER) {
            focusComposerRequested = true
            focusComposerIfReady()
        }
    }

    private fun focusComposerIfRequested() {
        if (focusComposerRequested) focusComposerIfReady()
    }

    private fun focusComposerIfReady() {
        if (!::composerInput.isInitialized) return
        focusComposerRequested = false
        composerInput.requestFocus()
        composerInput.post {
            val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm?.showSoftInput(composerInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun initViews() {
        rvNotes = findViewById(R.id.rvNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        btnAddWidget = findViewById(R.id.btnAddWidget)
        btnSecurity = findViewById(R.id.btnSecurity)
        btnLogout = findViewById(R.id.btnLogout)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        composerInput = findViewById(R.id.composerInput)
        composerSend = findViewById(R.id.composerSend)
        tvConnectionId = findViewById(R.id.tvConnectionId)
        tvPendingCount = findViewById(R.id.tvPendingCount)

        noteAdapter = NoteAdapter(notesList, 
            onNoteClick = { note -> toggleNoteCompletion(note) },
            onDeleteClick = { note -> deleteNote(note) },
            onCopyClick = { note -> copyNoteText(note) }
        )
        rvNotes.layoutManager = LinearLayoutManager(this)
        rvNotes.adapter = noteAdapter

        fabAddNote.setOnClickListener {
            composerInput.requestFocus()
            focusComposerIfReady()
        }

        composerSend.setOnClickListener {
            addNoteFromComposer()
        }
        composerInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                composerSend.isEnabled = !editable.isNullOrBlank()
            }
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        btnAddWidget.setOnClickListener {
            addWidgetToHomeScreen()
        }

        btnSecurity.setOnClickListener {
            startActivity(SecurityIntents.settings(this))
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        startFirebaseService()
    }

    private fun addNoteFromComposer() {
        val noteText = composerInput.text.toString().trim()
        val qrContent = currentQrContent
        if (noteText.isEmpty() || qrContent.isNullOrEmpty()) return

        composerSend.isEnabled = false
        val db = FirebaseDatabase.getInstance()
            .getReference("Connections")
            .child(qrContent)
            .child("Notes")
        val noteId = db.push().key
        if (noteId == null) {
            composerSend.isEnabled = true
            Toast.makeText(this, "No se pudo crear la nota", Toast.LENGTH_SHORT).show()
            return
        }

        db.child(noteId).setValue(
            mapOf(
                "Text" to noteText,
                "Timestamp" to System.currentTimeMillis(),
                "IsCompleted" to false
            )
        ).addOnSuccessListener {
            composerInput.text.clear()
            composerSend.isEnabled = true
            NoteWidget.updateWidget(applicationContext)
        }.addOnFailureListener {
            composerSend.isEnabled = true
            Toast.makeText(this, "Error al agregar nota", Toast.LENGTH_SHORT).show()
        }
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
            SecurityStore(this@MainActivity).clear()
            AppSecuritySession.appUnlocked = false
            
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
                tvConnectionId.text = qrContent
                tvPendingCount.text = "${notesList.count { it.isCompleted != true }} pendientes"
                
                progressBar.visibility = View.GONE
                fabAddNote.visibility = View.GONE
                
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
        val securityStore = SecurityStore(this)
        if (securityStore.hasPin() && !securityStore.isWidgetRevealed() &&
            !AppSecuritySession.appUnlocked && !appAuthInProgress) {
            appAuthInProgress = true
            appAuthLauncher.launch(SecurityIntents.unlockApp(this))
        }
        startFirebaseService()
    }

    private fun startFirebaseService() {
        val serviceIntent = Intent(this, FirebaseListenerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val ACTION_FOCUS_COMPOSER = "com.example.duonote.action.FOCUS_COMPOSER"
    }
}
