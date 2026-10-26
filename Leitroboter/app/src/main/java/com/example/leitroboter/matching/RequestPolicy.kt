package com.example.leitroboter.matching

import java.text.Normalizer
import java.util.Locale

object RequestPolicy {
    const val MAX_REQUEST_LENGTH = 400

    fun isValid(text: String): Boolean = text.isNotBlank() &&
        text.length <= MAX_REQUEST_LENGTH &&
        text.none { Character.isISOControl(it) || Character.getType(it) == Character.FORMAT.toInt() }

    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")
}
