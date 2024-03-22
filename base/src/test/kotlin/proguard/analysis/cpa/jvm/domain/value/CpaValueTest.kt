package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.analysis.cpa.bam.BamCache
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.classfile.MethodSignature
import proguard.evaluation.value.ParticularReferenceValue
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

fun runCpa(cfa: JvmCfa, mainSignature: MethodSignature): BamCache<MethodSignature> {
    val bamCpaRun = JvmValueBamCpaRun.Builder(cfa, mainSignature).setReduceHeap(true).build()
    bamCpaRun.execute()
    return bamCpaRun.cpa.cache
}

fun getLastState(cache: BamCache<MethodSignature>, mainSignature: MethodSignature): JvmValueAbstractState {
    return cache
        .get(mainSignature)
        .map { it.reachedSet.asCollection().maxBy { (it as JvmValueAbstractState).programLocation.offset } }
        .single() as JvmValueAbstractState
}

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
        val cfa: JvmCfa = CfaUtil.createInterproceduralCfa(programClassPool)
        val mainSignature = MethodSignature("Test", "main", "([Ljava/lang/String;)V")

        val cache = runCpa(cfa, mainSignature)
        val last = getLastState(cache, mainSignature)

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

        val mainSignature = MethodSignature("Test", "test", "()V")
        val cache = runCpa(cfa, mainSignature)
        val last = getLastState(cache, mainSignature)

        "Correct string value" {
            val value = last.frame.localVariables[1].value
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.referenceValue().value.preciseValue shouldBe "1"
        }
    }

    "Checkcast instruction handled correctly in value analysis" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Test.jbc",
                """
                import java.lang.String;
                import java.lang.StringBuilder;
                version 8;
                public class Test extends java.lang.Object {
                
                    public void <init>() {
                        aload_0
                        invokespecial java.lang.Object#void <init>()
                        return
                    }
                    
                    public static String castOk() {
                        new StringBuilder
                        dup
                        ldc "foo"
                        invokespecial StringBuilder#void <init>(String)
                        checkcast StringBuilder
                        invokevirtual StringBuilder#String toString()
                        areturn
                    }
                    
                    public static String castInterface() {
                        new StringBuilder
                        dup
                        ldc "foo"
                        invokespecial StringBuilder#void <init>(String)
                        checkcast java.lang.CharSequence
                        invokevirtual StringBuilder#String toString()
                        areturn
                    }
                    
                    public static String castNotOk() {
                        new StringBuilder
                        dup
                        ldc "foo"
                        invokespecial StringBuilder#void <init>(String)
                        checkcast java.lang.ClassLoader
                        invokevirtual StringBuilder#String toString()
                        areturn
                    }
                }
                """.trimIndent(),
            ),
        )
        val cfa: JvmCfa = CfaUtil.createInterproceduralCfa(programClassPool, libraryClassPool)

        "Simple successful checkcast (same class)" - {
            val mainSignature = MethodSignature("Test", "castOk", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature)
            val last = getLastState(cache, mainSignature)

            val value = last.frame.operandStack[0].value
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.referenceValue().value.preciseValue shouldBe "foo"
        }

        "Successful checkcast (interface)" - {
            val mainSignature = MethodSignature("Test", "castInterface", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature)
            val last = getLastState(cache, mainSignature)

            val value = last.frame.operandStack[0].value
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.referenceValue().value.preciseValue shouldBe "foo"
        }

        "Unsuccessful cast" {
            val mainSignature = MethodSignature("Test", "castNotOk", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature)
            val last = getLastState(cache, mainSignature)

            val value = last.frame.operandStack[0].value
            value.shouldNotBeInstanceOf<ParticularReferenceValue>()
        }
    }
})
