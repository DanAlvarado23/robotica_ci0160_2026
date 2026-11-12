package com.example.leitroboter.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptResponseTest {
    @Test fun acceptsOneTranscriptField() {
        assertEquals("¿Dónde está el baño?", TranscriptResponse.parse("{\"transcript\":\"¿Dónde está el baño?\"}"))
        assertEquals("quiero \"imprimir\"", TranscriptResponse.parse(" { \"transcript\" : \"quiero \\\"imprimir\\\"\" }\n"))
        assertEquals("baño", TranscriptResponse.parse("{\"transcript\":\"ba\\u00f1o\"}"))
        assertEquals("", TranscriptResponse.parse("{\"transcript\":\"\"}"))
        assertEquals("", TranscriptResponse.parse("{\"transcript\":\"   \"}"))
    }

    @Test fun rejectsOutputOutsideTheContract() {
        listOf(
            "{\"transcript\":null}",
            "{\"transcript\":\"baño\",\"extra\":\"x\"}",
            "{\"transcript\":\"baño\",\"transcript\":\"impresora\"}",
            "{\"destination\":\"banos\"}",
            "[\"baño\"]",
            "{\"transcript\":\"baño\"} trailing",
            "{\"transcript\":\"línea\\nnueva\"}",
            "{\"transcript\":\"" + "a".repeat(401) + "\"}",
            "{\"transcript\":\"baño\"}" + " ".repeat(TranscriptResponse.MAX_RESPONSE_BYTES)
        ).forEach { assertNull(it, TranscriptResponse.parse(it)) }
    }
}
