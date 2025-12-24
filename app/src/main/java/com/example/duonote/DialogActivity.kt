package com.example.duonote

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class DialogActivity : AppCompatActivity() {
    private lateinit var qrDataStore: QRDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        qrDataStore = QRDataStore(this)

        val editText = findViewById<EditText>(R.id.edit_note)
        val btnAdd = findViewById<Button>(R.id.btn_add_note)
        btnAdd.setOnClickListener {
            val noteText = editText.text.toString().trim()
            if (noteText.isNotEmpty()) {
                lifecycleScope.launch {
                    val contentQR = qrDataStore.qrContent.first() ?: "default"
                    val db = FirebaseDatabase.getInstance().getReference("Connections").child(contentQR).child("Notes")
                    val noteId = db.push().key ?: return@launch
                    val currentTimestamp = System.currentTimeMillis()
                    val isCompleted = false
                    db.child(noteId).setValue(mapOf("Text" to noteText, "Timestamp" to currentTimestamp, "IsCompleted" to isCompleted))
                    NoteWidget.updateWidget(applicationContext)
                    finish()
                }
            }
        }
    }
}
