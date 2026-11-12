package com.example.leitroboter.matching

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class DestinationClient(
    private val apiUrl: String,
    private val apiToken: String,
    private val allowedDestinations: Set<String>,
    private val allowCleartext: Boolean = false
) {
    private val activeConnection = AtomicReference<HttpURLConnection?>()

    fun match(text: String): DestinationResult {
        if (!RequestPolicy.isValid(text)) return DestinationResult.Failure
        val payload = JSONObject().put("text", text).toString().toByteArray(Charsets.UTF_8)
        return post(apiUrl, payload, "application/json; charset=utf-8")
    }

    /** Devuelve lo que dijo la persona, "" si no se entendió voz, o null si falló el servicio. */
    fun transcribeAudio(wav: ByteArray): String? {
        if (!PcmWave.isCanonical(wav)) return null
        val endpoint = DestinationEndpoint.forTranscription(apiUrl) ?: return null
        val body = postRaw(endpoint, wav, "audio/wav", TranscriptResponse.MAX_RESPONSE_BYTES) ?: return null
        return TranscriptResponse.parse(body)
    }

    private fun post(endpoint: String, payload: ByteArray, payloadType: String): DestinationResult {
        val body = postRaw(endpoint, payload, payloadType, DestinationResponse.MAX_RESPONSE_BYTES)
            ?: return DestinationResult.Failure
        return DestinationResponse.parse(body, allowedDestinations)
    }

    private fun postRaw(endpoint: String, payload: ByteArray, payloadType: String, maxBytes: Int): String? {
        if (Thread.currentThread().isInterrupted) return null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            if (url.protocol != "https" && !(allowCleartext && url.protocol == "http")) {
                return null
            }
            if (url.host.isBlank() || url.userInfo != null || url.ref != null ||
                apiToken.any { it == '\r' || it == '\n' }) return null

            connection = url.openConnection() as HttpURLConnection
            activeConnection.set(connection)
            connection.apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 5_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", payloadType)
                setRequestProperty("Accept", "application/json")
                if (apiToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiToken")
            }
            connection.setFixedLengthStreamingMode(payload.size)
            if (Thread.currentThread().isInterrupted) return null
            connection.outputStream.use { it.write(payload) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val contentType = connection.contentType?.substringBefore(';')?.trim()
            if (contentType != "application/json") return null
            val output = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buffer = ByteArray(256)
                while (true) {
                    if (Thread.currentThread().isInterrupted) return null
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > maxBytes) {
                        return null
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString("UTF-8")
        } catch (_: Exception) {
            // La interfaz recibe un error genérico, sin detalles del proveedor.
            return null
        } catch (_: LinkageError) {
            return null
        } finally {
            activeConnection.compareAndSet(connection, null)
            connection?.disconnect()
        }
    }

    fun cancel() {
        activeConnection.getAndSet(null)?.disconnect()
    }
}
