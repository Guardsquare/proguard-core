package proguard.testutils.classfile.runner

import proguard.classfile.ClassMemberPair
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Field
import proguard.classfile.Method
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.io.ProgramClassWriter
import proguard.classfile.util.ClassUtil
import proguard.classfile.util.ClassUtil.internalClassName
import proguard.classfile.util.InternalTypeEnumeration
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.ProgramClassFilter
import proguard.preverify.CodePreverifier
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.lang.reflect.Member

/**
 * A ClassLoader that can load classes from a ProGuardCORE ClassPool.
 */
class ClassPoolClassLoader(private val classPool: ClassPool) : ClassLoader() {
    override fun findClass(name: String): Class<*> {
        val clazz = classPool.getClass(internalClassName(name))
        if (clazz != null) {
            // Add stack map frames with the CodePreverifier, as the bytecode needs to run on the JVM.
            clazz.accept(AllMethodVisitor(AllAttributeVisitor(CodePreverifier(false))))
            // Write out the class to a ByteArrayOutputStream.
            val byteArrayOutputStream = ByteArrayOutputStream()
            clazz.accept(ProgramClassFilter(ProgramClassWriter(DataOutputStream(byteArrayOutputStream))))
            val bytes = byteArrayOutputStream.toByteArray()
            return defineClass(name, bytes, 0, bytes.size)
        }
        return super.findClass(name)
    }

    operator fun get(clazz: Clazz): Class<*> {
        check(clazz in classPool) { "$clazz is not in the class pool" }
        return loadClass(ClassUtil.externalClassName(clazz.name))
    }

    fun resolve(memberPair: ClassMemberPair): Member {
        val cls = get(memberPair.clazz)
        return when (memberPair.member) {
            is Field -> cls.getDeclaredField(memberPair.name)
            is Method -> cls.getDeclaredMethod(
                memberPair.name,
                *InternalTypeEnumeration(memberPair.descriptor)
                    .asSequence()
                    .map {
                        when (it) {
                            "Z" -> Boolean::class.java
                            "B" -> Byte::class.java
                            "C" -> Char::class.java
                            "S" -> Short::class.java
                            "I" -> Int::class.java
                            "J" -> Long::class.java
                            "F" -> Float::class.java
                            "D" -> Double::class.java
                            "V" -> Void.TYPE
                            else -> Class.forName(ClassUtil.externalClassForNameType(it), false, this)
                        }
                    }
                    .toList()
                    .toTypedArray(),
            )

            else -> throw IllegalStateException("Invalid object : ${memberPair.member}")
        }
    }
}
