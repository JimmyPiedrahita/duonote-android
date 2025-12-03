package com.example.harmoninote

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private val notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    private val onCopyClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNoteText: TextView = view.findViewById(R.id.tvNoteText)
        val btnCopyNote: ImageButton = view.findViewById(R.id.btnCopyNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvNoteText.text = note.text
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
            onNoteClick(note)
        }

        holder.btnCopyNote.setOnClickListener {
            onCopyClick(note)
        }
    }

    override fun getItemCount() = notes.size
}
