package com.example.leitroboter.data

import com.example.leitroboter.R

// Lugares del cuarto piso del edificio anexo de la ECCI
object PlacesCatalog {
    val places = listOf(
        Place(
            name = "Laboratorio de PI de Ingeniería de Software y Bases de Datos",
            roomNumber = "4-6",
            mapImageRes = R.drawable.mapa_4_6,
            description = "Laboratorio del PI de Ingeniería de Software y Bases de Datos.",
            directions = "Al salir del ascensor, gire a la izquierda y camine hacia el fondo. El laboratorio 4-6 es la puerta grande a su izquierda, al lado de la oficina 4-5.",
            aliases = listOf(
                "laboratorio de pi",
                "laboratorio pi",
                "lab de pi",
                "pi",
                "laboratorio de pi de ingenieria de software y bases de datos",
                "laboratorio de pi para ingenieria de software y bases de datos",
                "proyecto integrador",
                "laboratorio de proyecto integrador",
                "bases de datos",
                "laboratorio de bases de datos",
                "4-6",
                "46",
                "aula 4-6",
                "sala 4-6"
            )
        ),
        Place(
            name = "Oficina de la profesora Gabriela Barrantes Sliesarieva",
            roomNumber = "4-5",
            mapImageRes = R.drawable.mapa_4_5,
            description = "Oficina de la profesora y directora Gabriela.",
            directions = "Al salir del ascensor, gire a la izquierda y camine hasta el final del pasillo. La oficina 4-5 es la puerta que queda de frente.",
            aliases = listOf(
                "gabriela",
                "profesora gabriela",
                "oficina de gabriela",
                "oficina de la profesora gabriela",
                "gabriela barrantes",
                "gabriela barrantes sliesarieva",
                "barrantes",
                "gaby",
                "directora",
                "oficina de la directora",
                "direccion",
                "4-5",
                "45",
                "oficina 4-5"
            )
        ),
        Place(
            name = "Oficina del profesor Luis Gustavo Esquivel Quirós",
            roomNumber = "4-4",
            mapImageRes = R.drawable.mapa_4_4,
            description = "Oficina del profesor Luis Gustavo.",
            directions = "Al salir del ascensor, gire a la izquierda, camine hasta el final del pasillo y doble a la derecha. La oficina 4-4 es la primera puerta a su izquierda.",
            aliases = listOf(
                "luis gustavo",
                "profesor luis gustavo",
                "oficina de luis gustavo",
                "oficina del profesor luis gustavo",
                "4-4",
                "44",
                "oficina 4-4",
                "luis gustavo esquivel quiros",
                "luis gustavo esquivel",
                "esquivel",
                "luis",
                "gustavo"
            )
        ),
        Place(
            name = "Oficina del profesor Edgar Casasola Murillo",
            roomNumber = "4-3",
            mapImageRes = R.drawable.mapa_4_3,
            description = "Oficina del profesor Casasola.",
            directions = "Al salir del ascensor, gire a la izquierda, camine hasta el final del pasillo y doble a la derecha. La oficina 4-3 es la segunda puerta a su izquierda, al lado de la 4-4, en la esquina.",
            aliases = listOf(
                "casasola",
                "profesor casasola",
                "oficina de casasola",
                "oficina del profesor casasola",
                "edgar casasola",
                "edgar casasola murillo",
                "edgar",
                "4-3",
                "43",
                "oficina 4-3"
            )
        ),
        Place(
            name = "Sala de consultas",
            roomNumber = "4-2",
            mapImageRes = R.drawable.mapa_4_2,
            description = "Sala común para consultas entre profesores y estudiantes.",
            directions = "Al salir del ascensor, gire a la izquierda, camine hasta el final del pasillo y doble a la derecha. La sala 4-2 es la primera puerta a su derecha, frente a la oficina 4-4.",
            aliases = listOf(
                "sala de consultas",
                "consultas",
                "consulta",
                "sala comun",
                "sala comun para consultas",
                "4-2",
                "42",
                "sala 4-2"
            )
        ),
        Place(
            name = "Sala individual de estudio",
            roomNumber = null,
            mapImageRes = R.drawable.mapa_sala_estudio,
            description = "Sala individual de estudio.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. Es la cuarta puerta a su izquierda, al lado de la sala 4-19.",
            aliases = listOf(
                "sala individual de estudio",
                "sala individual",
                "sala de estudio",
                "estudio individual",
                "estudiar"
            )
        ),
        Place(
            name = "Comedor de profesores",
            roomNumber = "4-20",
            mapImageRes = R.drawable.mapa_4_20,
            description = "Comedor de profesores.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. El comedor (4-20) es la segunda puerta a su izquierda, al lado del cuarto de impresora.",
            aliases = listOf(
                "comedor",
                "comedor de profesores",
                "cocina",
                "4-20",
                "420",
                "sala 4-20"
            )
        ),
        Place(
            name = "Área común y sala de estudiantes",
            roomNumber = "4-19",
            mapImageRes = R.drawable.mapa_4_19,
            description = "Área común y sala de estudiantes.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. La sala 4-19 es la tercera puerta a su izquierda, al lado del comedor.",
            aliases = listOf(
                "area comun",
                "sala de estudiantes",
                "area comun y sala de estudiantes",
                "estudiantes",
                "4-19",
                "419",
                "sala 4-19"
            )
        ),
        Place(
            name = "Cuarto de impresora",
            roomNumber = "4-18",
            mapImageRes = R.drawable.mapa_4_18,
            description = "Cuarto de la impresora.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. El cuarto de impresora (4-18) es la primera puerta a su izquierda.",
            aliases = listOf(
                "impresora",
                "cuarto de impresora",
                "cuarto de la impresora",
                "imprimir",
                "4-18",
                "418",
                "sala 4-18"
            )
        ),
        // Búsqueda general: hay dos laboratorios de robótica, se muestran ambos
        Place(
            name = "Laboratorios de Robótica",
            roomNumber = "4-15 y 4-17",
            mapImageRes = R.drawable.mapa_laboratorios_robotica,
            description = "En este piso hay dos laboratorios de robótica: la 4-15 (Robótica y énfasis en Ingeniería de Software) y la 4-17 (Robótica Kryscia y énfasis en Ciencias de la Computación).",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. La 4-15 es la segunda puerta a su derecha y la 4-17 es la última sala al fondo a su izquierda, frente a la 4-15.",
            aliases = listOf(
                "robotica",
                "laboratorio de robotica",
                "laboratorios de robotica",
                "lab de robotica",
                "aula de robotica",
                "aula robotica",
                "laboratorio robotica"
            )
        ),
        Place(
            name = "Laboratorio de Robótica Kryscia y énfasis en Ciencias de la Computación",
            roomNumber = "4-17",
            mapImageRes = R.drawable.mapa_4_17,
            description = "Laboratorio de Robótica Kryscia y del énfasis en Ciencias de la Computación.",
            directions = "Al salir del ascensor, gire a la derecha, cruce las puertas de vidrio y siga hasta el fondo del pasillo. El laboratorio 4-17 es la última sala a su izquierda, frente a la 4-15.",
            aliases = listOf(
                "laboratorio kryscia",
                "kryscia",
                "laboratorio de robotica kryscia",
                "laboratorio de ciencias de la computacion",
                "ciencias de la computacion",
                "4-17",
                "417",
                "aula 4-17",
                "laboratorio 4-17",
                "laboratorio del enfasis de ciencias de la computacion",
                "tcu",
                "cc"
            )
        ),
        Place(
            name = "Laboratorio de Robótica y énfasis en Ingeniería de Software",
            roomNumber = "4-15",
            mapImageRes = R.drawable.mapa_4_15,
            description = "Laboratorio de Robótica y del énfasis en Ingeniería de Software.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. El laboratorio 4-15 es la segunda puerta a su derecha, después de la 4-14.",
            aliases = listOf(
                "laboratorio de ingenieria de software",
                "ingenieria de software",
                "laboratorio de robotica e ingenieria de software",
                "4-15",
                "415",
                "aula 4-15",
                "laboratorio 4-15",
                "laboratorio del enfasis de ingenieria de software",
                "is"
            )
        ),
        Place(
            name = "Laboratorio del énfasis en Ingeniería en Tecnologías de Información",
            roomNumber = "4-14",
            mapImageRes = R.drawable.mapa_4_14,
            description = "Laboratorio del énfasis en Ingeniería en Tecnologías de Información.",
            directions = "Al salir del ascensor, gire a la derecha y cruce las puertas de vidrio. El laboratorio 4-14 es la primera puerta a su derecha.",
            aliases = listOf(
                "laboratorio de tecnologias de informacion",
                "tecnologias de informacion",
                "laboratorio de ingenieria en tecnologias de informacion",
                "iti",
                "4-14",
                "414",
                "aula 4-14",
                "laboratorio 4-14"
            )
        ),
        Place(
            name = "Ascensor",
            roomNumber = null,
            mapImageRes = R.drawable.mapa_ascensor,
            description = "Ascensor del cuarto piso.",
            directions = "Usted se encuentra frente al ascensor.",
            aliases = listOf(
                "ascensor",
                "elevador"
            )
        ),
        Place(
            name = "Baños",
            roomNumber = null,
            mapImageRes = R.drawable.mapa_banios,
            description = "Baños del cuarto piso. Mujeres a la derecha, hombres a la izquierda y el baño para personas con discapacidad en el medio.",
            directions = "Al salir del ascensor, gire a la derecha. Los baños están justo a su derecha, al lado del ascensor.",
            aliases = listOf(
                "baños",
                "baño",
                "banios",
                "banio",
                "servicio sanitario",
                "servicios sanitarios",
                "bano",
                "baño de mujeres",
                "baño de hombres",
                "baño de discapacitados",
                "baño accesible"
            )
        ),
        Place(
            name = "Salida de emergencia",
            roomNumber = null,
            mapImageRes = R.drawable.mapa_salida_emergencia,
            description = "Escalera de emergencia del cuarto piso.",
            directions = "Al salir del ascensor, gire a la derecha, cruce las puertas de vidrio y siga recto hasta el final del pasillo. La salida de emergencia es la puerta del fondo.",
            aliases = listOf(
                "salida de emergencia",
                "salida",
                "emergencia",
                "escalera de emergencia"
            )
        ),
        Place(
            name = "Escaleras",
            roomNumber = null,
            mapImageRes = R.drawable.mapa_escaleras,
            description = "Escaleras principales del edificio.",
            directions = "Al salir del ascensor, las escaleras están justo al frente, al otro lado del pasillo.",
            aliases = listOf(
                "escaleras",
                "escalera",
                "gradas"
            )
        )
    )
}
