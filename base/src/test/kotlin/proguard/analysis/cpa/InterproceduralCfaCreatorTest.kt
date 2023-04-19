/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import proguard.analysis.CallResolver
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.ClassPool
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class InterproceduralCfaCreatorTest : FreeSpec({

    fun checkNode(
        node: JvmCfaNode,
        offset: Int,
        isExitNode: Boolean,
        enteringEdgesSize: Int,
        leavingEdgesSize: Int
    ) {
        node.offset shouldBe offset
        node.isExitNode shouldBe isExitNode
        node.enteringEdges.size shouldBe enteringEdgesSize
        node.leavingEdges.size shouldBe leavingEdgesSize
    }

    "Call test" - {

        val classPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    public class A
                    {
                        public static void caller()
                        {
                            callee();
                        }
                
                        public static void callee()
                        {
                        }
                    }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        ).programClassPool
        val cfa = CfaUtil.createInterproceduralCfaFromClassPool(classPool)

        "Correct graph" {
            val callerEntry = cfa.functionEntryNodes.first { n -> n.signature.method == "caller" }
            checkNode(callerEntry, 0, false, 0, 2)

            val callEdge = callerEntry.leavingEdges.first { e -> e is JvmCallCfaEdge } as JvmCallCfaEdge
            callEdge.call.caller.signature.fqn shouldBe "LA;caller()V"
            callEdge.call.target.fqn shouldBe "LA;callee()V"

            val calleeEntry = cfa.functionEntryNodes.first { n -> n.signature.method == "callee" }
            callEdge.target shouldBeSameInstanceAs calleeEntry
            checkNode(calleeEntry, 0, false, 1, 1)
        }

        cfa.clear()
    }

    "Regression test: no nodes/edges added if caller and callee CFA are missing" - {

        val classPool = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    public class A
                    {
                        public static void caller()
                        {
                            callee();
                        }
                
                        public static void callee()
                        {
                        }
                    }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        ).programClassPool
        val cfa = JvmCfa()
        val callGraph = CallGraph()
        val resolver = CallResolver.Builder(
            classPool,
            ClassPool(),
            callGraph
        )
            .setEvaluateAllCode(true)
            .build()
        classPool.classesAccept(resolver)
        CfaUtil.addInterproceduralEdgesToCfa(cfa, callGraph)

        "No nodes added" {
            cfa.isEmpty shouldBe true
        }

        cfa.clear()
    }
})
