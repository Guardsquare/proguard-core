package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.analysis.cpa.bam.BamCache
import proguard.analysis.cpa.interfaces.ProgramLocationDependent
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.Signature
import proguard.evaluation.value.ParticularReferenceValue
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class BamOperatorsTest : FreeSpec({

    val debug = false

    val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
        JavaSource(
            "ExtraString.java",
            """
                public class ExtraString
                {
                    public static void main(String[] args)
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Hello");
                        StringBuilder sb1 = new StringBuilder();
                        sb1.append("World");
                        String s = "Hello!";
                        String s1 = "World!";
                        foo(s1, sb1);
                        System.out.println(s);
                        System.out.println(sb.toString());
                    }

                    public static void foo(String s, StringBuilder sb) {
                        System.out.println(s);
                        System.out.println(sb.toString());
                    }
                }
            """.trimIndent(),
        ),
        javacArguments = mutableListOf("-source", "1.8", "-target", "1.8")
    )

    fun runBamCpa(className: String): BamCache<MethodSignature> {
        val cfa = CfaUtil.createInterproceduralCfa(programClassPool, libraryClassPool)
        if (debug) {
            try {
                Files.write(File("graph.dot").toPath(), CfaUtil.toDot(cfa).toByteArray(StandardCharsets.UTF_8))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        val clazz = programClassPool.getClass(className) as ProgramClass
        val mainSignature = Signature.of(clazz, clazz.findMethod("main", null)) as MethodSignature
        val bamCpaRun = JvmValueBamCpaRun.Builder(cfa, mainSignature).setReduceHeap(true).build()
        bamCpaRun.execute()
        return bamCpaRun.cpa.cache
    }

    "Bam operators test" - {
        val cache = runBamCpa("ExtraString")

        val fooReachedSets = cache
            .get(MethodSignature("ExtraString", "foo", "(Ljava/lang/String;Ljava/lang/StringBuilder;)V"))
        fooReachedSets.size shouldBe 1
        val fooReachedSet = fooReachedSets.first().reachedSet

        "State reduced correctly" - {

            val entryState = fooReachedSet.asCollection().find { (it as ProgramLocationDependent<*, *, *>).programLocation.offset == 0 } as JvmValueAbstractState

            "Correct parameters" {
                (entryState.getVariableOrDefault(0, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value() shouldBe "World!"
                (entryState.getVariableOrDefault(1, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value().toString() shouldBe "World"
            }

            "Just one string builder in heap" {
                val heap = entryState.heap
                heap.shouldBeInstanceOf<JvmShallowHeapAbstractState<Int, ValueAbstractState>>()
                heap.referenceToObject.size shouldBe 1
                val abstractState = heap.referenceToObject.values.first()
                abstractState.shouldBeInstanceOf<ValueAbstractState>()
                val value = abstractState.value
                value.shouldBeInstanceOf<ParticularReferenceValue>()
                value.value().toString() shouldBe "World"
            }
        }

        val mainReachedSets = cache
            .get(MethodSignature("ExtraString", "main", "([Ljava/lang/String;)V"))
        mainReachedSets.size shouldBe 1
        val mainReachedSet = mainReachedSets.first().reachedSet

        "State expanded correctly" - {

            val exitState = mainReachedSet.asCollection().find { (it as ProgramLocationDependent<*, *, *>).programLocation.offset == -1 } as JvmValueAbstractState

            "All variables available" {
                (exitState.getVariableOrDefault(1, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value().toString() shouldBe "Hello"
                (exitState.getVariableOrDefault(2, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value().toString() shouldBe "World"
                (exitState.getVariableOrDefault(3, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value() shouldBe "Hello!"
                (exitState.getVariableOrDefault(4, ValueAbstractState.UNKNOWN).value as ParticularReferenceValue).value() shouldBe "World!"
            }

            "Both string builder in the heap" {
                val heap = exitState.heap
                heap.shouldBeInstanceOf<JvmShallowHeapAbstractState<Int, ValueAbstractState>>()
                val referenceToObject = heap.referenceToObject
                referenceToObject.size shouldBe 2
                referenceToObject.values shouldExist { it.value is ParticularReferenceValue && (it.value as ParticularReferenceValue).value().toString() == "Hello" }
                referenceToObject.values shouldExist { it.value is ParticularReferenceValue && (it.value as ParticularReferenceValue).value().toString() == "World" }
            }
        }
    }
})
