package com.example.leitroboter.matching

import kotlin.math.sqrt

class SilenceDetector(private val sampleRate: Int = PcmWave.SAMPLE_RATE) {
    enum class Decision { CONTINUE, END_OF_SPEECH, NO_SPEECH }

    private var elapsedMs = 0.0
    private var noiseFloor = Double.MAX_VALUE
    private var loudMs = 0.0
    private var quietMs = 0.0
    private val initialLevels = mutableListOf<Pair<Double, Double>>()
    var heardSpeech = false
        private set

    /** RMS más alto escuchado después de calibrar; sirve para ajustar los umbrales. */
    var peakLevel = 0.0
        private set

    /** Ruido de fondo medido, o 0 si todavía no hay calibración. */
    val measuredNoise: Double
        get() = if (noiseFloor == Double.MAX_VALUE) 0.0 else noiseFloor

    /** Umbral de voz actual, útil para registrar la calibración. */
    val speechThreshold: Double
        get() = maxOf(MIN_SPEECH_RMS, minOf(noiseFloor, MAX_NOISE_FLOOR) * SPEECH_FACTOR)

    fun feed(samples: ShortArray, count: Int): Decision {
        if (count <= 0) return Decision.CONTINUE
        val durationMs = count * 1_000.0 / sampleRate
        val level = rms(samples, count)
        elapsedMs += durationMs
        peakLevel = maxOf(peakLevel, level)
        if (!heardSpeech) {
            noiseFloor = minOf(noiseFloor, level)
            initialLevels.add(level to durationMs)
            if (elapsedMs < CALIBRATION_MS) return Decision.CONTINUE
            loudMs = 0.0
            quietMs = 0.0
            for ((initialLevel, initialDuration) in initialLevels) {
                updateSpeech(initialLevel, initialDuration)
            }
            if (heardSpeech) initialLevels.clear()
        } else {
            updateSpeech(level, durationMs)
        }
        return when {
            heardSpeech && quietMs >= END_SILENCE_MS -> Decision.END_OF_SPEECH
            !heardSpeech && elapsedMs >= NO_SPEECH_TIMEOUT_MS -> Decision.NO_SPEECH
            else -> Decision.CONTINUE
        }
    }

    private fun updateSpeech(level: Double, durationMs: Double) {
        if (level >= speechThreshold) {
            loudMs += durationMs
            quietMs = 0.0
            if (loudMs >= MIN_SPEECH_MS) heardSpeech = true
        } else {
            loudMs = 0.0
            if (heardSpeech) quietMs += durationMs
        }
    }

    companion object {
        const val CALIBRATION_MS = 300.0
        const val MIN_SPEECH_MS = 200.0
        const val END_SILENCE_MS = 1_200.0
        const val NO_SPEECH_TIMEOUT_MS = 5_000.0
        /** Umbral mínimo de voz (escala de 0 a 32767), aunque el ruido de fondo sea muy bajo. */
        const val MIN_SPEECH_RMS = 150.0
        /** Ruido máximo que se acepta como base, por si la calibración capta voz. */
        const val MAX_NOISE_FLOOR = 1_500.0
        const val SPEECH_FACTOR = 2.5

        fun rms(samples: ShortArray, count: Int): Double {
            var sum = 0.0
            for (index in 0 until count) {
                val value = samples[index].toDouble()
                sum += value * value
            }
            return sqrt(sum / count)
        }
    }
}
