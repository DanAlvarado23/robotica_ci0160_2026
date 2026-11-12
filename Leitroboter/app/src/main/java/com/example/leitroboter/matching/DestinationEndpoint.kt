package com.example.leitroboter.matching

import java.net.URI

object DestinationEndpoint {
    /** Valida la URL de texto y cambia su ruta final a /transcribe-audio. */
    fun forTranscription(textEndpoint: String): String? = sibling(textEndpoint, "/transcribe-audio")

    private fun sibling(textEndpoint: String, suffix: String): String? = try {
        val endpoint = URI(textEndpoint)
        val path = endpoint.path.orEmpty()
        if ((endpoint.scheme != "https" && endpoint.scheme != "http") ||
            endpoint.host.isNullOrBlank() || endpoint.userInfo != null ||
            endpoint.rawQuery != null || endpoint.rawFragment != null ||
            endpoint.rawPath != path ||
            !Regex("(?:/[A-Za-z0-9_-]+)*/match").matches(path)) {
            null
        } else {
            URI(
                endpoint.scheme, null, endpoint.host, endpoint.port,
                path.removeSuffix("/match") + suffix, null, null
            ).toASCIIString()
        }
    } catch (_: Exception) { null }
}
