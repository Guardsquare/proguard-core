package com.guardsquare.proguard.tools

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import proguard.classfile.ClassPool
import proguard.classfile.LibraryClass
import proguard.classfile.ProgramClass
import proguard.classfile.io.LibraryClassReader
import proguard.classfile.io.ProgramClassReader
import proguard.classfile.util.ClassPoolClassLoader
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassSuperHierarchyInitializer
import proguard.classfile.util.ClassUtil.externalClassName
import proguard.classfile.util.ClassUtil.internalClassName
import proguard.classfile.util.WarningPrinter
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MultiClassVisitor
import proguard.io.util.IOUtil.writeJar
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

@OptIn(ExperimentalCli::class)
class TransformCmd : Subcommand("transform", "Apply transformations to input") {
    var input by argument(ArgType.String, description = "Input file name")
    var output by option(ArgType.String, description = "Output file name", shortName = "o", fullName = "output")
    var transformer by option(ArgType.String, description = "classes containing ClassVisitor implementations", shortName = "t", fullName = "transformer").required()

    private val programClassPool: ClassPool by lazy { read(input, "**", false) }
    private val transformersPool: ClassPool by lazy { read(transformer, "**", false) }

    override fun execute() {
        val transformersLibraryClassPool = ClassPool()
        // Add ClassVisitor to the library class pool, so that Clazz.extendsOrImplements can be used.
        transformersLibraryClassPool.addLibraryClass(ClassVisitor::class.java)

        initialize(transformersPool, transformersLibraryClassPool)
        initialize(programClassPool, ClassPool()) // TODO: libraryClassPool

        val classLoader = ClassPoolClassLoader(transformersPool)

        // Load all the ClassVisitor classes and create instances
        val classVisitors = transformersPool
            .classes()
            .filter { it.extendsOrImplements(internalClassName(ClassVisitor::class.java.name)) }
            .map { classLoader.loadClass(externalClassName(it.name)) as Class<ClassVisitor> }
            .map { it.getDeclaredConstructor().newInstance() }
            .map { MultiClassVisitor({ clazz -> println("Transforming ${clazz.name}") }, it) }
            .toTypedArray()

        programClassPool.classesAccept(MultiClassVisitor(*classVisitors))

        writeJar(programClassPool, output ?: input)
    }

    private fun initialize(programClassPool: ClassPool, libraryClassPool: ClassPool) {
        // TODO: put this util in ProGuardCORE
        // TODO: hide warnings for now
        val printWriter = PrintWriter(object : OutputStream() {
            override fun write(b: Int) {
            }
        })
        val warningPrinter = WarningPrinter(printWriter)

        // Initialize the class hierarchies.
        libraryClassPool.classesAccept(
            ClassSuperHierarchyInitializer(
                programClassPool,
                libraryClassPool,
                null,
                null
            )
        )
        programClassPool.classesAccept(
            ClassSuperHierarchyInitializer(
                programClassPool,
                libraryClassPool,
                warningPrinter,
                warningPrinter
            )
        )

        // Initialize the other references from the program classes.
        programClassPool.classesAccept(
            ClassReferenceInitializer(
                programClassPool,
                libraryClassPool,
                warningPrinter,
                warningPrinter,
                warningPrinter,
                null
            )
        )

        // Flush the warnings.
        printWriter.flush()
    }

    private fun ClassPool.addClass(vararg classes: Class<*>) {
        // TODO: put this util in ProGuardCORE
        for (clazz in classes) {
            val `is` = this::class.java.classLoader.getResourceAsStream("${internalClassName(clazz.name)}.class") as InputStream
            val classReader = ProgramClassReader(DataInputStream(`is`))
            val programClass = ProgramClass()
            programClass.accept(classReader)
            addClass(programClass)
        }
    }
    private fun ClassPool.addLibraryClass(vararg classes: Class<*>) {
        // TODO: put this util in ProGuardCORE
        for (clazz in classes) {
            val `is` = this::class.java.classLoader.getResourceAsStream("${internalClassName(clazz.name)}.class") as InputStream
            val classReader = LibraryClassReader(DataInputStream(`is`), false, false)
            val libraryClass = LibraryClass()
            libraryClass.accept(classReader)
            addClass(libraryClass)
        }
    }
}
