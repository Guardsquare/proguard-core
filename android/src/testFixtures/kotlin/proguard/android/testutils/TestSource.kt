import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.classfile.ClassPool
import proguard.classfile.visitor.ClassPoolFiller
import proguard.util.ClassPoolClassLoader
import proguard.util.JarUtil.readJar
import testutils.ClassPoolBuilder
import testutils.ClassPools
import testutils.TestSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

class SmaliSource(val filename: String, @Language("Smali") val contents: String) : TestSource() {
    override fun asSourceFile() = SourceFile.new(filename, contents)
}

fun ClassPoolBuilder.Companion.fromSmali(vararg smaliSources: SmaliSource): ClassPools {
    // smali -> baksmali -> dex file -> dex2pro -> class -> JavaSource
    val classPool = ClassPool()
    val classPoolFiller = ClassPoolFiller(classPool)
    smaliSources.forEach {
        val file = File.createTempFile("tmp", null)
        file.writeText(it.contents)

        val options = SmaliOptions()
        val dexFileName = "classes.dex"
        options.outputDexFile = dexFileName
        Smali.assemble(options, file.absolutePath)

        file.deleteOnExit()

        readJar(dexFileName, false).classesAccept(classPoolFiller)
    }

    initialize(classPool, false)
    return ClassPools(classPool, libraryClassPool)
}

@Throws(RuntimeException::class)
fun runClassPool(programClassPool: ClassPool?, mainClass: String?, args: Array<String?>?): String {
    val clazzLoader = ClassPoolClassLoader(programClassPool)
    return try {
        // set stdout to a new ByteArrayOutputStream to capture anything put on stdout
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos)
        val old = System.out
        System.setOut(ps)
        clazzLoader
            .loadClass(mainClass)
            .getDeclaredMethod("main", Array<String>::class.java)
            .invoke(null, args as Any?)

        // revert stdout back to its original state
        System.setOut(old)
        baos.toString()
    } catch (e: IllegalAccessException) {
        throw RuntimeException(e)
    } catch (e: InvocationTargetException) {
        throw RuntimeException(e)
    } catch (e: NoSuchMethodException) {
        throw RuntimeException(e)
    } catch (e: ClassNotFoundException) {
        throw RuntimeException(e)
    }
//    } catch (e: Exception){
//        "failure"
//    }
}
