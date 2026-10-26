package com.example.leitroboter.data

data class Place(
    // Identificador compartido con el servidor.
    val id: String,
    val name: String,
    val roomNumber: String?,
    val mapImageRes: Int,
    val description: String,
    // Indicaciones desde el ascensor.
    val directions: String,
    val aliases: List<String>
)
