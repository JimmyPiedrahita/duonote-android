package com.example.harmoninote

object NotaRepository {
    private val notas = mutableListOf(
        Nota("1", "Comprar leche")
    )

    fun obtenerNotas(): List<Nota> = notas

    fun eliminarNotaPorId(id: String) {
        notas.removeAll { it.id == id }
    }
}
