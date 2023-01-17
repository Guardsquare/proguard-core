package proguard.util

import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.visitor.ClassPrinter
import proguard.classfile.visitor.MethodCollector
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Some methods to help during debugging.
 *
 * @author Samuel Hopstock
 */
object DebugUtil {

    /**
     * Get the bytecode of a particular [Clazz].
     */
    @JvmStatic
    fun asString(clazz: Clazz): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        clazz.accept(ClassPrinter(pw))
        return sw.toString()
    }

    /**
     * Get the bytecode of a particular [Method].
     */
    @JvmStatic
    @JvmOverloads
    fun asString(clazz: Clazz, method: Method, verbose: Boolean = false): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        if (verbose) {
            method.accept(clazz, ClassPrinter(pw))
        } else {
            method.accept(clazz, AllAttributeVisitor(AttributeNameFilter(Attribute.CODE, AllInstructionVisitor(ClassPrinter(pw)))))
        }
        return sw.toString()
    }

    /**
     * Get the bytecode of a particular [Method] from your [ClassPool].
     */
    @JvmStatic
    @JvmOverloads
    fun asString(classPool: ClassPool, signature: MethodSignature, verbose: Boolean = false): String {
        val clazz = classPool.getClass(signature.className)
        check(clazz is Clazz) { "Class " + clazz.name + " not found in class pool" }
        val method = clazz.findMethod(signature.method, signature.descriptor.toString())
        check(method is Method) { "Method " + signature + " not found in class " + clazz.name }
        return asString(clazz, method, verbose)
    }

    /**
     * Get the bytecode of all methods in a [Class] from your [ClassPool].
     */
    @JvmStatic
    fun asString(classPool: ClassPool, className: String): String {
        val clazz = classPool.getClass(className)
        check(clazz is Clazz) { "Class $className not found in class pool" }
        return asString(clazz)
    }

    /**
     * Get the bytecode of a particular [ProgramMethod] in a situation
     * where you only have access to a [JvmCfa] and the corresponding [MethodSignature].
     */
    @JvmStatic
    @JvmOverloads
    fun asString(cfa: JvmCfa, signature: MethodSignature, verbose: Boolean = false): String {
        val methodNode = cfa.getFunctionEntryNode(signature) ?: throw IllegalStateException("Method $signature not found in CFA")
        val clazz = methodNode.clazz
        check(clazz is ProgramClass) { "Class " + clazz.name + " is not a program class" }
        val method = clazz.findMethod(signature.method, signature.descriptor.toString())
        check(method is ProgramMethod) { "Method " + signature + " not found in class " + clazz.getName() }
        return asString(clazz, method, verbose)
    }

    /**
     * Get a copy-pastable String of comma-separated FQNs for all methods in a class.
     *
     *
     * Example output format:
     * <pre>
     * `
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;[B)Lokhttp3/RequestBody;",
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;[BI)Lokhttp3/RequestBody;",
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;[BII)Lokhttp3/RequestBody;",
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;Lokio/ByteString;)Lokhttp3/RequestBody;",
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;Ljava/io/File;)Lokhttp3/RequestBody;",
     * "Lokhttp3/RequestBody;create(Lokhttp3/MediaType;Ljava/lang/String;)Lokhttp3/RequestBody;"
     ` *
     </pre> *
     */
    @JvmStatic
    fun getMethodFqnList(classPool: ClassPool, className: String?): String {
        val clazz = classPool.getClass(className) ?: throw IllegalStateException("Class not found")
        val methods = mutableSetOf<Method>()
        val collector = MethodCollector(methods)
        clazz.methodsAccept(collector)
        return methods.joinToString(",\n") { "\"" + MethodSignature(clazz, it).fqn + "\"" }
    }
}
