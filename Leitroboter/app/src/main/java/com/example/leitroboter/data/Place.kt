package com.example.leitroboter.data

data class Place(
    // Identificador compartido con el servidor.
    val id: String,
    val name: String,
    val roomNumber: String?,
    val mapImageRes: Int,
    val description: String,
    // Indicaciones desde el ascensor, con la persona de frente a él (ver README).
    val directions: String,
    // Animación (res/raw) con la que Pepper señala el primer giro, o null si no hay gesto.
    val gestureAnimationRes: Int?,
    val aliases: List<String>
)
