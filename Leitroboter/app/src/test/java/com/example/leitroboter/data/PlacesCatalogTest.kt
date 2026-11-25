package com.example.leitroboter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesCatalogTest {
    @Test fun exactRoomRemainsUniqueWithinAGroup() {
        val places = PlacesCatalog.exactMatches("AULA 4-6")
        assertEquals(1, places.size)
        assertEquals("tripleta_4_6", places.single().id)
        assertEquals(listOf(places.single()), Floors.candidates(places.single(), "AULA 4-6", PlacesCatalog.places))
    }

    @Test fun resolvesSharedAliasesOnTheRequestedFloorWithoutNetwork() {
        for ((request, id) in listOf(
            "baños del piso dos" to "banos_2",
            "baños del sexto piso" to "banos_6",
            "ascensor piso 6" to "ascensor_6",
            "sala de reuniones del sexto piso" to "reuniones_6_2",
            "oficina de Kryscia" to "kryscia_6_4",
            "aula 6-6" to "aula_6_6",
            "secretaría del quinto piso" to "sala_5_15",
            "IDF del piso 5" to "sala_5_22"
        )) {
            val matches = PlacesCatalog.exactMatches(request)
            assertTrue(request, matches.isNotEmpty())
            val resolved = Floors.candidates(matches.first(), request, PlacesCatalog.places)
            assertEquals(request, id, resolved.single().id)
        }
        val auditorio = PlacesCatalog.exactMatches("auditorio del sexto piso").single()
        assertTrue(Floors.candidates(auditorio, "auditorio del sexto piso", PlacesCatalog.places).isEmpty())
    }

    @Test fun destinationsShareTheirFloorMapAndHaveNormalizedRoutes() {
        assertEquals(6, PlacesCatalog.places.map { it.mapImageRes }.distinct().size)
        for (place in PlacesCatalog.places) {
            assertTrue(place.id, place.routes.isNotEmpty())
            assertEquals(1, PlacesCatalog.places.filter { it.floor == place.floor }.map { it.mapImageRes }.distinct().size)
            for (route in place.routes) {
                assertTrue(route.points.isNotEmpty())
                assertTrue(route.points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f })
            }
        }
    }

    @Test fun numberedNamesDoNotHideTheRoomNumber() {
        val place = PlacesCatalog.places.first { it.id == "sala_5_12" }
        assertEquals(listOf(place), Floors.candidates(place, "quiero ir a la sala de investigación 1 5-12", PlacesCatalog.places))
    }
}
