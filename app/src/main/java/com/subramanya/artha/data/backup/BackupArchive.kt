package com.subramanya.artha.data.backup

import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The schema-v2 "full backup" container: a plain ZIP holding
 *
 *     backup.json        — the BackupCodec JSON snapshot (incl. settings)
 *     receipts/<name>    — every receipt image from filesDir/receipts
 *
 * Pure JVM (java.util.zip) so the round-trip is unit-testable. Restore is
 * two-pass on purpose: [readJson] first so the payload can be fully decoded and
 * validated BEFORE anything (DB or receipt files) is touched, then
 * [extractReceipts] writes the images.
 */
object BackupArchive {

    const val BACKUP_ENTRY = "backup.json"
    const val RECEIPTS_PREFIX = "receipts/"

    /** ZIP local-file-header magic: "PK". Distinguishes v2 archives from JSON/encrypted text. */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    /** Streams a backup archive to [out]: the JSON snapshot plus [receiptFiles]. */
    fun write(out: OutputStream, json: String, receiptFiles: List<File>) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            receiptFiles.filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry(RECEIPTS_PREFIX + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** First pass: the backup.json text, or null when the entry is missing. */
    fun readJson(bytes: ByteArray): String? {
        eachEntry(bytes) { entry, zip ->
            if (entry.name == BACKUP_ENTRY) {
                return zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return null
    }

    /**
     * Second pass: writes every `receipts/<name>` entry into [intoDir] and returns
     * the extracted file names. Entry names are sanitised to their last path
     * segment so a crafted archive can't escape the receipts directory.
     */
    fun extractReceipts(bytes: ByteArray, intoDir: File): Set<String> {
        intoDir.mkdirs()
        val extracted = mutableSetOf<String>()
        eachEntry(bytes) { entry, zip ->
            if (!entry.isDirectory && entry.name.startsWith(RECEIPTS_PREFIX)) {
                val name = entry.name.substringAfterLast('/')
                if (name.isNotBlank()) {
                    File(intoDir, name).outputStream().use { zip.copyTo(it) }
                    extracted.add(name)
                }
            }
        }
        return extracted
    }

    private inline fun eachEntry(bytes: ByteArray, block: (ZipEntry, ZipInputStream) -> Unit) {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                block(entry, zip)
                entry = zip.nextEntry
            }
        }
    }
}
