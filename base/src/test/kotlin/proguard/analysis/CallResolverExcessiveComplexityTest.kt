/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.analysis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.string.shouldNotContain
import proguard.analysis.Metrics.MetricType.PARTIAL_EVALUATOR_EXCESSIVE_COMPLEXITY
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.evaluation.exception.ExcessiveComplexityException
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.util.withTestLogger

class CallResolverExcessiveComplexityTest : FunSpec({
    test("Check that an excessively complex method does not print anything to the log") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                """Test.jbc""",
                """
          version 1.8;
          public final class Test {
              private static final void foo() {
                  aconst_null
                  astore 0
                try1:
                 aload 0
                 invokevirtual java.lang.Object#int hashCode()
                 pop
                 return
                try1_end:
                try2:    
                catch java.lang.Throwable try1 try1_end
                  astore 0
                  aload 0
                  athrow
                try2_end:
                catch any try1 try1_end
                catch any try2 try2_end
                return
              }
          }
                """.trimIndent()
            )
        )

        val callGraph = CallGraph()
        val resolver =
            CallResolver.Builder(programClassPool, libraryClassPool, callGraph)
                .setClearCallValuesAfterVisit(false)
                .setUseDominatorAnalysis(true)
                .setEvaluateAllCode(false)
                .setIncludeSubClasses(true)
                .setMaxPartialEvaluations(50)
                .build()

        withTestLogger { outputStream ->
            programClassPool.classesAccept(resolver)
            (Metrics.counts[PARTIAL_EVALUATOR_EXCESSIVE_COMPLEXITY] ?: 0) shouldBeGreaterThan 0
            outputStream.toString() shouldNotContain ExcessiveComplexityException::class.java.name
        }
    }
})
