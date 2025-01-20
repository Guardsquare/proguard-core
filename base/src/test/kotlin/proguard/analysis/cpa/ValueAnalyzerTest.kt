package proguard.analysis.cpa

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.interfaces.AbortOperator
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.ValueAnalyzer
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class ValueAnalyzerTest : BehaviorSpec({
    val (programClassPool, libraryClassPool) =
        ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A
                    {

                        public void main()
                        {
                            String s = "Hello" + " World!";                    
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
    Given("A value analyzer with an abort operator that always stops") {
        val alwaysAbortOperator = AbortOperator { true }
        val valueAnalyzer = ValueAnalyzer.Builder(cfa, programClassPool, libraryClassPool).setAbortOperator(alwaysAbortOperator).build()
        When("It is executed") {
            val mainSignature = MethodSignature("A", "main", "()V")
            val result = valueAnalyzer.analyze(mainSignature)
            Then("There is no reached state besides the first one") {
                result.mainMethodReachedSet.asCollection().size shouldBe 1
            }
        }
    }
})
