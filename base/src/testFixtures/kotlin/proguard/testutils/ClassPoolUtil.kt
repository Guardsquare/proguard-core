package proguard.testutils

import proguard.classfile.ClassPool
import proguard.classfile.util.ClassPoolClassLoader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

/**
 * Runs the given class from the given classpool, captures the stdout returns the stdout result as a string.
 * @param programClassPool
 * @param mainClass
 * @param args
 * @return
 * @throws RuntimeException
 */
@Throws(RuntimeException::class)
fun runClassPool(programClassPool: ClassPool, mainClass: String, args: Array<String> = emptyArray()): String {
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
}
