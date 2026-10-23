package proguard.dexfile.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.visitor.ClassPoolFiller
import proguard.dexfile.reader.DexFileReader.KEEP_CLINIT
import proguard.dexfile.reader.DexFileReader.SKIP_CODE
import proguard.dexfile.reader.DexFileReader.SKIP_DEBUG
import proguard.dexfile.reader.DexReaderFactory.createSingleReader
import proguard.dexfile.reader.node.DexFileNode
import java.io.File
import java.io.FileInputStream

class Dex2ProConcurrentConversionTest : BehaviorSpec({
    Given("Several dex files and one shared Dex2Pro converting them into one class pool") {
        val files = 4
        val classesPerFile = 200
        val dexFiles =
            (0 until files).map { fileIndex ->
                val sourceDir = tempdir()
                repeat(classesPerFile) { classIndex ->
                    sourceDir.resolve("R${fileIndex}_$classIndex.smali")
                        .writeText(".class public LR${fileIndex}_$classIndex;\n.super Ljava/lang/Object;\n")
                }
                val options = SmaliOptions()
                val dexFile = tempfile("dex2pro-race-$fileIndex-", ".dex")
                options.outputDexFile = dexFile.absolutePath
                Smali.assemble(options, sourceDir.absolutePath) shouldBe true
                dexFile
            }
        val expectedClassNames =
            (0 until files).flatMap { fileIndex ->
                (0 until classesPerFile).map { classIndex -> "R${fileIndex}_$classIndex" }
            }.toSet()

        When("The conversions of all files overlap on the shared executor") {
            val rounds = (0 until 5).map { convertRound(dexFiles, expectedClassNames) }

            Then("The shared class pool stays intact after every round") {
                rounds.forEach { round ->
                    round.poolSize shouldBe round.expectedClassNames.size
                    round.iterated.keys shouldContainExactlyInAnyOrder round.expectedClassNames
                    round.expectedClassNames.forEach { name ->
                        round.pool.getClass(name) shouldBeSameInstanceAs round.iterated[name]
                    }
                }
            }
        }
    }
})

private class ConversionRound(
    val pool: ClassPool,
    val poolSize: Int,
    val expectedClassNames: Set<String>,
    val iterated: Map<String, Clazz>,
)

private fun convertRound(dexFiles: List<File>, expectedClassNames: Set<String>): ConversionRound {
    val fileNodes = dexFiles.map { dexFile -> parseDexFile(dexFile) }

    val pool = ClassPool()
    val dex2pro = Dex2Pro(8)
    fileNodes.forEach { node -> dex2pro.convertDex(node, ClassPoolFiller(pool)) }
    dex2pro.shutdown(60)

    val iterated = mutableMapOf<String, Clazz>()
    pool.classesAccept { clazz -> iterated[clazz.name] = clazz }
    return ConversionRound(pool, pool.size(), expectedClassNames, iterated)
}

private fun parseDexFile(dexFile: File): DexFileNode {
    val fileNode = DexFileNode()
    FileInputStream(dexFile).use { inputStream ->
        createSingleReader(inputStream).accept(fileNode, SKIP_CODE or KEEP_CLINIT or SKIP_DEBUG)
    }
    return fileNode
}
