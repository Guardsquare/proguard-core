package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.analysis.cpa.bam.BamCache
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.ValueAnalyzer
import proguard.classfile.ClassPool
import proguard.classfile.MethodSignature
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

fun runCpa(cfa: JvmCfa, mainSignature: MethodSignature, programClassPool: ClassPool, libraryClassPool: ClassPool): BamCache<ValueAbstractState> {
    return ValueAnalyzer.Builder(cfa, programClassPool, libraryClassPool).build().analyze(mainSignature).resultCache
}

fun getLastState(cache: BamCache<ValueAbstractState>, mainSignature: MethodSignature): JvmValueAbstractState {
    return cache
        .get(mainSignature)
        .map { it.reachedSet.asCollection().maxBy { (it as JvmValueAbstractState).programLocation.offset } }
        .single() as JvmValueAbstractState
}

class CpaValueTest : BehaviorSpec({

    Given("Code using overloaded fields with different types") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
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

        When("The value analysis is run on the code") {
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("There analyzed return state contains two fields") {
                val last = getLastState(cache, mainSignature)
                last.staticFields.size shouldBe 2
            }
        }
    }

    Given("Code calling a method taking a category 2 parameter") {
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

        When("The value analysis is run on the code") {
            val mainSignature = MethodSignature("Test", "test", "()V")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)
            Then("The the analyzed return state contains the correct string") {
                val last = getLastState(cache, mainSignature)
                val value = last.frame.localVariables[1].value
                value.shouldBeInstanceOf<ParticularReferenceValue>()
                value.referenceValue().value.preciseValue shouldBe "1"
            }
        }
    }

    Given("Code using the checkcast instruction") {
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

        When("The analysis is run on code doing a simple cast on the same class") {
            val mainSignature = MethodSignature("Test", "castOk", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return state contains the correct string") {
                val last = getLastState(cache, mainSignature)
                val value = last.frame.operandStack[0].value
                value.shouldBeInstanceOf<ParticularReferenceValue>()
                value.referenceValue().value.preciseValue shouldBe "foo"
            }
        }

        When("The analysis is run on code doing a simple cast on an interface") {
            val mainSignature = MethodSignature("Test", "castInterface", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return state contains the correct string") {
                val last = getLastState(cache, mainSignature)
                val value = last.frame.operandStack[0].value
                value.shouldBeInstanceOf<ParticularReferenceValue>()
                value.referenceValue().value.preciseValue shouldBe "foo"
            }
        }

        When("The analysis is run on code doing an invalid cast") {
            val mainSignature = MethodSignature("Test", "castNotOk", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return state does not contain a known value") {
                // At the moment the correct behavior of throwing a ClassCastException is not handled by the analysis.
                // Instead, the analysis just cleans the known value.
                val last = getLastState(cache, mainSignature)
                val value = last.frame.operandStack[0].value
                value.shouldNotBeInstanceOf<ParticularReferenceValue>()
            }
        }
    }

    Given("Code containing an analyzed method with an unknown parameter") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                    import java.util.Random;
                    class Test
                    {

                        public static String test()
                        {
                            return "Hello".substring(new Random().nextInt(5));
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        )
        val cfa = CfaUtil.createInterproceduralCfa(
            programClassPool,
            libraryClassPool,
        )
        When("The value analysis is run on the code") {
            val mainSignature = MethodSignature("Test", "test", "()Ljava/lang/String;")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return value is identified and the reference id is a CFA node") {
                val last = getLastState(cache, mainSignature)
                val ret = last.frame.operandStack[0].value
                ret.shouldBeInstanceOf<IdentifiedReferenceValue>()
                ret.id.shouldBeInstanceOf<JvmCfaNode>()
            }
        }
    }

    Given("Code containing an analyzed method returning an array") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                    class Test
                    {
                        public static byte[] test()
                        {
                            return "Hello".getBytes();
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        )
        val cfa = CfaUtil.createInterproceduralCfa(
            programClassPool,
            libraryClassPool,
        )
        When("The value analysis is run on the code") {
            val mainSignature = MethodSignature("Test", "test", "()[B")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return value is identified and the reference id is a CFA node") {
                val last = getLastState(cache, mainSignature)
                val ret = last.frame.operandStack[0].value
                // This is checking the current behavior, but it would be more correct to have an IdentifiedArrayReferenceValue
                ret.shouldBeInstanceOf<IdentifiedReferenceValue>()
                ret.type shouldBe "[B"
                ret.id.shouldBeInstanceOf<JvmCfaNode>()
            }
        }
    }

    Given("Code containing an analyzed method with an unknown parameter and returning an array") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                    import java.io.UnsupportedEncodingException;
                    import java.util.Random;
                    class Test
                    {

                        public static byte[] test() throws UnsupportedEncodingException
                        {
                            String charset = new Random().nextBoolean() ? "UTF-8" : "UTF-16";
                            return "Hello".getBytes(charset);
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        )
        val cfa = CfaUtil.createInterproceduralCfa(
            programClassPool,
            libraryClassPool,
        )
        When("The value analysis is run on the code") {
            val mainSignature = MethodSignature("Test", "test", "()[B")
            val cache = runCpa(cfa, mainSignature, programClassPool, libraryClassPool)

            Then("The analyzed return value is identified and the reference id is a CFA node") {
                val last = getLastState(cache, mainSignature)
                val ret = last.frame.operandStack[0].value
                // This is checking the current behavior, but it would be more correct to have an IdentifiedArrayReferenceValue
                ret.shouldBeInstanceOf<IdentifiedReferenceValue>()
                ret.type shouldBe "[B"
                ret.id.shouldBeInstanceOf<JvmCfaNode>()
            }
        }
    }
})
