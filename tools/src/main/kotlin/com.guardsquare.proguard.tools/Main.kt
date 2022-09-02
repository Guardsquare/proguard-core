package com.guardsquare.proguard.tools

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.required
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.ProgramMethod
import proguard.classfile.util.ClassUtil.externalClassName
import proguard.classfile.util.ClassUtil.externalFullFieldDescription
import proguard.classfile.util.ClassUtil.externalFullMethodDescription
import proguard.classfile.visitor.AllFieldVisitor
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.ClassPrinter
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MemberVisitor
import proguard.io.ClassPath
import proguard.io.ClassPathEntry
import proguard.io.DataEntry
import proguard.io.DataEntryReader
import proguard.io.DexClassReader
import proguard.io.FileDataEntry
import proguard.io.NameFilteredDataEntryReader
import proguard.io.util.IOUtil
import proguard.io.util.IOUtil.writeJar
import java.io.File

@ExperimentalCli
fun main(args: Array<String>) {

    val parser = ArgParser("proguard-core-tools")

    class ListCmd : Subcommand("list", "List classes, methods & fields") {

        var input by argument(ArgType.String, description = "Input file name")
        var classNameFilter by option(
            ArgType.String,
            description = "Class name filter",
            shortName = "cf",
            fullName = "classNameFilter"
        ).default("**")
        val programClassPool: ClassPool by lazy { read(input, classNameFilter, false) }

        init {
            subcommands(ClassNamePrinterCmd(), MethodNamePrinterCmd(), FieldNamePrinterCmd(), MemberPrinterCmd(), StringPrinterCmd())
        }

        override fun execute() { }

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

        inner class StringPrinterCmd : Subcommand("strings", "List all the strings") {
            override fun execute() {
                programClassPool.classesAccept(StringPrinter())
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

    class PrintCmd : Subcommand("print", "Print information about the input") {

        var input by argument(ArgType.String, description = "Input file name")
        var classNameFilter by option(
            ArgType.String,
            description = "Class name filter",
            shortName = "cf",
            fullName = "classNameFilter"
        ).default("**")
        val programClassPool: ClassPool by lazy { read(input, classNameFilter, false) }

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
        ).required()
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
            val programClassPool = read(input, classNameFilter, false)
            val file = File(output)
            if (file.exists() && !forceOverwrite) {
                System.err.println("$file exists, use --force to overwrite")
                return
            }
            writeJar(programClassPool, file.absolutePath)
        }
    }

    class Jar2DexCmd : Subcommand("jar2dex", "Convert jar to dex - NOT YET IMPLEMENTED") {

        var input by argument(ArgType.String, description = "Input file name")
        var output by option(ArgType.String, description = "Output file name", shortName = "o", fullName = "output")
        override fun execute() {
            TODO("Not yet implemented")
        }
    }

    parser.subcommands(Dex2JarCmd(), Jar2DexCmd(), ListCmd(), PrintCmd(), TransformCmd())
    parser.parse(args)
}

fun read(
    filename: String,
    classNameFilter: String,
    isLibrary: Boolean
): ClassPool = IOUtil.read(
    ClassPath(ClassPathEntry(File(filename), false)),
    classNameFilter, true, isLibrary, false, false, false
) { dataEntryReader, classPoolFiller ->
    val dexReader = NameFilteredDataEntryReader(
        "classes*.dex",
        DexClassReader(
            true,
            classPoolFiller
        ),
        dataEntryReader
    )
    NameFilteredDataEntryReader(
        "**.smali",
        Smali2DexReader(
            (dexReader)
        ),
        dexReader
    )
}

class Smali2DexReader(private val delegate: DataEntryReader) : DataEntryReader {
    override fun read(dataEntry: DataEntry) {

        val options = SmaliOptions()
        val dexFile = File.createTempFile("classes", ".dex")
        options.outputDexFile = dexFile.absolutePath
        Smali.assemble(options, (dataEntry as FileDataEntry).file.absolutePath)

        val fileDataEntry = FileDataEntry(dexFile)
        delegate.read(fileDataEntry)
        dexFile.deleteOnExit()
    }
}
