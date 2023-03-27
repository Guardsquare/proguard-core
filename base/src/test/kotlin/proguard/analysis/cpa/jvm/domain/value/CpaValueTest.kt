package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.Signature
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

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
                """.trimIndent()
            )

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
})
