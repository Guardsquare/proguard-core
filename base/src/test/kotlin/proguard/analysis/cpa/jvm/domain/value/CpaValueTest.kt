package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.Signature
import proguard.evaluation.value.ParticularReferenceValue
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class CpaValueTest : FreeSpec({

    "Given overloaded fields with different types" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Test.jbc",
                """
                version 8;
                public class Test extends java.lang.Object {
                    public static int a;
                    public static java.lang.StringBuilder a;
                
                    public void <init>() {
                        aload_0
                        invokespecial java.lang.Object#void <init>()
                        return
                    }
                
                    public static void main(java.lang.String[]) {
                        iconst_1
                        putstatic #int a
                        new java.lang.StringBuilder
                        dup
                        invokespecial java.lang.StringBuilder#void <init>()
                        putstatic #java.lang.StringBuilder a
                        getstatic #java.lang.StringBuilder a
                        return
                    }
                }
                """.trimIndent(),
            ),

        )
        var cfa: JvmCfa = CfaUtil.createInterproceduralCfa(programClassPool)
        val clazz = programClassPool.getClass("Test") as ProgramClass
        val mainSignature = Signature.of(clazz, clazz.findMethod("main", null)) as MethodSignature
        val bamCpaRun = JvmValueBamCpaRun.Builder(cfa, mainSignature).setReduceHeap(true).build()
        bamCpaRun.execute()
        val cache = bamCpaRun.cpa.getCache()
        val last = cache
            .get(mainSignature)
            .map { it.reachedSet.asCollection().maxBy { (it as JvmValueAbstractState).programLocation.offset } }
            .single() as JvmValueAbstractState
        "Then there should be two fields in the JvmValueAbstractState" {
            last.staticFields.size shouldBe 2
        }
    }

    "Executed method taking category2 value" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                    import java.lang.StringBuilder;

                    public class Test {
                        public static void test() {
                            StringBuilder builder = new StringBuilder();
                            builder.append(1L);
                            String s = builder.toString();
                        }
                    }
                """,
            ),
            initialize = true,
        )
        val cfa: JvmCfa = CfaUtil.createInterproceduralCfa(programClassPool, libraryClassPool)
        val clazz = programClassPool.getClass("Test") as ProgramClass
        val mainSignature = Signature.of(clazz, clazz.findMethod("test", null)) as MethodSignature
        val bamCpaRun = JvmValueBamCpaRun.Builder(cfa, mainSignature).setReduceHeap(true).build()
        bamCpaRun.execute()
        val cache = bamCpaRun.cpa.cache
        val last = cache
            .get(mainSignature)
            .map { it.reachedSet.asCollection().maxBy { (it as JvmValueAbstractState).programLocation.offset } }
            .single() as JvmValueAbstractState
        "Correct string value" {
            val value = last.frame.localVariables[1].value
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.referenceValue().value() shouldBe "1"
        }
    }
})
