package com.example.harmoninote

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class DialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)
        val editText = findViewById<EditText>(R.id.edit_note)
        val btnAdd = findViewById<Button>(R.id.btn_add_note)
        btnAdd.setOnClickListener {
            val noteText = editText.text.toString()
            if (noteText.isNotEmpty()) {
                val db = FirebaseDatabase.getInstance().getReference("notes")
                val noteId = db.push().key ?: return@setOnClickListener
                val currentTimestamp = System.currentTimeMillis()
                val isCompleted = false
                db.child(noteId).setValue(mapOf("Text" to noteText, "Timestamp" to currentTimestamp, "IsCompleted" to isCompleted))
                NoteWidget.updateWidget(applicationContext)
                finish()
            }
        }
    }
}
