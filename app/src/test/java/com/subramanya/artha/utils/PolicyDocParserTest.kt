package com.subramanya.artha.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unit test for [PolicyDocParser.decode] — exercises the JSON→[PolicyData] mapping without
 * any network. Uses a real-sample policy JSON.
 */
class PolicyDocParserTest {

    private val sample = """
        {"name":"Care Supreme","provider":"Meridian Health Insurance Ltd.","type":"HEALTH",
         "policyNumber":"92838249","sumAssured":10000000,"premiumAmount":64780,
         "premiumFrequency":"SINGLE","startDate":"2024-11-22","endDate":"2026-11-21",
         "nominee":"Lakshmi Gopala","taxSection":"80D","planName":"Care Supreme — Floater",
         "policyTerm":"2 years","lifeAssured":"Gopala Krishnan","uin":"MHIHLIP24063V012425",
         "insurerHelpline":"1800 266 4545",
         "members":[{"name":"Gopala Krishnan","relation":"Self","age":42}]}
    """.trimIndent()

    /**
     * parseDate → buildDate parses "yyyy-MM-dd" via a lenient-off SimpleDateFormat with NO
     * timezone set, i.e. the JVM default timezone at local midnight. Derive the expected
     * epoch millis exactly the same way so the assertion holds on any runner.
     */
    private fun expectedMillis(date: String): Long =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!.time

    @Test
    fun decode_mapsAllCoreFields() {
        val parser = PolicyDocParser()
        val data = parser.decodeForTest(sample)
        assertNotNull(data)
        requireNotNull(data)

        assertEquals("Care Supreme", data.name)
        assertEquals("Meridian Health Insurance Ltd.", data.provider)
        assertEquals(10000000.0, data.sumAssured!!, 0.0)
        assertEquals(64780.0, data.premiumAmount!!, 0.0)
        assertEquals("92838249", data.policyNumber)
        assertEquals("Lakshmi Gopala", data.nominee)
        assertEquals("80D", data.taxSection)
        assertEquals("Care Supreme — Floater", data.planName)
        assertEquals("2 years", data.policyTerm)
        assertEquals("Gopala Krishnan", data.lifeAssured)
        assertEquals("MHIHLIP24063V012425", data.uin)
        assertEquals("1800 266 4545", data.insurerHelpline)
        assertEquals("HEALTH", data.typeHint)
        assertEquals("SINGLE", data.premiumFrequencyHint)

        assertEquals(expectedMillis("2024-11-22"), data.startDateMillis)
        assertEquals(expectedMillis("2026-11-21"), data.endDateMillis)

        assertNotNull(data.detailsJson)
        assertTrue(data.detailsJson!!.contains("Gopala Krishnan"))
    }
}
