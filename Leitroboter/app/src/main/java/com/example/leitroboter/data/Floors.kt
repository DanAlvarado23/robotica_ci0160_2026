package com.example.leitroboter.data

import com.example.leitroboter.matching.RequestPolicy

/** Reglas de pisos: en qué piso está Pepper y cómo elegir entre lugares repetidos en varios pisos. */
object Floors {
    /** Piso de Pepper si no se configura otro (res/values/config.xml, pepper_floor). */
    const val DEFAULT_PEPPER_FLOOR = 4
    val ALL = 1..6

    private val ordinals = mapOf(
        "primer" to 1, "primero" to 1, "1er" to 1, "1ro" to 1,
        "segundo" to 2, "2do" to 2,
        "tercer" to 3, "tercero" to 3, "3er" to 3, "3ro" to 3,
        "cuarto" to 4, "4to" to 4,
        "quinto" to 5, "5to" to 5,
        "sexto" to 6, "6to" to 6
    )
    private val cardinals = mapOf(
        "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6
    )

    /** Piso que la persona pidió explícitamente ("baño del piso 2", "segundo piso"), o null. */
    fun requestedFloor(request: String): Int? {
        val words = RequestPolicy.normalize(request).replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ").filter { it.isNotEmpty() }
        for ((index, word) in words.withIndex()) {
            if (word == "piso" || word == "nivel") {
                // "piso 2", "piso dos"
                words.getOrNull(index + 1)?.let { next -> number(next)?.let { return it } }
                // "segundo piso", "2 piso"
                words.getOrNull(index - 1)?.let { previous ->
                    (ordinals[previous] ?: number(previous))?.let { return it }
                }
            }
        }
        return null
    }

    private fun number(word: String): Int? =
        (word.toIntOrNull() ?: cardinals[word] ?: ordinals[word])?.takeIf { it in 1..99 }

    private val floorNumberWords = "(?:[0-9]{1,2}|${(cardinals.keys + ordinals.keys).joinToString("|")})"
    private val floorMention = Regex(
        "\\b(?:(?:en|de|del|al) )?(?:el )?(?:(?:piso|nivel) $floorNumberWords|$floorNumberWords (?:piso|nivel))\\b"
    )

    fun withoutFloor(request: String): String =
        RequestPolicy.normalize(request).replace(floorMention, " ")
            .replace(Regex("\\s+"), " ").trim()

    /**
     * Lugares entre los que la persona debe elegir piso. Si el lugar es único o la persona dijo
     * el piso, la lista tiene un solo lugar.
     */
    fun candidates(place: Place, request: String, catalog: List<Place>): List<Place> {
        val sameGroup = if (place.group == null) listOf(place)
            else catalog.filter { it.group == place.group }.sortedBy { it.floor }
        val query = withoutFloor(request)
        val exact = sameGroup.filter { candidate ->
            RequestPolicy.normalize(candidate.name) == query ||
                candidate.aliases.any { RequestPolicy.normalize(it) == query }
        }
        val room = Regex("\\b([1-6])\\s*[-–]\\s*([0-9]{1,2})\\b").find(request)?.let {
            "${it.groupValues[1]} ${it.groupValues[2]}"
        }
        val candidates = when {
            exact.size == 1 -> exact
            room != null -> sameGroup.filter { RequestPolicy.normalize(it.roomNumber.orEmpty()) == room }
            else -> sameGroup
        }
        val floor = requestedFloor(request) ?: return candidates
        return candidates.filter { it.floor == floor }
    }

    /** Coincidencias exactas de varios pisos del mismo grupo; null si no son un solo grupo. */
    fun groupOf(matches: List<Place>): String? =
        matches.mapNotNull { it.group }.distinct().singleOrNull()
            ?.takeIf { group -> matches.all { it.group == group } }
}
