package com.example.leitroboter

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.aldebaran.qi.Future as QiFuture
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.BodyLanguageOption
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy
import com.example.leitroboter.data.Floors
import com.example.leitroboter.data.Place
import com.example.leitroboter.data.MapRoute
import com.example.leitroboter.data.PlacesCatalog
import com.example.leitroboter.matching.DestinationClient
import com.example.leitroboter.matching.DestinationResult
import com.example.leitroboter.matching.PcmWave
import com.example.leitroboter.matching.RequestPolicy
import com.example.leitroboter.matching.WavRecorder
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {
    private enum class Screen { MENU, SEARCH, MAP, FLOORS, FLOOR_PLACES, CHOOSE_FLOOR }

    /** Frase que dice Pepper; si trae animación, la ejecuta mientras la dice. */
    private data class SpeechPart(val text: String, val animationRes: Int? = null)

    private var screen = Screen.MENU
    private var lastPlaceId: String? = null
    private var lastFloor = Floors.DEFAULT_PEPPER_FLOOR
    // Piso donde está Pepper, a la par del ascensor (res/values/config.xml).
    private val pepperFloor by lazy { resources.getInteger(R.integer.pepper_floor) }
    private var lastRequest = ""
    private var placeInput: EditText? = null
    private var submitButton: Button? = null
    private var voiceButton: Button? = null
    private var statusText: TextView? = null
    private var requestGeneration = 0
    private var searchSession = 0L
    private var recognizerSession = -1L
    private var externalVoiceActive = false
    private var voiceCapturePending = false
    private var foreground = false
    @Volatile private var qiContext: QiContext? = null
    private var pendingMapSpeech = emptyList<SpeechPart>()
    // El estado de voz se modifica en el hilo principal.
    private val speechFutures = mutableSetOf<QiFuture<*>>()
    private var speechEpoch = 0
    private var speechTask: Runnable? = null
    private var voiceSilenceTask: Runnable? = null
    private var transcriptPauseTask: Runnable? = null
    private var pendingRequest: Future<*>? = null
    private var wavRecorder: WavRecorder? = null
    private var requestTimeout: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val automaticRecordingStop = Runnable { stopAudioAndSearch() }
    private val executor = Executors.newSingleThreadExecutor()
    private val destinationClient by lazy {
        DestinationClient(
            apiUrl = getString(R.string.destination_api_url),
            apiToken = getString(R.string.destination_api_token),
            allowedDestinations = PlacesCatalog.places.map { it.id }.toSet(),
            allowCleartext = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Oculta la franja gris de QiSDK arriba de la pantalla (la barra de lo que Pepper oye).
        // La app ya muestra en la barra de búsqueda lo que entendió.
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE)
        QiSDK.register(this, this)
        when (savedInstanceState?.getString("screen")) {
            Screen.SEARCH.name, Screen.CHOOSE_FLOOR.name ->
                listenForPlace(savedInstanceState.getString("request").orEmpty())
            Screen.FLOORS.name -> showFloors()
            Screen.FLOOR_PLACES.name -> showFloorPlaces(savedInstanceState.getInt("floor", Floors.DEFAULT_PEPPER_FLOOR))
            Screen.MAP.name -> {
                val place = PlacesCatalog.places.firstOrNull {
                    it.id == savedInstanceState.getString("placeId")
                }
                if (place == null) showNotFound() else showPlace(place)
            }
            else -> showMenu()
        }
    }

    private fun showMenu() {
        stopSpeaking()
        cancelPendingRequest()
        searchSession++
        externalVoiceActive = false
        clearSearchViews()
        screen = Screen.MENU
        lastPlaceId = null
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.searchButton).setOnClickListener { listenForPlace() }
        findViewById<Button>(R.id.exploreButton).setOnClickListener { showFloors() }
        say(GREETING)
    }

    private fun listenForPlace(previousRequest: String = "") {
        stopSpeaking()
        cancelPendingRequest()
        searchSession++
        externalVoiceActive = false
        screen = Screen.SEARCH
        lastPlaceId = null
        setContentView(R.layout.activity_listen)
        placeInput = findViewById<EditText>(R.id.placeInput).apply { setText(previousRequest) }
        submitButton = findViewById(R.id.submitPlaceButton)
        voiceButton = findViewById(R.id.voicePlaceButton)
        statusText = findViewById(R.id.searchStatus)
        submitButton?.setOnClickListener { processRequest(placeInput?.text.toString().trim()) }
        voiceButton?.setOnClickListener {
            if (wavRecorder == null) requestVoiceInput() else stopAudioAndSearch()
        }
        findViewById<Button>(R.id.backButton).setOnClickListener { showMenu() }
        say(ASK_PLACE)
    }

    private fun requestVoiceInput() {
        if (externalVoiceActive) return
        cancelPendingRequest()
        val generation = requestGeneration
        voiceCapturePending = true
        stopSpeaking()
        setBusy(true)
        statusText?.setText(R.string.voice_waiting_for_silence)
        val deadline = System.nanoTime() + 5_000_000_000L
        val waitForSilence = object : Runnable {
            override fun run() {
                if (!canDeliver(generation)) return
                if (speechFutures.any { !it.isDone }) {
                    if (System.nanoTime() >= deadline) {
                        voiceSilenceTask = null
                        voiceCapturePending = false
                        setBusy(false)
                        statusText?.setText(R.string.voice_silence_unavailable)
                    } else {
                        mainHandler.postDelayed(this, 50L)
                    }
                    return
                }
                voiceSilenceTask = null
                requestVoiceInputWhenSilent()
            }
        }
        voiceSilenceTask = waitForSilence
        mainHandler.post(waitForSilence)
    }

    @Suppress("DEPRECATION")
    private fun requestVoiceInputWhenSilent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-CR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        if (intent.resolveActivity(packageManager) == null) {
            requestDirectAudio()
            return
        }
        try {
            // El reconocedor externo devuelve texto; la app no recibe su audio.
            externalVoiceActive = true
            recognizerSession = searchSession
            startActivityForResult(intent, VOICE_REQUEST)
        } catch (_: ActivityNotFoundException) {
            externalVoiceActive = false
            requestDirectAudio()
        } catch (_: SecurityException) {
            externalVoiceActive = false
            requestDirectAudio()
        }
    }

    private fun requestDirectAudio() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            voiceCapturePending = false
            setBusy(false)
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceCapturePending = false
            setBusy(false)
            statusText?.setText(R.string.audio_permission_explanation)
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_REQUEST)
            return
        }
        startAudioCapture()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != MICROPHONE_REQUEST || screen != Screen.SEARCH) return
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        // La grabación comienza con una nueva pulsación de Hablar.
        statusText?.setText(if (granted) R.string.audio_permission_granted else R.string.audio_permission_denied)
    }

    private fun startAudioCapture() {
        cancelPendingRequest()
        voiceCapturePending = true
        val generation = requestGeneration
        armRequestTimeout(generation)
        val recorder = WavRecorder()
        wavRecorder = recorder
        placeInput?.isEnabled = false
        submitButton?.isEnabled = false
        voiceButton?.isEnabled = true
        voiceButton?.setText(R.string.audio_stop_button)
        statusText?.setText(R.string.audio_recording)
        mainHandler.postDelayed(automaticRecordingStop, PcmWave.MAX_DURATION_MS)
        pendingRequest = executor.submit {
            val wav = recorder.record()
            runOnUiThread {
                if (!canDeliver(generation)) return@runOnUiThread
                mainHandler.removeCallbacks(automaticRecordingStop)
                wavRecorder = null
                voiceCapturePending = false
                pendingRequest = null
                if (wav == null) {
                    clearRequestTimeout()
                    setBusy(false)
                    if (recorder.endedWithoutSpeech) {
                        statusText?.setText(R.string.audio_no_speech)
                        say(NO_SPEECH)
                    } else {
                        statusText?.setText(R.string.audio_recording_failed)
                    }
                } else {
                    // Fin de la voz, botón Detener o tope de 8 s: la consulta se envía sola.
                    submitAudio(wav)
                }
            }
        }
    }

    private fun stopAudioAndSearch() {
        val recorder = wavRecorder ?: return
        mainHandler.removeCallbacks(automaticRecordingStop)
        voiceButton?.isEnabled = false
        statusText?.setText(R.string.search_processing)
        recorder.stop()
    }

    private fun submitAudio(wav: ByteArray) {
        val generation = requestGeneration
        armRequestTimeout(generation)
        setBusy(true)
        statusText?.setText(R.string.audio_transcribing)
        pendingRequest = executor.submit {
            val transcript = destinationClient.transcribeAudio(wav)
            runOnUiThread {
                if (!canDeliver(generation)) return@runOnUiThread
                clearRequestTimeout()
                pendingRequest = null
                setBusy(false)
                when {
                    transcript == null -> statusText?.setText(R.string.service_unavailable)
                    transcript.isEmpty() -> statusText?.setText(R.string.voice_not_understood)
                    // Se muestra lo que entendió el servidor antes de buscar el destino.
                    else -> showTranscriptThenSearch(transcript)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != VOICE_REQUEST) return
        externalVoiceActive = false
        voiceCapturePending = false
        if (screen != Screen.SEARCH || recognizerSession != searchSession) return
        setBusy(false)
        if (resultCode != RESULT_OK) {
            statusText?.setText(R.string.voice_cancelled)
            return
        }
        val transcript = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.trim().orEmpty()
        if (!RequestPolicy.isValid(transcript)) {
            statusText?.setText(R.string.voice_not_understood)
            return
        }
        // El reconocedor ya detectó el fin de la voz; se busca sin tocar Buscar.
        showTranscriptThenSearch(transcript)
    }

    /** Muestra lo que se entendió y espera un momento para que la persona lo lea antes de buscar. */
    private fun showTranscriptThenSearch(transcript: String) {
        placeInput?.setText(transcript)
        placeInput?.setSelection(transcript.length)
        setBusy(true)
        statusText?.setText(R.string.voice_heard)
        val generation = requestGeneration
        val task = Runnable {
            transcriptPauseTask = null
            if (!canDeliver(generation)) return@Runnable
            setBusy(false)
            statusText?.setText(R.string.search_processing)
            processRequest(transcript)
        }
        transcriptPauseTask = task
        mainHandler.postDelayed(task, TRANSCRIPT_PAUSE_MS)
    }

    private fun processRequest(request: String) {
        if (!RequestPolicy.isValid(request)) {
            placeInput?.error = getString(R.string.invalid_request)
            return
        }
        stopSpeaking()
        val exactMatches = PlacesCatalog.exactMatches(request)
        // Un mismo nombre puede estar en varios pisos (grupo): se pregunta el piso si hace falta.
        if (exactMatches.size == 1 || (exactMatches.size > 1 && Floors.groupOf(exactMatches) != null)) {
            resolvePlace(exactMatches.first(), request)
            return
        }

        cancelPendingRequest()
        val generation = requestGeneration
        armRequestTimeout(generation)
        setBusy(true)
        pendingRequest = executor.submit {
            val result = try {
                destinationClient.match(request)
            } catch (_: LinkageError) {
                DestinationResult.Failure
            } catch (_: Exception) {
                DestinationResult.Failure
            }
            deliverResult(generation, request, result)
        }
    }

    private fun canDeliver(generation: Int): Boolean =
        generation == requestGeneration && screen == Screen.SEARCH && foreground && !isFinishing && !isDestroyed

    private fun deliverResult(generation: Int, request: String, result: DestinationResult) {
        runOnUiThread {
            if (!canDeliver(generation)) return@runOnUiThread
            clearRequestTimeout()
            pendingRequest = null
            setBusy(false)
            when (result) {
                is DestinationResult.Match -> {
                    val place = PlacesCatalog.places.firstOrNull { it.id == result.id }
                    if (place == null) showNotFound() else resolvePlace(place, request)
                }
                DestinationResult.NoMatch -> showNotFound(Floors.requestedFloor(request))
                DestinationResult.Failure -> statusText?.setText(R.string.service_unavailable)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        placeInput?.isEnabled = !busy
        submitButton?.isEnabled = !busy
        voiceButton?.isEnabled = !busy
        voiceButton?.setText(R.string.voice_button)
        statusText?.setText(if (busy) R.string.search_processing else R.string.search_ready)
    }

    /** Muestra el lugar, o la lista de pisos si el lugar se repite y no se dijo en cuál piso. */
    private fun resolvePlace(place: Place, request: String) {
        val candidates = Floors.candidates(place, request, PlacesCatalog.places)
        when (candidates.size) {
            0 -> showNotFound(Floors.requestedFloor(request))
            1 -> showPlace(candidates.single())
            else -> showFloorChoice(candidates, request)
        }
    }

    private fun showPlace(place: Place) {
        lastPlaceId = place.id
        val otherFloor = place.floor != pepperFloor
        val routeStart = if (otherFloor) getString(R.string.directions_other_floor, place.floor) else ""
        val directions = joinSentences(routeStart, place.directions)
        val title = if (place.roomNumber != null) "${place.name} (${place.roomNumber})" else place.name
        showMap(
            placeName = "$title · ${getString(R.string.floor_label, place.floor)}",
            mapImage = place.mapImageRes,
            routes = place.routes,
            // El nombre ya está en el título; la descripción solo se muestra si agrega algo.
            description = listOf(
                if (repeatsName(place.description, place.name)) "" else place.description,
                directions
            ).filter { it.isNotBlank() }.joinToString("\n"),
            speech = listOf(
                SpeechPart(joinSentences(
                    if (otherFloor) "Usted quiere ir a ${place.name}, en el piso ${place.floor}"
                    else "Usted quiere ir a ${place.name}",
                    if (repeatsName(place.description, place.name)) "" else place.description,
                    routeStart
                )),
                SpeechPart(place.directions, place.gestureAnimationRes)
            )
        )
    }

    /** Pisos del edificio para explorar los lugares sin buscar. */
    private fun showFloors() {
        showList(
            Screen.FLOORS,
            getString(R.string.floors_title),
            getString(R.string.floors_subtitle, pepperFloor),
            Floors.ALL.map { floor ->
                val count = PlacesCatalog.places.count { it.floor == floor }
                ListOption(
                    if (count == 0) getString(R.string.floor_button_empty, floor)
                    else getString(R.string.floor_button, floor, count),
                    enabled = count > 0
                ) { showFloorPlaces(floor) }
            },
            getString(R.string.new_search) to { listenForPlace() },
            speech = getString(R.string.floors_title)
        )
    }

    private fun showFloorPlaces(floor: Int) {
        lastFloor = floor
        showList(
            Screen.FLOOR_PLACES,
            getString(R.string.floor_label, floor),
            getString(R.string.floor_places_subtitle),
            PlacesCatalog.places.filter { it.floor == floor }.map { place ->
                ListOption(shortLabel(place)) { showPlace(place) }
            },
            getString(R.string.other_floors) to { showFloors() },
            speech = "Estos son los lugares del piso $floor. Toque uno para ver cómo llegar."
        )
    }

    /** El lugar pedido existe en varios pisos: la persona toca el que busca. */
    private fun showFloorChoice(candidates: List<Place>, request: String) {
        showList(
            Screen.CHOOSE_FLOOR,
            getString(R.string.choose_floor_title),
            getString(R.string.choose_floor_subtitle),
            candidates.map { place ->
                ListOption("${getString(R.string.floor_label, place.floor)} · ${shortLabel(place)}") { showPlace(place) }
            },
            getString(R.string.new_search) to { listenForPlace(request) },
            speech = "Ese lugar está en varios pisos. ¿A cuál piso quiere ir?",
            request = request
        )
    }

    private class ListOption(val text: String, val enabled: Boolean = true, val onClick: () -> Unit)

    private fun showList(
        listScreen: Screen,
        title: String,
        subtitle: String,
        options: List<ListOption>,
        secondary: Pair<String, () -> Unit>,
        speech: String = "",
        request: String = ""
    ) {
        stopSpeaking()
        cancelPendingRequest()
        searchSession++
        externalVoiceActive = false
        clearSearchViews()
        screen = listScreen
        lastPlaceId = null
        lastRequest = request
        setContentView(R.layout.screen_list)
        findViewById<TextView>(R.id.listTitle).text = title
        findViewById<TextView>(R.id.listSubtitle).text = subtitle
        fillGrid(findViewById(R.id.listContainer), options)
        findViewById<Button>(R.id.listBackButton).apply {
            text = secondary.first
            setOnClickListener { secondary.second() }
        }
        findViewById<Button>(R.id.backButton).setOnClickListener { showMenu() }
        if (speech.isNotBlank()) say(speech)
    }

    private fun fillGrid(grid: LinearLayout, options: List<ListOption>) {
        val columns = when {
            options.size <= 6 -> 2
            options.size <= 12 -> 3
            else -> 4
        }
        val density = resources.displayMetrics.density
        val width = resources.configuration.screenWidthDp
        val maxWidth = GRID_MAX_WIDTH_DP[columns] ?: width
        val sidePadding = ((width - 64 - maxWidth).coerceAtLeast(0) * density / 2).toInt()
        grid.setPadding(sidePadding, 0, sidePadding, 0)
        val textSize = GRID_TEXT_SP[columns] ?: 14f
        for (rowOptions in options.chunked(columns)) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                // Sin esto, Android alinea los botones por la línea del texto y los desplaza
                // hacia arriba o abajo según tengan una o dos líneas.
                isBaselineAligned = false
            }
            for (option in rowOptions) {
                val button = layoutInflater.inflate(R.layout.item_list_button, row, false) as Button
                button.text = option.text
                button.textSize = textSize
                button.isEnabled = option.enabled
                if (!option.enabled) button.alpha = 0.5f
                button.setOnClickListener { option.onClick() }
                row.addView(button)
            }
            // La última fila incompleta conserva el ancho de columna de las demás.
            repeat(columns - rowOptions.size) {
                row.addView(Space(this), LinearLayout.LayoutParams(0, 0, 1f))
            }
            grid.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    /** Nombre corto para un botón de la cuadrícula: "3-14 · José Antonio Brenes Carranza".
     *  El número va primero para que no se parta al final de una línea. */
    private fun shortLabel(place: Place): String {
        val name = place.shortName
            ?: OFFICE_PREFIXES.fold(place.name) { text, prefix -> text.removePrefix(prefix) }
        val room = place.roomNumber
        return if (room != null && !name.contains(room)) "$room · $name" else name
    }

    private fun showNotFound() = showNotFound(null)

    private fun showNotFound(floor: Int?) {
        lastPlaceId = null
        showMap(
            getString(R.string.place_not_found),
            R.drawable.mapa_default,
            if (floor == null) getString(R.string.place_not_found_description)
            else getString(R.string.place_not_found_on_floor, floor)
        )
    }

    private fun showMap(
        placeName: String,
        mapImage: Int,
        description: String,
        speech: List<SpeechPart> = listOf(SpeechPart(description)),
        routes: List<MapRoute> = emptyList()
    ) {
        stopSpeaking()
        cancelPendingRequest()
        searchSession++
        externalVoiceActive = false
        clearSearchViews()
        screen = Screen.MAP
        setContentView(R.layout.screen_place)
        // El servidor devuelve un ID; el texto y el mapa se obtienen del catálogo local.
        findViewById<TextView>(R.id.placeName).text = placeName
        findViewById<RouteMapView>(R.id.placeMap).apply {
            setImageResource(mapImage)
            setRoutes(routes)
        }
        findViewById<TextView>(R.id.placeDescription).text = description
        findViewById<Button>(R.id.backButton).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.newSearchButton).setOnClickListener { listenForPlace() }
        pendingMapSpeech = speech
        speakPendingMap()
    }

    private fun speakPendingMap() {
        if (screen != Screen.MAP || pendingMapSpeech.isEmpty() || speechTask != null) return
        say(pendingMapSpeech) { pendingMapSpeech = emptyList() }
    }

    private fun clearSearchViews() {
        placeInput = null
        submitButton = null
        voiceButton = null
        statusText = null
    }

    private fun cancelPendingRequest() {
        requestGeneration++
        voiceSilenceTask?.let { mainHandler.removeCallbacks(it) }
        voiceSilenceTask = null
        transcriptPauseTask?.let { mainHandler.removeCallbacks(it) }
        transcriptPauseTask = null
        voiceCapturePending = false
        clearRequestTimeout()
        mainHandler.removeCallbacks(automaticRecordingStop)
        wavRecorder?.cancel()
        wavRecorder = null
        pendingRequest?.cancel(true)
        pendingRequest = null
        destinationClient.cancel()
    }

    private fun clearRequestTimeout() {
        requestTimeout?.let { mainHandler.removeCallbacks(it) }
        requestTimeout = null
    }

    private fun armRequestTimeout(generation: Int) {
        clearRequestTimeout()
        val deadline = Runnable {
            if (canDeliver(generation)) {
                cancelPendingRequest()
                setBusy(false)
                statusText?.setText(R.string.service_unavailable)
            }
        }
        requestTimeout = deadline
        mainHandler.postDelayed(deadline, 28_000L)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("screen", screen.name)
        outState.putString("request", placeInput?.text?.toString() ?: lastRequest)
        outState.putInt("floor", lastFloor)
        outState.putString("placeId", lastPlaceId)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        speakPendingMap()
        if (screen == Screen.MENU && qiContext != null) say(GREETING)
    }

    override fun onStop() {
        foreground = false
        stopSpeaking()
        cancelPendingRequest()
        setBusy(false)
        super.onStop()
    }

    // Pantalla completa (modo inmersivo): esconde la barra de estado y la de navegación de
    // Android. Se vuelve a aplicar cada vez que la app recupera el foco, porque Android las
    // muestra de nuevo al volver de otra ventana (por ejemplo, el diálogo del micrófono).
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }

    override fun onDestroy() {
        stopSpeaking()
        cancelPendingRequest()
        executor.shutdownNow()
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    override fun onRobotFocusGained(qiContext: QiContext) {
        Log.i(TAG, "Foco Pepper obtenido")
        this.qiContext = qiContext
        runOnUiThread {
            if (this.qiContext !== qiContext || !foreground || externalVoiceActive || voiceCapturePending) {
                return@runOnUiThread
            }
            if (screen == Screen.MENU) say(GREETING) else speakPendingMap()
        }
    }

    override fun onRobotFocusLost() {
        Log.i(TAG, "Foco Pepper perdido")
        qiContext = null
        runOnUiThread { stopSpeaking() }
    }

    override fun onRobotFocusRefused(reason: String) {
        Log.w(TAG, "Foco Pepper rechazado: $reason")
        qiContext = null
        runOnUiThread { stopSpeaking() }
    }

    /** Lee frases de la aplicación e indicaciones del catálogo. */
    private fun say(text: String) = say(listOf(SpeechPart(text)))

    /** Dice las partes en orden; las animaciones se cancelan junto con la voz. */
    private fun say(parts: List<SpeechPart>, onFinished: () -> Unit = {}) {
        stopSpeaking()
        val context = qiContext ?: return
        val pending = parts.filter { it.text.isNotBlank() }
        if (!foreground || externalVoiceActive || voiceCapturePending || pending.isEmpty()) return
        val epoch = speechEpoch
        val deadline = System.nanoTime() + 5_000_000_000L
        val startWhenSilent = object : Runnable {
            override fun run() {
                if (!canSpeak(epoch, context)) return
                if (speechFutures.any { !it.isDone }) {
                    if (System.nanoTime() < deadline) {
                        mainHandler.postDelayed(this, 50L)
                    } else {
                        speechTask = null
                        Log.w(TAG, "Pepper sigue ocupado con la indicación anterior")
                    }
                    return
                }
                sayNextPart(pending.iterator(), epoch, context, onFinished)
            }
        }
        speechTask = startWhenSilent
        mainHandler.post(startWhenSilent)
    }

    private fun sayNextPart(
        parts: Iterator<SpeechPart>,
        epoch: Int,
        context: QiContext,
        onFinished: () -> Unit
    ) {
        if (!canSpeak(epoch, context)) return
        if (!parts.hasNext()) {
            speechTask = null
            onFinished()
            return
        }
        val part = parts.next()

        fun continueAfterVoiceError(message: String, error: Throwable) {
            Log.w(TAG, message, error)
            part.animationRes?.let { animate(it, context) }
            sayNextPart(parts, epoch, context, onFinished)
        }

        try {
            val build = SayBuilder.with(context).withText(part.text.replace("\n", " "))
                .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                .buildAsync()
            speechFutures.add(build)
            build.thenConsume { completed ->
                runOnUiThread {
                    speechFutures.remove(build)
                    if (!canSpeak(epoch, context)) return@runOnUiThread
                    if (!completed.isSuccess) {
                        if (completed.hasError()) {
                            continueAfterVoiceError("No se pudo preparar la voz de Pepper", completed.error)
                        } else {
                            speechTask = null
                        }
                        return@runOnUiThread
                    }
                    val running = try {
                        completed.value.async().run()
                    } catch (error: Exception) {
                        continueAfterVoiceError("No se pudo iniciar la voz de Pepper", error)
                        return@runOnUiThread
                    }
                    speechFutures.add(running)
                    part.animationRes?.let { animate(it, context) }
                    running.thenConsume { completedSpeech ->
                        if (completedSpeech.hasError()) {
                            Log.w(TAG, "No se pudo completar la voz de Pepper", completedSpeech.error)
                        } else if (completedSpeech.isSuccess) {
                            Log.i(TAG, "Voz de Pepper completada")
                        }
                        runOnUiThread finishPart@{
                            speechFutures.remove(running)
                            if (!canSpeak(epoch, context)) return@finishPart
                            if (completedSpeech.isSuccess || completedSpeech.hasError()) {
                                sayNextPart(parts, epoch, context, onFinished)
                            } else {
                                speechTask = null
                            }
                        }
                    }
                }
            }
        } catch (error: Exception) {
            continueAfterVoiceError("No se pudo preparar la voz de Pepper", error)
        }
    }

    /** Ejecuta un gesto de res/raw; stopSpeaking() también lo cancela. */
    private fun animate(animationRes: Int, context: QiContext) {
        try {
            val running = AnimationBuilder.with(context).withResources(animationRes).buildAsync()
                .andThenCompose { animation ->
                    AnimateBuilder.with(context).withAnimation(animation).buildAsync()
                }
                .andThenCompose { action -> action.async().run() }
            speechFutures.add(running)
            running.thenConsume { completed ->
                if (completed.hasError()) Log.w(TAG, "No se pudo completar el gesto de Pepper", completed.error)
                runOnUiThread { speechFutures.remove(running) }
            }
        } catch (error: Exception) {
            Log.w(TAG, "No se pudo iniciar el gesto de Pepper", error)
        }
    }

    private fun canSpeak(epoch: Int, context: QiContext): Boolean =
        epoch == speechEpoch && qiContext === context && foreground && !isFinishing &&
            !isDestroyed && !externalVoiceActive && !voiceCapturePending

    private fun stopSpeaking() {
        speechEpoch++
        speechTask?.let { mainHandler.removeCallbacks(it) }
        speechTask = null
        // Espera la cancelación de Say antes de habilitar el micrófono.
        speechFutures.filter { !it.isDone }.toList().forEach {
            try { it.requestCancellation() } catch (_: Exception) { }
        }
    }

    private fun repeatsName(description: String, name: String): Boolean {
        val descriptionWords = significantWords(description)
        val nameWords = significantWords(name)
        if (descriptionWords.isEmpty()) return true
        val descriptionText = descriptionWords.joinToString(" ")
        val nameText = nameWords.joinToString(" ")
        if (nameText.contains(descriptionText) || descriptionText.contains(nameText)) return true
        return descriptionWords.count { it in nameWords } >= descriptionWords.size * 0.8
    }

    private fun significantWords(text: String): List<String> =
        RequestPolicy.normalize(text).replace(Regex("[^a-z0-9 ]"), " ").split(" ")
            .filter { it.isNotEmpty() && it !in IGNORED_WORDS }

    private fun joinSentences(vararg sentences: String): String = sentences.map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ") { if (it.endsWith(".")) it else "$it." }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (screen != Screen.MENU) showMenu() else super.onBackPressed()
    }

    private companion object {
        const val TAG = "Leitroboter"
        val GRID_MAX_WIDTH_DP = mapOf(2 to 760, 3 to 1040)
        /** Tamaño de letra de los botones según las columnas: igual en toda la cuadrícula. */
        val GRID_TEXT_SP = mapOf(2 to 18f, 3 to 16f, 4 to 14f)
        val OFFICE_PREFIXES = listOf("Oficina del profesor ", "Oficina de la profesora ")
        const val VOICE_REQUEST = 1001
        /** Tiempo para leer en la barra lo que se entendió antes de mostrar la ruta. */
        const val TRANSCRIPT_PAUSE_MS = 1_000L
        const val MICROPHONE_REQUEST = 1002
        const val GREETING = "¡Hola! Soy Leitroboter. Le puedo ayudar a encontrar aulas, oficinas y laboratorios del edificio anexo de la ECCI. Toque Iniciar para buscar un lugar, o Explorar por piso para ver qué hay en cada piso."
        const val ASK_PLACE = "¿A dónde le gustaría ir?"
        const val NO_SPEECH = "No le escuché. Toque Hablar para intentar de nuevo."
        val IGNORED_WORDS = setOf("de", "del", "la", "el", "los", "las", "y", "en", "a", "al")
    }
}
