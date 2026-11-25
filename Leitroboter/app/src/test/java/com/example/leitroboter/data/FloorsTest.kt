package com.example.leitroboter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FloorsTest {
    private fun place(id: String, floor: Int, group: String?) = Place(
        id = id, name = "Baños", roomNumber = null, floor = floor, group = group, mapImageRes = 0,
        description = "", directions = "", gestureAnimationRes = null, aliases = listOf("baños")
    )

    private val catalog = listOf(
        place("banos", 4, "banos"), place("banos_2", 2, "banos"), place("banos_6", 6, "banos"),
        place("auditorio_1", 1, null)
    )

    @Test fun readsFloorFromRequest() {
        assertEquals(2, Floors.requestedFloor("el baño del piso 2"))
        assertEquals(2, Floors.requestedFloor("Baños segundo piso"))
        assertEquals(6, Floors.requestedFloor("baño en el piso seis"))
        assertEquals(5, Floors.requestedFloor("quinto nivel"))
        assertEquals(3, Floors.requestedFloor("3er piso"))
        assertNull(Floors.requestedFloor("baños"))
        assertEquals(9, Floors.requestedFloor("piso 9"))
        assertNull(Floors.requestedFloor("aula 4-6"))
    }

    @Test fun repeatedPlaceAsksForFloorUnlessGiven() {
        val banos = catalog.first()
        assertEquals(listOf("banos_2", "banos", "banos_6"), Floors.candidates(banos, "baños", catalog).map { it.id })
        assertEquals(listOf("banos_6"), Floors.candidates(banos, "baños del sexto piso", catalog).map { it.id })
        assertEquals(emptyList<Place>(), Floors.candidates(banos, "baños del piso 3", catalog))
        assertEquals(listOf("auditorio_1"), Floors.candidates(catalog.last(), "auditorio", catalog).map { it.id })
    }

    @Test fun doesNotSubstituteAnotherFloorForAUniquePlace() {
        assertEquals(emptyList<Place>(), Floors.candidates(catalog.last(), "auditorio del piso 6", catalog))
        assertEquals(emptyList<Place>(), Floors.candidates(catalog.first(), "baños del piso 9", catalog))
    }

    @Test fun exactRoomDoesNotAskForItsFloorAgain() {
        val fourth = place("pi_4", 4, "pi").copy(roomNumber = "4-6", aliases = listOf("pi", "4-6", "46"))
        val third = place("pi_3", 3, "pi").copy(roomNumber = "3-5", aliases = listOf("pi", "3-5", "35"))
        val places = listOf(third, fourth)
        for (request in listOf("4-6", "46", "quiero ir al aula 4-6")) {
            assertEquals(listOf(fourth), Floors.candidates(fourth, request, places))
        }
        assertEquals(emptyList<Place>(), Floors.candidates(fourth, "4-6 del piso 3", places))
    }

    @Test fun stripsOnlyTheFloorMentionFromAnExactSearch() {
        assertEquals("banos", Floors.withoutFloor("Baños del piso dos"))
        assertEquals("banos", Floors.withoutFloor("Baños en el segundo piso"))
        assertEquals("4 6", Floors.withoutFloor("4-6"))
        assertEquals("idf", Floors.withoutFloor("IDF del piso 5"))
    }

    @Test fun exactMatchesFromOneGroupOnly() {
        assertEquals("banos", Floors.groupOf(catalog.take(2)))
        assertNull(Floors.groupOf(catalog.takeLast(2)))
    }
}
