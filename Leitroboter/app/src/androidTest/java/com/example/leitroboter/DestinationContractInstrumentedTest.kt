package com.example.leitroboter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.leitroboter.matching.DestinationResponse
import com.example.leitroboter.matching.DestinationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica las expresiones regulares con ICU de Android, distinto del motor de la JVM. */
@RunWith(AndroidJUnit4::class)
class DestinationContractInstrumentedTest {
    @Test fun parsesCanonicalIdUsingAndroidRegexEngine() {
        assertEquals(
            DestinationResult.Match("laboratorio_robotica_4_17"),
            DestinationResponse.parse(
                "{\"destination\":\"laboratorio_robotica_4_17\"}",
                setOf("laboratorio_robotica_4_17")
            )
        )
    }

    @Test fun refusesExtraKeysAndUnknownIdsUsingAndroidRegexEngine() {
        for (body in listOf(
            "{\"destination\":\"banos\",\"extra\":\"code\"}",
            "{\"destination\":\"banos\",\"destination\":\"banos\"}",
            "{\"destination\":\"python_code\"}"
        )) {
            assertSame(DestinationResult.Failure, DestinationResponse.parse(body, setOf("banos")))
        }
    }
}
