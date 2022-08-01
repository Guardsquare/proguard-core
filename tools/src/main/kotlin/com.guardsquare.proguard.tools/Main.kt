package com.guardsquare.proguard.tools

import kotlinx.cli.*
import proguard.classfile.*
import proguard.classfile.util.ClassUtil.*
import proguard.classfile.visitor.*
import proguard.dexfile.reader.DexClassReader
import proguard.io.*
import proguard.util.ExtensionMatcher
import proguard.util.OrMatcher
import java.io.Closeable
import java.io.File


@ExperimentalCli
fun main(args: Array<String>) {

    val parser = ArgParser("proguard-core-tools")

    class ListCmd : Subcommand("list", "List classes, methods & methods") {

        var input by argument(ArgType.String, description = "Input file name")
        var classNameFilter by option(
            ArgType.String,
            description = "Class name filter",
            shortName = "cf",
            fullName = "classNameFilter"
        ).default("**")
        val programClassPool: ClassPool by lazy { readJar(input, classNameFilter, false) }

        init {
            subcommands(ClassNamePrinterCmd(), MethodNamePrinterCmd(), FieldNamePrinterCmd(), MemberPrinterCmd())
        }

        override fun execute() {}

        inner class ClassNamePrinterCmd : Subcommand("classes", "List all the classes") {
            override fun execute() {
                programClassPool.classesAccept(ClassNamePrinter())
            }

            inner class ClassNamePrinter : ClassVisitor {
                override fun visitAnyClass(clazz: Clazz) {}
                override fun visitProgramClass(programClass: ProgramClass) {
                    println(externalClassName(programClass.name))
                }
            }
        }

        inner class MethodNamePrinterCmd : Subcommand("methods", "List all the methods") {
            override fun execute() {
                programClassPool.classesAccept(AllMethodVisitor(MemberPrinter()))
            }
        }

        inner class FieldNamePrinterCmd : Subcommand("fields", "List all the fields") {
            override fun execute() {
                programClassPool.classesAccept(AllFieldVisitor(MemberPrinter()))
            }
        }

        inner class MemberPrinterCmd : Subcommand("members", "List all the members") {
            override fun execute() {
                programClassPool.classesAccept(AllMemberVisitor(MemberPrinter()))
            }
        }

        inner class MemberPrinter : MemberVisitor {

            override fun visitAnyMember(clazz: Clazz, member: Member) { }

            override fun visitProgramField(programClass: ProgramClass, programField: ProgramField) {
                println(
                    externalClassName(programClass.name) + " " +
                            externalFullFieldDescription(
                                programField.accessFlags,
                                programField.getName(programClass),
                                programField.getDescriptor(programClass)
                            )
                )
            }

            override fun visitProgramMethod(programClass: ProgramClass, programMethod: ProgramMethod) {
                println(
                    externalFullMethodDescription(
                        programClass.name,
                        programMethod.accessFlags,
                        programMethod.getName(programClass),
                        programMethod.getDescriptor(programClass)
                    )
                )
            }
        }
    }

    class PrintCmd : Subcommand("print", "Print classes details") {

        var input by argument(ArgType.String, description = "Input file name")
        var classNameFilter by option(
            ArgType.String,
            description = "Class name filter",
            shortName = "cf",
            fullName = "classNameFilter"
        ).default("**")
        val programClassPool: ClassPool by lazy { readJar(input, classNameFilter, false) }

        init {
            subcommands(ClassPrinterCmd())
        }

        override fun execute() { }

        inner class ClassPrinterCmd : Subcommand("classes", "Print all the classes") {
            override fun execute() {
                programClassPool.classesAccept(ClassPrinter())
            }
        }
    }

    class Dex2JarCmd : Subcommand("dex2jar", "Convert dex to jar") {

        var input by argument(ArgType.String, description = "Input file name")
        var output by option(
            ArgType.String,
            description = "Output file name",
            shortName = "o",
            fullName = "output"
        ).default("classes.jar")
        var classNameFilter by option(
            ArgType.String,
            description = "Class name filter",
            shortName = "cf",
            fullName = "classNameFilter"
        ).default("**")
        var forceOverwrite by option(
            ArgType.Boolean,
            description = "Force file overwriting",
            shortName = "f",
            fullName = "force"
        ).default(false)

        override fun execute() {
            val programClassPool = readJar(input, classNameFilter, false)
            val file = File(output)
            if (file.exists() && !forceOverwrite) {
                System.err.println("$file exists, use --force to overwrite")
                return
            }
            writeJar(programClassPool, file)
        }

        private fun writeJar(programClassPool: ClassPool, file: File) {
            class MyJarWriter(zipEntryWriter: DataEntryWriter) : JarWriter(zipEntryWriter), Closeable {
                override fun close() {
                    super.close()
                }
            }

            val jarWriter = MyJarWriter(ZipWriter(FixedFileWriter(file)))
            jarWriter.use { programClassPool.classesAccept(DataEntryClassWriter(it)) }
        }
    }

    class Jar2DexCmd : Subcommand("jar2dex", "Convert jar to dex - NOT YET IMPLEMENTED") {

        var input by argument(ArgType.String, description = "Input file name")
        var output by option(ArgType.String, description = "Output file name", shortName = "o", fullName = "output")
        override fun execute() {
            TODO("Not yet implemented")
        }
    }

    parser.subcommands(Dex2JarCmd(), Jar2DexCmd(), ListCmd(), PrintCmd())
    parser.parse(args)
}

fun readJar(
    jarFileName: String,
    classNameFilter: String,
    isLibrary: Boolean
): ClassPool {
    val classPool = ClassPool()
    val source: DataEntrySource = FileSource(File(jarFileName))
    val acceptedClassVisitor = ClassPoolFiller(classPool)

    var classReader: DataEntryReader = NameFilteredDataEntryReader(
        "**.class",
        ClassReader(
            isLibrary, false, false, false, null,
            ClassNameFilter(classNameFilter, acceptedClassVisitor)
        )
    )

    classReader = NameFilteredDataEntryReader(
        "classes*.dex",
        DexClassReader(!isLibrary, acceptedClassVisitor), classReader
    )

    classReader = FilteredDataEntryReader(
        DataEntryNameFilter(ExtensionMatcher("aar")),
        JarReader(
            NameFilteredDataEntryReader(
                "classes.jar",
                JarReader(classReader)
            )
        ),
        FilteredDataEntryReader(
            DataEntryNameFilter(
                OrMatcher(
                    ExtensionMatcher("jar"),
                    ExtensionMatcher("zip"),
                    ExtensionMatcher("apk")
                )
            ),
            JarReader(classReader),
            classReader
        )
    )
    source.pumpDataEntries(classReader)
    return classPool
}