package com.example.leitroboter

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast

import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.design.activity.RobotActivity

import com.example.leitroboter.data.Place
import com.example.leitroboter.data.PlacesCatalog

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {
    // Variable global para navegación
    private var showingMap = false

    // Contexto del robot, solo existe mientras la app tiene el foco del robot
    @Volatile
    private var qiContext: QiContext? = null

    // Frase que el robot está diciendo, para poder interrumpirla
    @Volatile
    private var currentSay: Future<Void>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        QiSDK.register(this, this)

        showMenu()
    }

    override fun onDestroy() {
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    override fun onRobotFocusGained(qiContext: QiContext) {
        this.qiContext = qiContext
        // Al iniciar, el foco llega después de mostrar el menú, así que saluda aquí
        if (!showingMap) say(GREETING)
    }

    override fun onRobotFocusLost() {
        qiContext = null
    }

    override fun onRobotFocusRefused(reason: String) {
        Log.e(TAG, "Foco del robot rechazado: $reason")
    }

    private fun say(text: String): Future<Void>? {
        val context = qiContext ?: run {
            Log.w(TAG, "Sin foco del robot")
            return null
        }
        // Interrumpe lo que estuviera diciendo antes
        stopSpeaking()
        val future = SayBuilder.with(context)
            .withText(text.replace("\n", " "))
            .buildAsync()
            .andThenCompose { it.async().run() }
        future.thenConsume {
            if (it.hasError()) Log.e(TAG, "Error al decir", it.error)
        }
        currentSay = future
        return future
    }

    private fun stopSpeaking() {
        currentSay?.requestCancellation()
        currentSay = null
    }

    private fun showMenu() {
        stopSpeaking()
        showingMap = false
        setContentView(R.layout.activity_main)
        // Saluda al volver al menú; al iniciar todavía no hay foco y saluda onRobotFocusGained
        if (qiContext != null) say(GREETING)

        val searchButton =
            findViewById<Button>(R.id.searchButton)

        searchButton.setOnClickListener {
            listenForPlace()
        }
    }

    private fun listenForPlace() {
        setContentView(R.layout.activity_listen)
        // Pregunta a dónde quiere ir.
        // Para la entrada por voz: el Listen debe iniciar solo cuando termine este Say
        // (por ejemplo con thenConsume sobre el Future que devuelve say), para que el robot no se escuche a sí mismo
        say(ASK_PLACE)

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

        findViewById<Button>(R.id.backButton).setOnClickListener {
            showMenu()
        }
    }

    private fun processRequest(request: String) {
        // Asociar la solicitud con un lugar conocido
        val place = findPlace(request)

        if (place != null) {
            showMap(
                placeName = if (place.roomNumber != null) "${place.name} (${place.roomNumber})" else place.name,
                mapImage = place.mapImageRes,
                description = "${place.description}\n${place.directions}",
                // Primero dice el nombre del lugar y luego la descripción y el camino
                // Si la descripción casi repite el nombre, se la salta para no decirlo dos veces
                speech = joinSentences(
                    "Usted quiere ir a ${place.name}",
                    if (repeatsName(place.description, place.name)) "" else place.description,
                    place.directions
                )
            )
        } else {
            showNotFound()
        }
        /*
        // Muestra un mensaje de "procesando..." mientras piensa
        setContentView(R.layout.activity_processing)
         */
    }

    // Pasa a minúsculas y quita tildes y la ñ, para que "baños" y "banos" coincidan
    private fun normalize(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ñ", "n")
    }

    private fun findPlace(request: String): Place? {
        // Normaliza la solicitud
        val normalizedRequest = normalize(request)

        // Obtiene la base de datos de los lugares
        val places = PlacesCatalog.places

        // Busca entre los alias de los lugares uno que coincida con la solicitud
        return places.firstOrNull { place ->
            place.aliases.any { alias ->
                normalize(alias) == normalizedRequest
            } || normalize(place.name) == normalizedRequest
        }
    }

    private fun showNotFound() {
        // Si no encuentra el lugar solicitado muestra una pantalla por defecto
        showMap(
            placeName = "No encontré el lugar que pediste",
            mapImage = R.drawable.mapa_default,
            description = "No te entendí o el lugar que me dijiste no se encuentra en el cuarto piso del edificio anexo de la ECCI.\nRegresa al menú y vuelve a preguntar"
        )
    }

    // Indica si la descripción casi repite el nombre del lugar:
    // una contiene a la otra, o al menos el 80% de las palabras de la descripción están en el nombre
    private fun repeatsName(description: String, name: String): Boolean {
        val descriptionWords = significantWords(description)
        val nameWords = significantWords(name)
        if (descriptionWords.isEmpty()) return true

        val descriptionText = descriptionWords.joinToString(" ")
        val nameText = nameWords.joinToString(" ")
        if (nameText.contains(descriptionText) || descriptionText.contains(nameText)) return true

        val repeated = descriptionWords.count { it in nameWords }
        return repeated >= descriptionWords.size * 0.8
    }

    // Palabras normalizadas sin puntuación, artículos ni preposiciones
    private fun significantWords(text: String): List<String> {
        return normalize(text)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.isNotEmpty() && it !in IGNORED_WORDS }
    }

    // Une frases con un punto, sin duplicar el punto si la frase ya lo tiene
    private fun joinSentences(vararg sentences: String): String {
        return sentences
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ") { if (it.endsWith(".")) it else "$it." }
    }

    private fun showMap(
        placeName: String,
        mapImage: Int,
        description: String,
        // Lo que dice el robot; por defecto, lo mismo que la descripción
        speech: String = description
    ) {

        showingMap = true

        setContentView(R.layout.screen_place)
        // Muestra el nombre del lugar
        findViewById<TextView>(R.id.placeName).text = placeName
        // Muestra el mapa del lugar
        findViewById<ImageView>(R.id.placeMap)
            .setImageResource(mapImage)
        // Muestra una descripción del lugar
        findViewById<TextView>(R.id.placeDescription).text = description
        // Vuelve al menú, porque en Pepper no hay barra de navegación
        findViewById<Button>(R.id.backButton).setOnClickListener {
            showMenu()
        }
        // El robot dice verbalmente el lugar, la descripción y el camino
        say(speech)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (showingMap) {
            showMenu()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        // Frases fijas que dice el robot
        private const val GREETING = "¡Hola! Soy Leitroboter. Le puedo ayudar a encontrar aulas, oficinas y laboratorios del cuarto piso del edificio anexo de la ECCI. Toque el botón para buscar un lugar."
        private const val ASK_PLACE = "¿A dónde le gustaría ir?"

        // Artículos y preposiciones que no cuentan al comparar descripción y nombre
        private val IGNORED_WORDS = setOf("de", "del", "la", "el", "los", "las", "y", "en", "a", "al")
    }
}