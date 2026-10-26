package com.example.leitroboter

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.builder.SayBuilder
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Requiere Pepper o su simulador y el argumento de instrumentación -e robotSpeech true. */
@RunWith(AndroidJUnit4::class)
class PepperSpeechInstrumentedTest {
    @Test fun robotFocusCanRunSpeech() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("robotSpeech") == "true")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val contextField = MainActivity::class.java.getDeclaredField("qiContext").apply {
                isAccessible = true
            }
            var context: QiContext? = null
            val focusDeadline = SystemClock.elapsedRealtime() + 30000
            while (context == null && SystemClock.elapsedRealtime() < focusDeadline) {
                scenario.onActivity { context = contextField.get(it) as QiContext? }
                if (context == null) SystemClock.sleep(100)
            }
            assertNotNull("Pepper did not grant robot focus", context)

            // Espera a que termine el saludo antes de probar Say.
            var previousSpeech = emptyList<Future<*>>()
            scenario.onActivity { activity ->
                MainActivity::class.java.getDeclaredMethod("stopSpeaking").apply {
                    isAccessible = true
                }.invoke(activity)
                val futuresField = MainActivity::class.java.getDeclaredField("speechFutures").apply {
                    isAccessible = true
                }
                previousSpeech = (futuresField.get(activity) as Collection<*>)
                    .filterIsInstance<Future<*>>().toList()
            }
            val silenceDeadline = SystemClock.elapsedRealtime() + 5000
            while (previousSpeech.any { !it.isDone } && SystemClock.elapsedRealtime() < silenceDeadline) {
                SystemClock.sleep(50)
            }
            assertTrue("Previous robot speech did not stop", previousSpeech.all { it.isDone })
            val say = SayBuilder.with(context!!)
                .withText("Prueba de voz de Leitroboter completada.")
                .buildAsync().get(10, TimeUnit.SECONDS)
            val spoken = say.async().run()
            spoken.get(20, TimeUnit.SECONDS)
            assertTrue("Pepper speech failed", spoken.isSuccess)
        }
    }
}
