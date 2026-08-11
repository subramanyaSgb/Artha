package com.subramanya.artha.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the owner-name anchoring that fixes two intermittent UPI-share bugs:
 * the model returning the app owner as the merchant, and flipping income/expense.
 */
class UpiReceiptParserTest {

    private val parser = UpiReceiptParser()
    private val owner = "Subramanya Gopal Bellary"

    // ---- isOwnerName: the merchant post-filter ----

    @Test fun `exact owner name matches`() {
        assertTrue(parser.isOwnerName("Subramanya Gopal Bellary", owner))
    }

    @Test fun `owner name case-insensitive`() {
        assertTrue(parser.isOwnerName("SUBRAMANYA GOPAL BELLARY", owner))
    }

    @Test fun `abbreviated owner is a subset match`() {
        // "Subramanya G B" — its 3+ char words are a subset of the owner's → treated as owner.
        assertTrue(parser.isOwnerName("Subramanya", owner))
    }

    @Test fun `two shared name words match`() {
        assertTrue(parser.isOwnerName("Gopal Bellary", owner))
    }

    @Test fun `a genuine merchant is NOT the owner`() {
        assertFalse(parser.isOwnerName("Chai Point", owner))
        assertFalse(parser.isOwnerName("Ramesh Kumar", owner))
        assertFalse(parser.isOwnerName("Swiggy", owner))
    }

    @Test fun `one incidental shared word is not enough`() {
        // Shares only "Gopal" — a single common first name shouldn't nuke a real merchant.
        assertFalse(parser.isOwnerName("Gopal Stores", owner))
    }

    @Test fun `blank owner name never matches`() {
        assertFalse(parser.isOwnerName("Anyone", ""))
        assertFalse(parser.isOwnerName("Subramanya Gopal Bellary", "   "))
    }

    // ---- decode: merchant dropped when it's the owner ----

    @Test fun `decode drops merchant when model returns the owner`() {
        val json = """{"amount":500,"direction":"CREDIT","merchant":"Subramanya Gopal Bellary"}"""
        val data = parser.decodeForTest(json, owner)
        assertNull("owner must never survive as merchant", data?.merchant)
        assertEquals(true, data?.isCredit) // direction still mapped
        assertEquals(500.0, data?.amount)
    }

    @Test fun `decode keeps a real merchant`() {
        val json = """{"amount":434,"direction":"DEBIT","merchant":"Chai Point"}"""
        val data = parser.decodeForTest(json, owner)
        assertEquals("Chai Point", data?.merchant)
        assertEquals(false, data?.isCredit)
    }

    @Test fun `decode maps direction both ways`() {
        assertEquals(true, parser.decodeForTest("""{"amount":1,"direction":"CREDIT","merchant":"X"}""", owner)?.isCredit)
        assertEquals(false, parser.decodeForTest("""{"amount":1,"direction":"DEBIT","merchant":"X"}""", owner)?.isCredit)
        assertNull(parser.decodeForTest("""{"amount":1,"merchant":"X"}""", owner)?.isCredit)
    }

    // ---- buildPrompt: name is injected as the owner anchor ----

    @Test fun `prompt names the owner when known`() {
        val p = parser.buildPrompt(owner)
        assertTrue(p.contains(owner))
        assertTrue(p.contains("NEVER the merchant"))
        assertTrue("direction anchored to owner", p.contains("DEBIT if \"$owner\" PAID"))
    }

    @Test fun `prompt falls back to generic direction when name unknown`() {
        val p = parser.buildPrompt("")
        assertFalse(p.contains("account owner (the app user) is"))
        assertTrue(p.contains("DEBIT if money was PAID")) // generic branch
    }
}
