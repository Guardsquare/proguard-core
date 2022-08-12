package com.guardsquare.proguard.tools

import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.required
import proguard.classfile.AccessConstants.PUBLIC
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
import proguard.classfile.visitor.ClassPrinter
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MultiClassVisitor
import proguard.io.util.IOUtil.writeJar
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

@OptIn(ExperimentalCli::class)
class TransformCmd : Subcommand("transform", "Apply transformations to input (experimental)") {
    var input by argument(ArgType.String, description = "Input file name")
    var output by option(ArgType.String, description = "Output file name", shortName = "o", fullName = "output")
    var transformer by option(ArgType.String, description = "Classes containing ClassVisitor implementations", shortName = "t", fullName = "transformer").required()
    var printClasses by option(ArgType.Boolean, description = "Print classes after applying ClassVisitors").default(false)
    var classNameFilter by option(ArgType.String, description = "Class name filter", shortName = "cf", fullName = "classNameFilter").default("**")

    private val programClassPool: ClassPool by lazy { read(input, "**", false) }
    private val transformersPool: ClassPool by lazy { read(transformer, "**", false) }

    override fun execute() {
        val transformersLibraryClassPool = ClassPool()
        // Add ClassVisitor to the library class pool, so that Clazz.extendsOrImplements can be used.
        transformersLibraryClassPool.addLibraryClass(ClassVisitor::class.java)

        initialize(transformersPool, transformersLibraryClassPool)
        val libraryClassPool = ClassPool()
        initialize(programClassPool, libraryClassPool) // TODO: libraryClassPool

        val classLoader = ClassPoolClassLoader(transformersPool)

        // Load all the ClassVisitor classes and create instances
        val classVisitors = transformersPool
            .classes()
            .asSequence()
            .filter { it.extendsOrImplements(internalClassName(ClassVisitor::class.java.name)) }
            .filter { it.accessFlags and PUBLIC == PUBLIC }
            .map { classLoader.loadClass(externalClassName(it.name)) as Class<ClassVisitor> }
            .map { it.getDeclaredConstructor().newInstance() }
            .onEach {
                // Set the class pools if the fields exist
                with(it::class.java) {
                    if (this.fields.count { it.name == "programClassPool" } > 0 &&
                        this.getField("programClassPool").type == ClassPool::class.java
                    ) {
                        this.getField("programClassPool").set(it, programClassPool)
                    }
                    if (this.fields.count { it.name == "libraryClassPool" } > 0 &&
                        this.getField("libraryClassPool").type == ClassPool::class.java
                    ) {
                        this.getField("libraryClassPool").set(it, libraryClassPool)
                    }
                }
            }
            .map { classVisitor ->
                MultiClassVisitor(
                    // { clazz -> println("Applying ${classVisitor.javaClass.simpleName} to ${externalClassName(clazz.name)}") },
                    classVisitor
                )
            }
            .toList()
            .toTypedArray()

        programClassPool.classesAccept(classNameFilter, MultiClassVisitor(*classVisitors))

        if (printClasses) programClassPool.classesAccept(ClassPrinter())

        output?.let { writeJar(programClassPool, it) }
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
