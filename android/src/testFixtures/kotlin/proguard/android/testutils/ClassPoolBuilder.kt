package proguard.android.testutils

import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.io.*
import proguard.io.util.IOUtil
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.ClassPools
import java.io.File

fun ClassPoolBuilder.Companion.fromSmali(smali: SmaliSource): ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource
    val file = File.createTempFile("tmp", ".smali")
    file.writeText(smali.contents)

    val classPool = IOUtil.read(file.absolutePath, "**", false) { dataEntryReader, classPoolFiller ->
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
        Smali.assemble(options,  (dataEntry as FileDataEntry).file.absolutePath)
        val fileDataEntry = FileDataEntry(dexFile)
        delegate.read(fileDataEntry)
        dexFile.deleteOnExit()
    }
}
