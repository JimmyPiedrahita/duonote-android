package com.example.duonote

data class Note(
    val id: String? = "",
    val text: String? = "",
    val timestamp: Long? = 0,
    val isCompleted: Boolean? = false
)
