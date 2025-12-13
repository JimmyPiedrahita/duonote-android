package com.example.harmoninote

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteDoubleClick: (Note) -> Unit,
    private val onCopyClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNoteText: TextView = view.findViewById(R.id.tvNoteText)
        val btnCopyNote: ImageButton = view.findViewById(R.id.btnCopyNote)
        val btnOpenLink: ImageButton = view.findViewById(R.id.btnOpenLink)
        var lastClickTime: Long = 0
        val handler = Handler(Looper.getMainLooper())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvNoteText.text = note.text

        // Detectar links
        val matcher = Patterns.WEB_URL.matcher(note.text ?: "")
        if (matcher.find()) {
            val url = matcher.group()
            holder.btnOpenLink.visibility = View.VISIBLE
            holder.btnOpenLink.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.btnOpenLink.visibility = View.GONE
        }

        // Asegurar que el texto no intercepte eventos
        holder.tvNoteText.setOnTouchListener(null)
        holder.tvNoteText.movementMethod = null
        holder.tvNoteText.isClickable = false
        holder.tvNoteText.isLongClickable = false

        val cardView = holder.itemView as com.google.android.material.card.MaterialCardView

        if (note.isCompleted == true) {
            holder.tvNoteText.paintFlags = holder.tvNoteText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvNoteText.setTextColor(holder.itemView.context.getColor(R.color.widget_text_completed))
            cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.widget_background_completed))
        } else {
            holder.tvNoteText.paintFlags = holder.tvNoteText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvNoteText.setTextColor(holder.itemView.context.getColor(R.color.widget_text_pending))
            cardView.setCardBackgroundColor(holder.itemView.context.getColor(R.color.widget_background_pending))
        }

        holder.itemView.setOnClickListener {
            handleItemClick(holder, note)
        }

        holder.btnCopyNote.setOnClickListener {
            onCopyClick(note)
        }
    }

    private fun handleItemClick(holder: NoteViewHolder, note: Note) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - holder.lastClickTime < 500) {
            holder.handler.removeCallbacksAndMessages(null)
            onNoteDoubleClick(note)
        } else {
            holder.handler.postDelayed({
                onNoteClick(note)
            }, 500)
        }
        holder.lastClickTime = currentTime
    }

    override fun getItemCount() = notes.size
}
