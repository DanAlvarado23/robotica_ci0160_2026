package com.example.leitroboter.matching

sealed class DestinationResult {
    data class Match(val id: String) : DestinationResult()
    object NoMatch : DestinationResult()
    object Failure : DestinationResult()
}

/** Acepta un único campo destination con un ID del catálogo o NO_MATCH. */
object DestinationResponse {
    const val MAX_RESPONSE_BYTES = 512
    private val response = Regex(
        "[ \\t\\r\\n]*\\{[ \\t\\r\\n]*\"destination\"[ \\t\\r\\n]*:[ \\t\\r\\n]*\"([A-Za-z0-9_-]{1,64})\"[ \\t\\r\\n]*\\}[ \\t\\r\\n]*"
    )

    fun parse(body: String, allowedDestinations: Set<String>): DestinationResult {
        if (body.toByteArray(Charsets.UTF_8).size > MAX_RESPONSE_BYTES) return DestinationResult.Failure
        val id = response.matchEntire(body)?.groupValues?.get(1) ?: return DestinationResult.Failure
        return when {
            id == "NO_MATCH" -> DestinationResult.NoMatch
            id in allowedDestinations -> DestinationResult.Match(id)
            else -> DestinationResult.Failure
        }
    }
}
