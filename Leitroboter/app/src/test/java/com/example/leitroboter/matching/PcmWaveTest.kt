package com.example.leitroboter.matching

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmWaveTest {
    @Test fun emitsBoundedCanonicalPcm16Mono16k() {
        val wav = PcmWave.wrap(ByteArray(PcmWave.MAX_PCM_BYTES))!!
        assertEquals(256_044, wav.size)
        assertTrue(PcmWave.isCanonical(wav))
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(16_000, header.getInt(24))
        assertEquals(256_000, header.getInt(40))
    }

    @Test fun rejectsShortOversizedOddAndInvalidHeaders() {
        assertNull(PcmWave.wrap(ByteArray(PcmWave.MIN_PCM_BYTES - 2)))
        assertNull(PcmWave.wrap(ByteArray(PcmWave.MAX_PCM_BYTES + 2)))
        assertNull(PcmWave.wrap(ByteArray(PcmWave.MIN_PCM_BYTES + 1)))
        assertFalse(PcmWave.isCanonical(ByteArray(5)))
        val wrongChannels = PcmWave.wrap(ByteArray(PcmWave.MIN_PCM_BYTES))!!
        wrongChannels[22] = 2
        assertFalse(PcmWave.isCanonical(wrongChannels))
    }

    @Test fun derivesAudioEndpointOnlyFromValidatedMatchPath() {
        assertEquals("http://10.0.2.2:8787/match-audio", DestinationEndpoint.forAudio("http://10.0.2.2:8787/match"))
        assertEquals("https://example.com/service/match-audio", DestinationEndpoint.forAudio("https://example.com/service/match"))
        listOf(
            "https://example.com/match?redirect=/match", "https://user:secret@example.com/match",
            "https://example.com/other", "file:///match", "https://example.com/%2e%2e/match",
            "https://example.com/match#fragment", "https://example.com/../match"
        ).forEach { assertNull(it, DestinationEndpoint.forAudio(it)) }
    }
}
