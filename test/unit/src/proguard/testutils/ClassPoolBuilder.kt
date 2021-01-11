/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.testutils

import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintWriter
import java.io.StringReader
import org.codehaus.janino.SimpleCompiler
import org.intellij.lang.annotations.Language
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassSubHierarchyInitializer
import proguard.classfile.util.ClassSuperHierarchyInitializer
import proguard.classfile.util.WarningPrinter
import proguard.classfile.visitor.ClassPoolFiller
import proguard.io.ClassFilter
import proguard.io.ClassReader
import proguard.io.DataEntryReader
import proguard.io.FileDataEntry
import proguard.io.JarReader
import proguard.io.StreamingDataEntry

class ClassPoolBuilder {
    companion object {
        fun fromClasses(vararg clazz: Clazz): ClassPool {
            return ClassPool().apply {
                clazz.forEach(::addClass)
            }
        }

        fun fromSource(@Language("Java") source: String): ClassPool {
            val simpleCompiler = SimpleCompiler("ClassPoolBuilder", StringReader(source))

            val libraryClassPool = javaRuntime
            val result = ClassPool()

            val classReader: DataEntryReader = ClassReader(false,
                    false,
                    false,
                    true,
                    WarningPrinter(PrintWriter(System.err)),
                    ClassPoolFiller(result))

            // Pipe each class file from the compiler into DexGuard
            simpleCompiler.classFiles.forEach {
                val pipedInputStream = PipedInputStream()
                val pipedOutputStream = PipedOutputStream(pipedInputStream)
                it.store(pipedOutputStream)
                classReader.read(StreamingDataEntry("ClassPoolBuilder.class", pipedInputStream))
            }

            result.classesAccept(ClassReferenceInitializer(result, libraryClassPool))
            result.classesAccept(ClassSuperHierarchyInitializer(result, libraryClassPool))
            result.accept(ClassSubHierarchyInitializer())

            return result
        }

        val javaRuntime: ClassPool = run {
            val runtimeJar = File(System.getProperty("java.home")).resolve("lib").resolve("rt.jar")

            val result = ClassPool()

            val classReader: DataEntryReader = ClassReader(true,
                    false,
                    false,
                    true,
                    WarningPrinter(PrintWriter(System.err)),
                    ClassPoolFiller(result))

            JarReader(ClassFilter(classReader)).read(FileDataEntry(runtimeJar.parentFile, runtimeJar))

            return@run result
        }
    }
}
