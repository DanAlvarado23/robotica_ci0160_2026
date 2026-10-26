package com.example.leitroboter

import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.leitroboter.data.Place
import com.example.leitroboter.data.PlacesCatalog
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConsecutiveSearchTest {
    @Test
    fun anotherLocationClearsInputAndShowsASecondDestination() {
        val first = PlacesCatalog.places.first { it.id == "laboratorio_robotica_4_17" }
        val second = PlacesCatalog.places.first { it.id == "banos" }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.searchButton)).perform(click())
            searchExact(first)
            onView(withId(R.id.placeName)).check(matches(withText(displayName(first))))
            onView(withId(R.id.newSearchButton)).perform(click())
            onView(withId(R.id.placeInput)).check(matches(withText("")))
            scenario.onActivity {
                assertEquals("", it.findViewById<EditText>(R.id.placeInput).text.toString())
            }
            searchExact(second)
            onView(withId(R.id.placeName)).check(matches(withText(displayName(second))))
            onView(withId(R.id.backButton)).perform(click())
            onView(withId(R.id.searchButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun anotherLocationAlsoClearsTheNoMatchResult() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.searchButton)).perform(click())
            onView(withId(R.id.placeInput)).perform(replaceText("texto anterior"), closeSoftKeyboard())
            // Fuerza el resultado sin coincidencia para probar la navegación sin depender de la red.
            scenario.onActivity { activity ->
                MainActivity::class.java.getDeclaredMethod("showNotFound").apply {
                    isAccessible = true
                }.invoke(activity)
            }
            onView(withId(R.id.placeName)).check(matches(withText(R.string.place_not_found)))
            onView(withId(R.id.newSearchButton)).perform(click())
            onView(withId(R.id.placeInput)).check(matches(withText("")))
            searchExact(PlacesCatalog.places.first { it.id == "banos" })
            onView(withId(R.id.newSearchButton)).check(matches(isDisplayed()))
        }
    }

    private fun searchExact(place: Place) {
        onView(withId(R.id.placeInput)).perform(replaceText(place.name), closeSoftKeyboard())
        onView(withId(R.id.submitPlaceButton)).perform(click())
    }

    private fun displayName(place: Place): String =
        if (place.roomNumber != null) "${place.name} (${place.roomNumber})" else place.name
}
