package com.example.harmoninote

object NotaRepository {
    private val notas = mutableListOf(
        Nota(1, "Comprar leche"),
        Nota(2, "Estudiar Kotlin"),
        Nota(3, "Llamar a la tía"),
        Nota(4, "Llamar a la tía"),
        Nota(5, "Llamar a la tía"),
        Nota(6, "Llamar a la tía"),
        Nota(7, "Llamar a la tía"),
        Nota(8, "Llamar a la tía"),
        Nota(9, "Llamar a la tía"),
        Nota(10, "Llamar a la tía"),
        Nota(11, "Llamar a la tía")
    )

    fun obtenerNotas(): List<Nota> = notas

    fun eliminarNotaPorId(id: Int) {
        notas.removeAll { it.id == id }
    }
}
