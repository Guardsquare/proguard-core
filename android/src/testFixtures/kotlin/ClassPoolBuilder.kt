import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.dexfile.reader.DexClassReader
import proguard.io.NameFilteredDataEntryReader
import proguard.io.util.IOUtil
import testutils.ClassPoolBuilder
import testutils.ClassPools
import java.io.File

fun ClassPoolBuilder.Companion.fromSmali(smali: SmaliSource): ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource
    val file = File.createTempFile("tmp", null)
    file.writeText(smali.contents)

    val options = SmaliOptions()
    val dexFile = File.createTempFile("classes", ".dex")
    val dexFileName = dexFile.absolutePath
    options.outputDexFile = dexFileName
    Smali.assemble(options, file.absolutePath)

    file.deleteOnExit()
    dexFile.deleteOnExit()

    val classPool = IOUtil.read(dexFileName, "**", false) { dataEntryReader, classPoolFiller ->
        // Convert dex files
        NameFilteredDataEntryReader(
            "classes*.dex",
            DexClassReader(
                true,
                classPoolFiller
            ),
            dataEntryReader
        )
    }
    initialize(classPool, false)
    return ClassPools(classPool, libraryClassPool)
}
