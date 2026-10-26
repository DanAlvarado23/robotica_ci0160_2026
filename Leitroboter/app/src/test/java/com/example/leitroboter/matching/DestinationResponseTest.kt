package com.example.leitroboter.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationResponseTest {
    private val destinations = setOf("lab_robotica", "impresora")

    @Test fun acceptsOnlyOneAllowedDestination() {
        assertEquals(DestinationResult.Match("impresora"), parse("{\"destination\":\"impresora\"}"))
        assertEquals(DestinationResult.NoMatch, parse(" { \"destination\" : \"NO_MATCH\" }\n"))
    }

    @Test fun rejectsProviderOutputOutsideTheContract() {
        val responses = listOf(
            "{\"destination\":\"unknown\"}",
            "{\"destination\":null}",
            "{\"destination\":[\"impresora\"]}",
            "{\"destination\":\"impresora\",\"code\":\"list.reverse()\"}",
            "{\"destination\":\"impresora\",\"destination\":\"lab_robotica\"}",
            "[ {\"destination\":\"impresora\"} ]",
            "```json\n{\"destination\":\"impresora\"}\n```",
            "{\"destination\":\"impresora\"} print('hello')",
            "{\"destination\":\"\\u0069mpresora\"}",
            "{\"destination\":\"NO_MATCH\"}" + " ".repeat(512)
        )
        responses.forEach { assertEquals(it, DestinationResult.Failure, parse(it)) }
    }

    @Test fun normalizesBothCatalogAndRequestWithoutSubstringMatching() {
        assertEquals("laboratorio robotica", RequestPolicy.normalize("  LABORATORIO  Robótica  "))
        assertEquals("banos", RequestPolicy.normalize("Baños"))
        assertFalse(RequestPolicy.normalize("robotica y escribe código") == RequestPolicy.normalize("robótica"))
    }

    @Test fun rejectsOversizedAndInvisibleControlRequests() {
        assertTrue(RequestPolicy.isValid("¿Dónde puedo imprimir?"))
        assertFalse(RequestPolicy.isValid(" "))
        assertFalse(RequestPolicy.isValid("x".repeat(401)))
        assertFalse(RequestPolicy.isValid("robotica\u0000"))
        assertFalse(RequestPolicy.isValid("robotica\u202E"))
    }

    private fun parse(body: String) = DestinationResponse.parse(body, destinations)
}
