/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 */

package proguard.io

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.io.*
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class ZipWriterTest : FreeSpec({

    "Given a zip input file" - {
        val time = 1700853925000
        //val time = Instant.now().toEpochMilli()

        // Truncate to seconds at most, as the dos time has a maximum granularity of seconds.
        val modificationTime = Instant.ofEpochMilli(time).truncatedTo(ChronoUnit.SECONDS)
        val zipArchiveAsByteArray = createZipArchive(modificationTime)

        val dataEntry =
            StreamingDataEntry("zipArchive",
            ByteArrayInputStream(zipArchiveAsByteArray.toByteArray()))

        "When using a pass-through ZipWriter" - {
            val os = ByteArrayOutputStream()

            val writer = ZipWriter(
                FixedOutputStreamWriter(os))

            val dataEntryReader =
                JarReader(
                DataEntryCopier(
                writer))

            dataEntryReader.read(dataEntry)

            writer.close()

            "Then the resulting modification time should be the same as the original one" {
                getModificationTime(ByteArrayInputStream(os.toByteArray()))?.toEpochMilli() shouldBe modificationTime.toEpochMilli()
            }
        }
    }
}) {
    companion object {
        fun createZipArchive(modificationTime: Instant): ByteArrayOutputStream {
            val baos = ByteArrayOutputStream()
            baos.use {
                val zipOutputStream = ZipOutputStream(baos)
                val entry = ZipEntry("test.txt")
                entry.setLastModifiedTime(FileTime.fromMillis(modificationTime.toEpochMilli()))
                zipOutputStream.putNextEntry(entry)
                zipOutputStream.closeEntry()
            }
            return baos
        }

        fun getModificationTime(inputStream: InputStream): Instant? {
            var modificationTime: Instant? = null

            val dataEntry = StreamingDataEntry("zipArchive", inputStream)

            val dataEntryReader =
                JarReader {
                    modificationTime = Instant.ofEpochMilli(it.modificationTime)
                }

            dataEntryReader.read(dataEntry)
            return modificationTime
        }
    }
}

class FixedOutputStreamWriter (private val os: OutputStream) : DataEntryWriter {
    override fun createDirectory(dataEntry: DataEntry): Boolean {
        return true
    }

    override fun sameOutputStream(dataEntry1: DataEntry, dataEntry2: DataEntry): Boolean {
        return true
    }

    override fun createOutputStream(dataEntry: DataEntry): OutputStream {
        return os
    }

    override fun close() {
        os.close()
    }

    override fun println(pw: PrintWriter, prefix: String) {
        pw.println(prefix + "FixedOutputStreamWriter ()")
    }
}
