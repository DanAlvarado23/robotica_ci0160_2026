package com.example.leitroboter.matching

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/** Graba un clip en memoria y libera AudioRecord al finalizar. */
class WavRecorder {
    private val stopRequested = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private val recordingLock = Any()
    private var activeRecord: AudioRecord? = null

    @Suppress("MissingPermission")
    fun record(): ByteArray? {
        var record: AudioRecord? = null
        try {
            if (cancelled.get() || stopRequested.get() || Thread.currentThread().isInterrupted) return null
            val minimum = AudioRecord.getMinBufferSize(
                PcmWave.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minimum <= 0) return null
            val createdRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, PcmWave.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum * 2, 3_200)
            )
            record = createdRecord
            synchronized(recordingLock) {
                if (createdRecord.state != AudioRecord.STATE_INITIALIZED || cancelled.get() ||
                    stopRequested.get() || Thread.currentThread().isInterrupted) return null
                activeRecord = createdRecord
                createdRecord.startRecording()
            }
            if (createdRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) return null
            val deadline = System.nanoTime() + PcmWave.MAX_DURATION_MS * 1_000_000L
            val samples = ShortArray(1_600)
            val pcm = ByteArrayOutputStream(PcmWave.MAX_PCM_BYTES)
            while (!cancelled.get() && !stopRequested.get() &&
                !Thread.currentThread().isInterrupted && System.nanoTime() < deadline &&
                pcm.size() < PcmWave.MAX_PCM_BYTES) {
                val capacity = minOf(samples.size, (PcmWave.MAX_PCM_BYTES - pcm.size()) / 2)
                val count = createdRecord.read(samples, 0, capacity, AudioRecord.READ_BLOCKING)
                if (count < 0) {
                    if (stopRequested.get()) break
                    return null
                }
                if (count == 0) return null
                for (index in 0 until count) {
                    val value = samples[index].toInt()
                    pcm.write(value and 0xff)
                    pcm.write((value ushr 8) and 0xff)
                }
            }
            if (cancelled.get() || Thread.currentThread().isInterrupted) return null
            return PcmWave.wrap(pcm.toByteArray())
        } catch (_: Exception) {
            return null
        } finally {
            synchronized(recordingLock) {
                if (activeRecord === record) activeRecord = null
                try { record?.stop() } catch (_: Exception) { }
                try { record?.release() } catch (_: Exception) { }
            }
        }
    }

    /** Detiene la grabación y conserva el clip para enviarlo. */
    fun stop() {
        stopRequested.set(true)
        synchronized(recordingLock) {
            try { activeRecord?.stop() } catch (_: Exception) { }
        }
    }

    /** Cancela la grabación y descarta el clip. */
    fun cancel() {
        cancelled.set(true)
        stop()
    }
}
