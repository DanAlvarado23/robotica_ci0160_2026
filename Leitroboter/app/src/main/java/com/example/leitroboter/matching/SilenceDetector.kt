package com.example.leitroboter.matching

import kotlin.math.sqrt

/**
 * Decide cuándo terminó de hablar la persona a partir de la energía (RMS) del audio.
 *
 * Los primeros [CALIBRATION_MS] miden el ruido de fondo (ventiladores de Pepper, pasillo).
 * Hay voz cuando el RMS supera ese ruido por [SPEECH_FACTOR] durante [MIN_SPEECH_MS];
 * después de haber voz, [END_SILENCE_MS] de silencio seguido terminan la grabación.
 * Si en [NO_SPEECH_TIMEOUT_MS] nunca hubo voz, se descarta sin enviar nada.
 * Las constantes se calibran en el Pepper real.
 */
class SilenceDetector(private val sampleRate: Int = PcmWave.SAMPLE_RATE) {
    enum class Decision { CONTINUE, END_OF_SPEECH, NO_SPEECH }

    private var elapsedMs = 0.0
    private var noiseFloor = Double.MAX_VALUE
    private var loudMs = 0.0
    private var quietMs = 0.0
    var heardSpeech = false
        private set

    /** Umbral de voz actual, útil para registrar la calibración. */
    val speechThreshold: Double
        get() = maxOf(MIN_SPEECH_RMS, minOf(noiseFloor, MAX_NOISE_FLOOR) * SPEECH_FACTOR)

    fun feed(samples: ShortArray, count: Int): Decision {
        if (count <= 0) return Decision.CONTINUE
        val durationMs = count * 1_000.0 / sampleRate
        val level = rms(samples, count)
        elapsedMs += durationMs
        if (elapsedMs <= CALIBRATION_MS) {
            // El mínimo es más robusto si la persona empieza a hablar enseguida.
            noiseFloor = minOf(noiseFloor, level)
            return Decision.CONTINUE
        }
        if (level >= speechThreshold) {
            loudMs += durationMs
            quietMs = 0.0
            if (loudMs >= MIN_SPEECH_MS) heardSpeech = true
        } else {
            loudMs = 0.0
            if (heardSpeech) quietMs += durationMs
        }
        return when {
            heardSpeech && quietMs >= END_SILENCE_MS -> Decision.END_OF_SPEECH
            !heardSpeech && elapsedMs >= NO_SPEECH_TIMEOUT_MS -> Decision.NO_SPEECH
            else -> Decision.CONTINUE
        }
    }

    companion object {
        const val CALIBRATION_MS = 300.0
        const val MIN_SPEECH_MS = 200.0
        const val END_SILENCE_MS = 1_200.0
        const val NO_SPEECH_TIMEOUT_MS = 5_000.0
        /** Umbral mínimo de voz (escala de 0 a 32767), aunque el ruido de fondo sea muy bajo. */
        const val MIN_SPEECH_RMS = 500.0
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
