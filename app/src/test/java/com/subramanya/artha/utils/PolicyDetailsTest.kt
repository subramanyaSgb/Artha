package com.subramanya.artha.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDetailsTest {

    @Test
    fun `full json parses members coverage contacts`() {
        val json = """
            {
              "members": [
                {"name": "Subramanya", "relation": "Self", "age": "30"},
                {"name": "Spouse", "relation": "Wife"}
              ],
              "riders": [{"name": "Critical Illness", "premium": "1200", "note": "Optional"}],
              "coverage": [
                {"label": "Room rent", "value": "No cap"},
                {"label": "Maternity", "value": "Covered"}
              ],
              "exclusions": ["Cosmetic surgery", "Pre-existing (2 yrs)"],
              "contacts": {"helpline": "1800-123-456", "claimsEmail": "claims@acme.com"},
              "premiumBreakdown": {"base": "10000", "gst": "1800", "total": "11800"},
              "status": "Active"
            }
        """.trimIndent()

        val d = parsePolicyDetails(json)
        assertNotNull(d)
        requireNotNull(d)

        assertEquals(2, d.members.size)
        assertEquals("Subramanya", d.members[0].name)
        assertEquals("Self", d.members[0].relation)
        assertEquals("30", d.members[0].age)
        // second member has no age -> null, still kept (name present)
        assertNull(d.members[1].age)

        assertEquals(1, d.riders.size)
        assertEquals("Critical Illness", d.riders[0].name)

        assertEquals(2, d.coverage.size)
        assertEquals("Room rent", d.coverage[0].label)
        assertEquals("No cap", d.coverage[0].value)

        assertEquals(2, d.exclusions.size)

        assertNotNull(d.contacts)
        assertEquals("1800-123-456", d.contacts?.helpline)
        assertEquals("claims@acme.com", d.contacts?.claimsEmail)
        assertNull(d.contacts?.branch)

        assertNotNull(d.premiumBreakdown)
        assertEquals("11800", d.premiumBreakdown?.total)

        assertEquals("Active", d.status)
    }

    @Test
    fun `malformed json returns null without throwing`() {
        assertNull(parsePolicyDetails("{ not valid json"))
        assertNull(parsePolicyDetails("[]")) // wrong root type
    }

    @Test
    fun `missing keys yield empty lists and null objects`() {
        val d = parsePolicyDetails("""{"status": "Lapsed"}""")
        assertNotNull(d)
        requireNotNull(d)
        assertTrue(d.members.isEmpty())
        assertTrue(d.riders.isEmpty())
        assertTrue(d.coverage.isEmpty())
        assertTrue(d.exclusions.isEmpty())
        assertNull(d.contacts)
        assertNull(d.premiumBreakdown)
        assertEquals("Lapsed", d.status)
    }

    @Test
    fun `null and blank input return null`() {
        assertNull(parsePolicyDetails(null))
        assertNull(parsePolicyDetails("   "))
    }

    @Test
    fun `empty object returns null since nothing renders`() {
        assertNull(parsePolicyDetails("{}"))
    }
}
