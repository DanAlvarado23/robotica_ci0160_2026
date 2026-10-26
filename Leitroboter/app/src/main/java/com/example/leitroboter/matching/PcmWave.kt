package com.example.leitroboter.matching

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** WAV PCM de 16 bits little-endian, mono, a 16 kHz. */
object PcmWave {
    const val SAMPLE_RATE = 16_000
    const val MAX_DURATION_MS = 8_000L
    const val HEADER_BYTES = 44
    const val MIN_PCM_BYTES = 8_000 // 250 ms de audio PCM.
    const val MAX_PCM_BYTES = 256_000
    const val MAX_WAV_BYTES = HEADER_BYTES + MAX_PCM_BYTES

    fun wrap(pcm: ByteArray): ByteArray? {
        if (pcm.size !in MIN_PCM_BYTES..MAX_PCM_BYTES || pcm.size % 2 != 0) return null
        return ByteBuffer.allocate(HEADER_BYTES + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcm.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcm.size)
            put(pcm)
        }.array()
    }

    fun isCanonical(wav: ByteArray): Boolean {
        if (wav.size !in (HEADER_BYTES + MIN_PCM_BYTES)..MAX_WAV_BYTES ||
            (wav.size - HEADER_BYTES) % 2 != 0) return false
        val pcmLength = wav.size - HEADER_BYTES
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        return String(wav, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            header.getInt(4) == wav.size - 8 &&
            String(wav, 8, 4, Charsets.US_ASCII) == "WAVE" &&
            String(wav, 12, 4, Charsets.US_ASCII) == "fmt " &&
            header.getInt(16) == 16 && header.getShort(20).toInt() == 1 &&
            header.getShort(22).toInt() == 1 && header.getInt(24) == SAMPLE_RATE &&
            header.getInt(28) == SAMPLE_RATE * 2 &&
            header.getShort(32).toInt() == 2 && header.getShort(34).toInt() == 16 &&
            String(wav, 36, 4, Charsets.US_ASCII) == "data" &&
            header.getInt(40) == pcmLength
    }
}
