package com.example.leitroboter

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast

import com.example.leitroboter.data.Place
import com.example.leitroboter.data.PlacesCatalog

class MainActivity : Activity() {
    // Variable global para navegación
    private var showingMap = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showMenu()
    }

    private fun showMenu() {
        showingMap = false
        setContentView(R.layout.activity_main)

        val searchButton =
            findViewById<Button>(R.id.searchButton)

        searchButton.setOnClickListener {
            listenForPlace()
        }
    }

    private fun listenForPlace() {
        setContentView(R.layout.activity_listen)

        val placeInput =
            findViewById<EditText>(R.id.placeInput)

        val submitButton =
            findViewById<Button>(R.id.submitPlaceButton)

        submitButton.setOnClickListener {
            val request = placeInput.text.toString().trim()

            if (request.isBlank()) {
                placeInput.error = "Escribe el nombre de un lugar"
                return@setOnClickListener
            }

            processRequest(request)
        }
    }

    private fun processRequest(request: String) {
        // Asociar la solicitud con un lugar conocido
        val place = findPlace(request)

        if (place != null) {
            showMap(
                placeName = place.name,
                mapImage = place.mapImageRes,
                description = place.description
            )
        } else {
            showNotFound()
        }
        /*
        // Muestra un mensaje de "procesando..." mientras piensa
        setContentView(R.layout.activity_processing)
         */
    }

    private fun findPlace(request: String): Place? {
        // Normaliza la solicitud
        val normalizedRequest = request
            .trim()
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")

        // Obtiene la base de datos de los lugares
        val places = PlacesCatalog.places

        // Busca entre los alias de los lugares uno que coincida con la solicitud
        return places.firstOrNull { place ->
            place.aliases.any { alias ->
                alias == normalizedRequest
            } || place.name.lowercase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u") == normalizedRequest
        }
    }

    private fun showNotFound() {
        // Si no encuentra el lugar solicitado muestra una pantalla por defecto
        showMap(
            placeName = "No encontré el lugar que pediste",
            mapImage = R.drawable.mapa_default,
            description = "No te entendí o el lugar que me dijiste no se encuentra en el cuarto piso del edificio anexo de la ECCI.\n Regresa al menú y vuelve a preguntar"
        )
    }

    private fun showMap(
        placeName: String,
        mapImage: Int,
        description: String
    ) {

        showingMap = true

        setContentView(R.layout.screen_place)
        // Muestra el nombre del lugar
        findViewById<TextView>(R.id.placeName).text = placeName
        // Muestra el mapa del lugar
        findViewById<ImageView>(R.id.placeMap)
            .setImageResource(mapImage)
        // Muestra una descripción del lugar
        // TODO: el robot dice verbalmente la descripción
        findViewById<TextView>(R.id.placeDescription).text = description
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (showingMap) {
            showMenu()
        } else {
            super.onBackPressed()
        }
    }
}