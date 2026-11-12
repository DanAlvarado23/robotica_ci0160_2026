package com.example.leitroboter.matching

import com.example.leitroboter.matching.SilenceDetector.Decision
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceDetectorTest {
    private val random = Random(7)

    /** Bloques de 100 ms como los que lee WavRecorder. */
    private fun block(amplitude: Double, tone: Boolean): ShortArray = ShortArray(1_600) { i ->
        val signal = if (tone) amplitude * sin(2 * PI * 220 * i / 16_000.0) else 0.0
        val noise = (random.nextDouble() * 2 - 1) * 150
        (signal + noise).toInt().coerceIn(-32_768, 32_767).toShort()
    }

    private fun run(detector: SilenceDetector, blocks: List<ShortArray>): Pair<Decision, Int> {
        blocks.forEachIndexed { index, samples ->
            val decision = detector.feed(samples, samples.size)
            if (decision != Decision.CONTINUE) return decision to index
        }
        return Decision.CONTINUE to blocks.size
    }

    private fun quiet(count: Int) = List(count) { block(0.0, tone = false) }
    private fun voice(count: Int) = List(count) { block(6_000.0, tone = true) }

    @Test fun endsAboutOneSecondAfterSpeechStops() {
        val detector = SilenceDetector()
        // 0,5 s de ruido, 1,5 s de voz y luego silencio.
        val (decision, index) = run(detector, quiet(5) + voice(15) + quiet(30))
        assertEquals(Decision.END_OF_SPEECH, decision)
        assertTrue(detector.heardSpeech)
        // 12 bloques de silencio = 1,2 s después del bloque 19.
        assertEquals(31, index)
    }

    @Test fun pausesShorterThanTheLimitDoNotCutTheSentence() {
        val (decision, index) = run(SilenceDetector(), quiet(4) + voice(8) + quiet(8) + voice(8) + quiet(20))
        assertEquals(Decision.END_OF_SPEECH, decision)
        assertTrue("cortó en la pausa", index > 28)
    }

    @Test fun givesUpWhenNobodySpeaks() {
        val detector = SilenceDetector()
        val (decision, index) = run(detector, quiet(80))
        assertEquals(Decision.NO_SPEECH, decision)
        assertFalse(detector.heardSpeech)
        assertEquals(49, index)
    }

    @Test fun shortClicksAreNotSpeech() {
        val blocks = quiet(5) + voice(1) + quiet(10) + voice(1) + quiet(60)
        assertEquals(Decision.NO_SPEECH, run(SilenceDetector(), blocks).first)
    }

    @Test fun detectsSpeechEvenIfThePersonTalksDuringCalibration() {
        val (decision, _) = run(SilenceDetector(), voice(20) + quiet(20))
        assertEquals(Decision.END_OF_SPEECH, decision)
    }

    @Test fun louderBackgroundRaisesTheThreshold() {
        val detector = SilenceDetector()
        val fan = List(3) { block(800.0, tone = true) }
        run(detector, fan)
        assertTrue(detector.speechThreshold > SilenceDetector.MIN_SPEECH_RMS)
    }
}
