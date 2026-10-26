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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.aldebaran.qi.Future as QiFuture
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.example.leitroboter.data.Place
import com.example.leitroboter.data.PlacesCatalog
import com.example.leitroboter.matching.DestinationClient
import com.example.leitroboter.matching.DestinationResult
import com.example.leitroboter.matching.PcmWave
import com.example.leitroboter.matching.RequestPolicy
import com.example.leitroboter.matching.WavRecorder
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {
    private enum class Screen { MENU, SEARCH, MAP }

    private var screen = Screen.MENU
    private var lastPlaceId: String? = null
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
    // El estado de voz se modifica en el hilo principal.
    private val speechFutures = mutableSetOf<QiFuture<*>>()
    private var speechEpoch = 0
    private var speechStartTask: Runnable? = null
    private var voiceSilenceTask: Runnable? = null
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
        QiSDK.register(this, this)
        when (savedInstanceState?.getString("screen")) {
            Screen.SEARCH.name -> listenForPlace(savedInstanceState.getString("request").orEmpty())
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
                    statusText?.setText(R.string.audio_recording_failed)
                } else {
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
        pendingRequest = executor.submit {
            val result = destinationClient.matchAudio(wav)
            deliverResult(generation, result)
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
        placeInput?.setText(transcript)
        placeInput?.setSelection(transcript.length)
        // El usuario revisa la transcripción antes de enviarla.
        statusText?.setText(R.string.voice_review)
    }

    private fun processRequest(request: String) {
        if (!RequestPolicy.isValid(request)) {
            placeInput?.error = getString(R.string.invalid_request)
            return
        }
        stopSpeaking()
        val normalized = RequestPolicy.normalize(request)
        val exactMatches = PlacesCatalog.places.filter { place ->
            RequestPolicy.normalize(place.name) == normalized ||
                place.aliases.any { RequestPolicy.normalize(it) == normalized }
        }
        if (exactMatches.size == 1) {
            showPlace(exactMatches.single())
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
            deliverResult(generation, result)
        }
    }

    private fun canDeliver(generation: Int): Boolean =
        generation == requestGeneration && screen == Screen.SEARCH && foreground && !isFinishing && !isDestroyed

    private fun deliverResult(generation: Int, result: DestinationResult) {
        runOnUiThread {
            if (!canDeliver(generation)) return@runOnUiThread
            clearRequestTimeout()
            pendingRequest = null
            setBusy(false)
            when (result) {
                is DestinationResult.Match -> {
                    val place = PlacesCatalog.places.firstOrNull { it.id == result.id }
                    if (place == null) showNotFound() else showPlace(place)
                }
                DestinationResult.NoMatch -> showNotFound()
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

    private fun showPlace(place: Place) {
        lastPlaceId = place.id
        showMap(
            placeName = if (place.roomNumber != null) "${place.name} (${place.roomNumber})" else place.name,
            mapImage = place.mapImageRes,
            description = listOf(place.description, place.directions).filter { it.isNotBlank() }.joinToString("\n"),
            speech = joinSentences(
                "Usted quiere ir a ${place.name}",
                if (repeatsName(place.description, place.name)) "" else place.description,
                place.directions
            )
        )
    }

    private fun showNotFound() {
        lastPlaceId = null
        showMap(
            getString(R.string.place_not_found),
            R.drawable.mapa_default,
            getString(R.string.place_not_found_description)
        )
    }

    private fun showMap(
        placeName: String,
        mapImage: Int,
        description: String,
        speech: String = description
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
        findViewById<ImageView>(R.id.placeMap).setImageResource(mapImage)
        findViewById<TextView>(R.id.placeDescription).text = description
        findViewById<Button>(R.id.backButton).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.newSearchButton).setOnClickListener { listenForPlace() }
        say(speech)
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
        outState.putString("request", placeInput?.text?.toString().orEmpty())
        outState.putString("placeId", lastPlaceId)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        if (screen == Screen.MENU && qiContext != null) say(GREETING)
    }

    override fun onStop() {
        foreground = false
        stopSpeaking()
        cancelPendingRequest()
        setBusy(false)
        super.onStop()
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
            if (this.qiContext === qiContext && foreground && screen == Screen.MENU &&
                !externalVoiceActive && !voiceCapturePending) say(GREETING)
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
    private fun say(text: String) {
        stopSpeaking()
        val context = qiContext ?: return
        if (!foreground || externalVoiceActive || voiceCapturePending || text.isBlank()) return
        val epoch = speechEpoch
        val deadline = System.nanoTime() + 5_000_000_000L
        val startWhenSilent = object : Runnable {
            override fun run() {
                if (!canSpeak(epoch, context)) return
                if (speechFutures.any { !it.isDone }) {
                    if (System.nanoTime() < deadline) mainHandler.postDelayed(this, 50L)
                    return
                }
                speechStartTask = null
                try {
                    val build = SayBuilder.with(context).withText(text.replace("\n", " ")).buildAsync()
                    speechFutures.add(build)
                    build.thenConsume { completed ->
                        runOnUiThread {
                            speechFutures.remove(build)
                            if (!canSpeak(epoch, context) || !completed.isSuccess) return@runOnUiThread
                            try {
                                val running = completed.value.async().run()
                                speechFutures.add(running)
                                running.thenConsume { completedSpeech ->
                                    if (completedSpeech.hasError()) {
                                        Log.w(TAG, "No se pudo completar la voz de Pepper", completedSpeech.error)
                                    } else if (completedSpeech.isSuccess) {
                                        Log.i(TAG, "Voz de Pepper completada")
                                    }
                                    runOnUiThread { speechFutures.remove(running) }
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {
                    // El mapa sigue disponible si falla el servicio de voz.
                }
            }
        }
        speechStartTask = startWhenSilent
        mainHandler.post(startWhenSilent)
    }

    private fun canSpeak(epoch: Int, context: QiContext): Boolean =
        epoch == speechEpoch && qiContext === context && foreground && !isFinishing &&
            !isDestroyed && !externalVoiceActive && !voiceCapturePending

    private fun stopSpeaking() {
        speechEpoch++
        speechStartTask?.let { mainHandler.removeCallbacks(it) }
        speechStartTask = null
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
        const val VOICE_REQUEST = 1001
        const val MICROPHONE_REQUEST = 1002
        const val GREETING = "¡Hola! Soy Leitroboter. Le puedo ayudar a encontrar aulas, oficinas y laboratorios del cuarto piso del edificio anexo de la ECCI. Toque el botón para buscar un lugar."
        const val ASK_PLACE = "¿A dónde le gustaría ir?"
        val IGNORED_WORDS = setOf("de", "del", "la", "el", "los", "las", "y", "en", "a", "al")
    }
}
