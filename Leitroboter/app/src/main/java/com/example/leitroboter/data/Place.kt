package com.example.leitroboter.data

class Place (
    val name: String,
    val roomNumber: String?,
    val mapImageRes: Int,
    val description: String,
    // Indicaciones desde el ascensor, separadas para que el robot pueda decirlas
    val directions: String,
    // En minúscula y sin tildes, porque se comparan exacto con la solicitud normalizada
    val aliases: List<String>
)
