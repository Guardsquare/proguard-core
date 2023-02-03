package proguard.android.testutils

import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.io.DataEntry
import proguard.io.DataEntryReader
import proguard.io.DexClassReader
import proguard.io.FileDataEntry
import proguard.io.NameFilteredDataEntryReader
import proguard.io.util.IOUtil
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.ClassPools
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun ClassPoolBuilder.Companion.fromSmali(smali: SmaliSource): ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource
    val file = File.createTempFile("tmp", ".smali")
    file.writeText(smali.contents)

    val classPool = IOUtil.read(file, false, true) { dataEntryReader, classPoolFiller ->
        // Convert dex files
        val dexReader = NameFilteredDataEntryReader(
            "classes*.dex",
            DexClassReader(
                true,
                classPoolFiller
            ),
            dataEntryReader
        )

        NameFilteredDataEntryReader(
            "**.smali",
            Smali2DexReader(dexReader),
            dexReader
        )
    }
    file.deleteOnExit()
    initialize(classPool, false)
    return ClassPools(classPool, libraryClassPool)
}

class Smali2DexReader(private val delegate: DataEntryReader) : DataEntryReader {
    override fun read(dataEntry: DataEntry) {
        val options = SmaliOptions()
        val dexFile = File.createTempFile("classes", ".dex")
        options.outputDexFile = dexFile.absolutePath
        val tempFile = File.createTempFile("smali", ".smali")
        copyInputStreamToFile(dataEntry.inputStream, tempFile)
        Smali.assemble(options, tempFile.absolutePath)
        val fileDataEntry = FileDataEntry(dexFile)
        delegate.read(fileDataEntry)
        dexFile.deleteOnExit()
        dataEntry.closeInputStream()
    }

    private fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file, false).use { outputStream ->
            var read: Int
            val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
            while (inputStream.read(bytes).also { read = it } != -1) {
                outputStream.write(bytes, 0, read)
            }
        }
    }
}
