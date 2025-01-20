package proguard.analysis.cpa

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.interfaces.AbortOperator
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.TaintAnalyzer
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class TaintAnalyzerTest : BehaviorSpec({
    val cfa = CfaUtil.createInterproceduralCfa(
        ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A
                    {

                        public void main()
                        {
                            String s;
                            s = source1(); // out offset: 3
                            sink(s); // in offset: 5
                        }

                        public static void sink(String s)
                        {
                        }

                        public static String source1()
                        {
                            return null;
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        ).programClassPool,
    )
    Given("A taint analyzer with an abort operator that always stops") {
        val alwaysAbortOperator = AbortOperator { true }
        val taintAnalyzer = TaintAnalyzer.Builder(cfa, setOf(), setOf()).setAbortOperator(alwaysAbortOperator).build()
        When("It is executed") {
            val result = taintAnalyzer.analyze(MethodSignature("A", "main", "()V"))
            Then("There is no reached state besides the first one") {
                result.taintAnalysisResult.mainMethodReachedSet.asCollection().size shouldBe 1
            }
        }
    }
})
