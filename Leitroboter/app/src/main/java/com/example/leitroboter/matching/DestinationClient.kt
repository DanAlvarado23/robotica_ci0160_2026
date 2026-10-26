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

    fun matchAudio(wav: ByteArray): DestinationResult {
        if (!PcmWave.isCanonical(wav)) return DestinationResult.Failure
        val endpoint = DestinationEndpoint.forAudio(apiUrl) ?: return DestinationResult.Failure
        return post(endpoint, wav, "audio/wav")
    }

    private fun post(endpoint: String, payload: ByteArray, payloadType: String): DestinationResult {
        if (Thread.currentThread().isInterrupted) return DestinationResult.Failure
        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpoint)
            if (url.protocol != "https" && !(allowCleartext && url.protocol == "http")) {
                return DestinationResult.Failure
            }
            if (url.host.isBlank() || url.userInfo != null || url.ref != null ||
                apiToken.any { it == '\r' || it == '\n' }) return DestinationResult.Failure

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
            if (Thread.currentThread().isInterrupted) return DestinationResult.Failure
            connection.outputStream.use { it.write(payload) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return DestinationResult.Failure
            val contentType = connection.contentType?.substringBefore(';')?.trim()
            if (contentType != "application/json") return DestinationResult.Failure
            val output = ByteArrayOutputStream()
            connection.inputStream.use { stream ->
                val buffer = ByteArray(256)
                while (true) {
                    if (Thread.currentThread().isInterrupted) return DestinationResult.Failure
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > DestinationResponse.MAX_RESPONSE_BYTES) {
                        return DestinationResult.Failure
                    }
                    output.write(buffer, 0, count)
                }
            }
            return DestinationResponse.parse(output.toString("UTF-8"), allowedDestinations)
        } catch (_: Exception) {
            // La interfaz recibe un error genérico, sin detalles del proveedor.
            return DestinationResult.Failure
        } catch (_: LinkageError) {
            return DestinationResult.Failure
        } finally {
            activeConnection.compareAndSet(connection, null)
            connection?.disconnect()
        }
    }

    fun cancel() {
        activeConnection.getAndSet(null)?.disconnect()
    }
}
