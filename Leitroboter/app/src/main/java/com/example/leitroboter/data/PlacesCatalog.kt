package com.example.leitroboter.data

import com.example.leitroboter.R

// TODO mapear el cuarto piso de la ecci y agregar los lugares a esta base de datos
object PlacesCatalog {
    val places = listOf(
        Place(
            name = "Aula de Robótica",
            mapImageRes = R.drawable.mapa_aula_robotica,
            description = "Espacio dedicado a las actividades del curso de robótica.\n Teniendo de frente al ascensor gire a la izquierda.",
            aliases = listOf(
                "aula de robotica",
                "aula robotica",
                "laboratorio de robotica",
                "robotica"
            )
        ),
        Place(
            name = "Baños",
            mapImageRes = R.drawable.mapa_banios,
            description = "Baños del cuarto piso del edificio anexo.\n Teniendo de frente al ascensor gire a la izquierda.",
            aliases = listOf(
                "baños",
                "baño",
                "banios",
                "banio"
            )
        ), Place(
            name = "Oficina",
            mapImageRes = R.drawable.mapa_oficina_1,
            description = "Oficinas de Profesores de la ECCI.\n Teniendo de frente al ascensor gire a la derecha.",
            aliases = listOf(
                "oficina",
                "oficina de profesor",
                "profesor",
                "casasola",
            )
        )
    )
}