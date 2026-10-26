package com.example.leitroboter.matching

import java.net.URI

object DestinationEndpoint {
    /** Valida la URL de texto y cambia su ruta final a /match-audio. */
    fun forAudio(textEndpoint: String): String? = try {
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
                path.removeSuffix("/match") + "/match-audio", null, null
            ).toASCIIString()
        }
    } catch (_: Exception) { null }
}
