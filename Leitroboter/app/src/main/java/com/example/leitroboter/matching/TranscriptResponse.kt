package com.example.leitroboter.matching

/**
 * Acepta un único campo transcript con texto plano. Devuelve "" si no hubo voz
 * y null si la respuesta no cumple el contrato.
 */
object TranscriptResponse {
    const val MAX_RESPONSE_BYTES = 4096
    private val response = Regex(
        "[ \\t\\r\\n]*\\{[ \\t\\r\\n]*\"transcript\"[ \\t\\r\\n]*:[ \\t\\r\\n]*" +
            "\"((?:[^\"\\\\\\u0000-\\u001f]|\\\\[\"\\\\/bfnrt]|\\\\u[0-9a-fA-F]{4})*)\"" +
            "[ \\t\\r\\n]*\\}[ \\t\\r\\n]*"
    )

    fun parse(body: String): String? {
        if (body.toByteArray(Charsets.UTF_8).size > MAX_RESPONSE_BYTES) return null
        val raw = response.matchEntire(body)?.groupValues?.get(1) ?: return null
        val text = unescape(raw).trim()
        return when {
            text.isEmpty() -> ""
            RequestPolicy.isValid(text) -> text
            else -> null
        }
    }

    private fun unescape(raw: String): String {
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\') { out.append(c); i++; continue }
            when (val e = raw[i + 1]) {
                'u' -> { out.append(raw.substring(i + 2, i + 6).toInt(16).toChar()); i += 6; continue }
                'b' -> out.append('\b')
                'f' -> out.append('\u000c')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                else -> out.append(e)
            }
            i += 2
        }
        return out.toString()
    }
}
