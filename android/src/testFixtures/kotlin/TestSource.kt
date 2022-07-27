import com.tschuchort.compiletesting.SourceFile
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.classfile.visitor.ClassPrinter
import proguard.util.JarUtil
import testutils.*
import java.io.File

class SmaliSource(val filename: String, val contents: String) : TestSource() {
    override fun asSourceFile() = SourceFile.new(filename, contents)
}

fun ClassPoolBuilder.Companion.fromSmali(smali: SmaliSource) : ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource

    val file = File(smali.filename)
    file.createNewFile()
    file.writeText(smali.contents)

    val options = SmaliOptions()
    options.outputDexFile = "classes.dex"
    Smali.assemble(options, smali.filename)

    file.delete()

    val jarFileName = "temp.jar"
    val classPool = JarUtil.readJar("classes.dex", false)
    classPool.classesAccept(ClassPrinter())
    JarUtil.writeJar(classPool, jarFileName)
    val source = TestSource.fromFile(File(jarFileName))

    return fromSource(source)

//    val dexClassReader = DexClassReader( )

/*    return ClassPoolBuilder.fromSource(

    )*/
}
