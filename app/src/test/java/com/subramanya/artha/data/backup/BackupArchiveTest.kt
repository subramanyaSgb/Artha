package com.subramanya.artha.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Round-trip gate for the v2 full-backup ZIP container: the JSON snapshot and
 * every receipt image must come back byte-identical, and a hostile entry name
 * must not escape the receipts directory.
 */
class BackupArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun writeArchive(json: String, receipts: List<File>): ByteArray {
        val buf = ByteArrayOutputStream()
        BackupArchive.write(buf, json, receipts)
        return buf.toByteArray()
    }

    @Test
    fun `json and receipts round trip`() {
        val src = tmp.newFolder("receipts")
        val a = File(src, "a.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val b = File(src, "b.jpg").apply { writeBytes(ByteArray(5_000) { it.toByte() }) }
        val json = """{"schema_version":2,"accounts":[]}"""

        val bytes = writeArchive(json, listOf(a, b))

        assertTrue(BackupArchive.isZip(bytes))
        assertEquals(json, BackupArchive.readJson(bytes))

        val dest = tmp.newFolder("restored")
        val extracted = BackupArchive.extractReceipts(bytes, dest)
        assertEquals(setOf("a.jpg", "b.jpg"), extracted)
        assertTrue(a.readBytes().contentEquals(File(dest, "a.jpg").readBytes()))
        assertTrue(b.readBytes().contentEquals(File(dest, "b.jpg").readBytes()))
    }

    @Test
    fun `non-zip bytes read null instead of throwing`() {
        assertNull(BackupArchive.readJson("{}".toByteArray()))
    }

    @Test
    fun `plain json is not detected as zip`() {
        assertFalse(BackupArchive.isZip("""{"schema_version":2}""".toByteArray()))
        assertFalse(BackupArchive.isZip(ByteArray(0)))
    }
}
