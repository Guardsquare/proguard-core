/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2026 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.analysis.cpa

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.analysis.CallResolver
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.analysis.cpa.domain.taint.TaintSource
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmStaticFieldTaintSource
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.TaintAnalyzer
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.ClassPool
import proguard.classfile.FieldSignature
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.util.Objects

private val SOURCE_FIELD = FieldSignature("Test", "tainted", "Ljava/lang/String;")
private val NON_SOURCE_FIELD = FieldSignature("Test", "clean", "Ljava/lang/String;")

class JvmStaticFieldTaintSourceTest : BehaviorSpec({

    val classPool = ClassPoolBuilder.fromSource(
        JavaSource(
            "Test.java",
            """
                public class Test
                {
                    public static String tainted = "";
                    public static String clean = "";

                    public void test()
                    {
                        sink(Test.tainted);
                    }

                    public void testClean()
                    {
                        sink(Test.clean);
                    }

                    public void sink(String s)
                    {
                    }
                }
            """.trimIndent(),
        ),
        javacArguments = listOf("-source", "1.8", "-target", "1.8"),
    ).programClassPool
    val callGraph = CallGraph()
    val resolver = CallResolver
        .Builder(
            classPool,
            ClassPool(),
            callGraph,
        )
        .setEvaluateAllCode(true)
        .build()
    classPool.classesAccept(resolver)
    MethodSignature.clearCache()
    val interproceduralCfa = CfaUtil.createInterproceduralCfa(classPool, callGraph)

    val taintSource = JvmStaticFieldTaintSource(SOURCE_FIELD)
    val sink = JvmInvokeTaintSink(
        MethodSignature("Test", "sink", "(Ljava/lang/String;)V"),
        false,
        setOf(1),
        setOf(),
    )
    val mainSignature = MethodSignature("Test", "test", "()V")
    val testCleanSignature = MethodSignature("Test", "testClean", "()V")

    fun analyzeFrom(sources: Set<TaintSource>, mainSignature: MethodSignature) = TaintAnalyzer
        .Builder(interproceduralCfa, sources, setOf(sink))
        .build()
        .analyze(mainSignature)
        .taintAnalysisResult

    Given("A taint analyzer with a static field taint source") {
        When("A method reading the source field is analyzed") {
            val result = analyzeFrom(setOf(taintSource), mainSignature)

            Then("The sink is triggered with the static field source as taint") {
                result.endpointToTriggeredSinks.size shouldBe 1
                result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
                result.endpoints.size shouldBe 1
                result.endpoints.first().extractFirstValue(SetAbstractState.bottom()) shouldBe setOf(taintSource)
            }
        }

        When("A method reading a non-source field is analyzed") {
            val result = analyzeFrom(setOf(taintSource), testCleanSignature)

            Then("The sink is not triggered") {
                result.endpoints.size shouldBe 0
            }
        }
    }

    Given("Static field taint sources") {
        Then("Sources with the same field are equal") {
            JvmStaticFieldTaintSource(SOURCE_FIELD) shouldBe JvmStaticFieldTaintSource(SOURCE_FIELD)
            JvmStaticFieldTaintSource(SOURCE_FIELD) shouldNotBe JvmStaticFieldTaintSource(NON_SOURCE_FIELD)
            JvmStaticFieldTaintSource(SOURCE_FIELD).signature.toString() shouldBe "LTest;tainted"
        }
    }

    Given("A taint analyzer with a custom static field taint source implementation") {
        val customSource = CustomStaticFieldTaintSource(SOURCE_FIELD)
        When("A method reading the source field is analyzed") {
            val result = analyzeFrom(setOf(taintSource, customSource), mainSignature)

            Then("The sink is triggered with the custom source as taint") {
                result.endpointToTriggeredSinks.size shouldBe 1
                result.endpointToTriggeredSinks.values.first() shouldBe listOf(sink)
                result.endpoints.size shouldBe 1
                result.endpoints.first().extractFirstValue(SetAbstractState.bottom()) shouldBe setOf(
                    taintSource,
                    customSource,
                )
            }
        }
    }
})

private class CustomStaticFieldTaintSource(private val fieldSignature: FieldSignature) : TaintSource {

    override fun getSignature(): FieldSignature = fieldSignature

    override fun equals(other: Any?): Boolean =
        other is CustomStaticFieldTaintSource && other.fieldSignature == fieldSignature

    override fun hashCode(): Int = Objects.hash(fieldSignature)
}
