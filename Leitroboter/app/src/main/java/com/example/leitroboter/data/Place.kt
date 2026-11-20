package com.example.leitroboter.data

data class Place(
    // Identificador compartido con el servidor.
    val id: String,
    val name: String,
    // Nombre corto para los botones de las listas cuando el nombre no cabe; null si no hace falta.
    val shortName: String? = null,
    val roomNumber: String?,
    // Piso del edificio anexo (1 a 6). Pepper está en el piso 4 (ver Floors).
    val floor: Int,
    // Mismo tipo de lugar en varios pisos (ej. "banos"); null si el lugar es único.
    val group: String?,
    val mapImageRes: Int,
    val description: String,
    // Indicaciones desde el ascensor de su piso, con la persona de frente a él (ver README).
    val directions: String,
    // Animación (res/raw) con la que Pepper señala el primer giro, o null si no hay gesto.
    val gestureAnimationRes: Int?,
    val aliases: List<String>
)
