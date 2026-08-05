package com.subramanya.artha.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDocStoreTest {

    @Test
    fun `pdf magic bytes are detected`() {
        assertTrue(PolicyDocStore.isPdfBytes("%PDF-1.7\nrest".toByteArray()))
    }

    @Test
    fun `non-pdf bytes are rejected`() {
        // JPEG SOI marker + junk — not a PDF.
        assertFalse(PolicyDocStore.isPdfBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x01)))
        assertFalse(PolicyDocStore.isPdfBytes("PDF".toByteArray())) // too short / no leading %
        assertFalse(PolicyDocStore.isPdfBytes(ByteArray(0)))
    }
}
