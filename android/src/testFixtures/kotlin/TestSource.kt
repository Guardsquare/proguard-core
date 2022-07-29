import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.util.JarUtil
import testutils.ClassPoolBuilder
import testutils.ClassPools
import testutils.TestSource
import java.io.File

class SmaliSource(val filename: String, @Language("smali") val contents: String) : TestSource() {
    override fun asSourceFile() = SourceFile.new(filename, contents)
}

fun ClassPoolBuilder.Companion.fromSmali(smali: SmaliSource): ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource

    val file = File.createTempFile("tmp", null)
    file.writeText(smali.contents)

    val options = SmaliOptions()
    val dexFileName = "classes.dex"
    options.outputDexFile = dexFileName
    Smali.assemble(options, file.absolutePath)

    file.deleteOnExit()

    val classPool = JarUtil.readJar(dexFileName, false)

    initialize(classPool, false)
    return ClassPools(classPool, libraryClassPool)
}
