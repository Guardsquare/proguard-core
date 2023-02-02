/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

@file:OptIn(ExperimentalCompilerApi::class)

package proguard.testutils

import com.guardsquare.proguard.assembler.io.JbcReader
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.assertions.fail
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.test.TestCase
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassSubHierarchyInitializer
import proguard.classfile.util.ClassSuperHierarchyInitializer
import proguard.classfile.util.WarningPrinter
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.visitor.ClassPoolFiller
import proguard.io.ClassFilter
import proguard.io.ClassReader
import proguard.io.DataEntryReader
import proguard.io.FileDataEntry
import proguard.io.JarReader
import proguard.io.NameFilteredDataEntryReader
import proguard.io.StreamingDataEntry
import java.io.ByteArrayInputStream
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.util.Objects
import kotlin.reflect.KProperty
import kotlin.streams.toList

data class ClassPools(val programClassPool: ClassPool, val libraryClassPool: ClassPool)

class ClassPoolBuilder private constructor() {

    companion object {
        private val compiler = KotlinCompilation()
        val libraryClassPool by LibraryClassPoolBuilder(compiler)

        fun fromClasses(vararg clazz: Clazz): ClassPool {
            return ClassPool().apply {
                clazz.forEach(::addClass)
            }
        }

        fun fromDirectory(
            dir: File,
            javacArguments: List<String> = emptyList(),
            kotlincArguments: List<String> = emptyList(),
            jdkHome: File = getCurrentJavaHome(),
            initialize: Boolean = true
        ): ClassPools =
            fromSource(
                *Files.walk(dir.toPath())
                    .filter { it.isJavaFile() || it.isKotlinFile() }
                    .map { TestSource.fromFile(it.toFile()) }
                    .toList()
                    .toTypedArray(),
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                jdkHome = jdkHome,
                initialize = initialize
            )

        fun fromFiles(vararg file: File): ClassPools =
            fromSource(*file.map { TestSource.fromFile(it) }.toTypedArray())

        fun fromSource(
            vararg source: TestSource,
            javacArguments: List<String> = emptyList(),
            kotlincArguments: List<String> = emptyList(),
            jdkHome: File = getCurrentJavaHome(),
            initialize: Boolean = true
        ): ClassPools {

            compiler.apply {
                this.sources = source.filterNot { it is AssemblerSource }.map { it.asSourceFile() }
                this.inheritClassPath = false
                this.workingDir = createTempDirectory("ClassPoolBuilder").toFile()
                this.javacArguments = javacArguments.toMutableList()
                this.kotlincArguments = kotlincArguments
                this.verbose = false
                this.jdkHome = jdkHome
            }

            val result = compiler.compile()

            if (result.exitCode != KotlinCompilation.ExitCode.OK) {
                fail("Compilation error: ${result.messages}")
            }

            val programClassPool = ClassPool()

            val classReader: DataEntryReader = NameFilteredDataEntryReader(
                "**.jbc",
                JbcReader(ClassPoolFiller(programClassPool)),
                ClassReader(
                    false,
                    false,
                    false,
                    true,
                    WarningPrinter(PrintWriter(System.err)),
                    ClassPoolFiller(programClassPool)
                )
            )

            result.compiledClassAndResourceFiles.filter { it.isClassFile() }.forEach {
                classReader.read(FileDataEntry(it))
            }

            source.filterIsInstance<AssemblerSource>().forEach {
                classReader.read(StreamingDataEntry(it.filename, it.getInputStream()))
            }

            if (initialize) {
                initialize(programClassPool, libraryClassPool, source.any { it is KotlinSource })
            }

            compiler.workingDir.deleteRecursively()

            return ClassPools(programClassPool, libraryClassPool)
        }

        fun initialize(programClassPool: ClassPool, containsKotlinCode: Boolean) {
            initialize(programClassPool, libraryClassPool, containsKotlinCode)
        }

        fun initialize(programClassPool: ClassPool, libraryClassPool: ClassPool, containsKotlinCode: Boolean) {
            val classReferenceInitializer =
                ClassReferenceInitializer(programClassPool, libraryClassPool)
            val classSuperHierarchyInitializer =
                ClassSuperHierarchyInitializer(programClassPool, libraryClassPool)
            val classSubHierarchyInitializer = ClassSubHierarchyInitializer()

            programClassPool.classesAccept(classSuperHierarchyInitializer)
            libraryClassPool.classesAccept(classSuperHierarchyInitializer)

            if (containsKotlinCode)
                programClassPool.classesAccept(
                    KotlinMetadataInitializer { _, message ->
                        println(
                            message
                        )
                    }
                )

            programClassPool.classesAccept(classReferenceInitializer)
            libraryClassPool.classesAccept(classReferenceInitializer)

            programClassPool.accept(classSubHierarchyInitializer)
            libraryClassPool.accept(classSubHierarchyInitializer)
        }
    }
}

private fun AssemblerSource.getInputStream() =
    ByteArrayInputStream(this.contents.toByteArray(StandardCharsets.UTF_8))

private class LibraryClassPoolBuilder(private val compiler: KotlinCompilation) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ClassPool {
        val key = Objects.hash(compiler.jdkHome, compiler.kotlinStdLibJdkJar, compiler.kotlinStdLibJdkJar)

        if (!libraryClassPools.containsKey(key)) {
            val libraryClassPool = getJavaRuntimeClassPool(compiler.jdkHome)
            compiler.kotlinStdLibJar?.let { libraryClassPool.fromFile(it) }
            compiler.kotlinReflectJar?.let { libraryClassPool.fromFile(it) }
            libraryClassPools[key] = libraryClassPool
            ClassPoolBuilder.initialize(ClassPool(), libraryClassPool, containsKotlinCode = true)
        }

        return libraryClassPools[key]!!
    }

    private fun getJavaRuntimeClassPool(jdkHome: File?): ClassPool {
        val libraryClassPool = ClassPool()

        if (jdkHome != null && jdkHome.exists()) {
            if (jdkHome.resolve("jmods").exists()) {
                jdkHome.resolve("jmods").listFiles().filter {
                    it.name == "java.base.jmod"
                }.forEach {
                    libraryClassPool.fromFile(it)
                }
            } else {
                val runtimeJar = if (jdkHome.resolve("jre").exists()) {
                    jdkHome.resolve("jre").resolve("lib").resolve("rt.jar")
                } else {
                    jdkHome.resolve("lib").resolve("rt.jar")
                }

                libraryClassPool.fromFile(runtimeJar)
            }
        }

        return libraryClassPool
    }

    private fun ClassPool.fromFile(file: File) {
        val classReader: DataEntryReader = ClassReader(
            true,
            false,
            false,
            true,
            true,
            null,
            ClassPoolFiller(this)
        )

        JarReader(ClassFilter(classReader)).read(FileDataEntry(file))
    }

    companion object {

        private val libraryClassPools: MutableMap<Int, ClassPool> = mutableMapOf()

        @AutoScan
        object LibraryClassPoolProcessingInfoCleaningListener : TestListener {
            override suspend fun beforeTest(testCase: TestCase) {
                libraryClassPools.values.forEach {
                    it.classesAccept { clazz ->
                        clazz.processingInfo = null
                        clazz.processingFlags = 0
                    }
                }
            }
        }
    }
}
