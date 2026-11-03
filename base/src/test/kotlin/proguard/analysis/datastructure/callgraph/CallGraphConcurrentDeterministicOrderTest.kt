/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2024 Guardsquare NV
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
package proguard.analysis.datastructure.callgraph

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.CallResolver
import proguard.classfile.ClassPool
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.util.CallGraphWalker

class CallGraphConcurrentDeterministicOrderTest : FunSpec({
    // Setup: Create a call graph where testMethod() has three predecessors and successors.
    val classPool = ClassPoolBuilder.fromSource(
        JavaSource(
            "A.java",
            """
            public class A
            {
                public static void testMethod()
                {
                    succ1();
                    succ2();
                    succ3();
                }
                public static void predA()
                {
                    testMethod();
                }
                public static void predB()
                {
                    testMethod();
                }
                public static void predC()
                {
                    testMethod();
                }
                public static void succ1()
                {
                }
                public static void succ2()
                {
                }
                public static void succ3()
                {
                }
            }
            """.trimIndent(),
        ),
        javacArguments = listOf("-source", "1.8", "-target", "1.8"),
    ).programClassPool
    val callGraph = CallGraph.concurrentCallGraph()
    val resolver = CallResolver.Builder(
        classPool,
        ClassPool(),
        callGraph,
    ).build()
    classPool.classesAccept(resolver)
    val startSignature = MethodSignature("A", "testMethod", "()V")

    test("concurrent call graph iterates incoming calls in sorted order regardless of insertion order") {
        val calls = callGraph.incoming[startSignature]!!.toList()
        val sortedCalls = calls.sorted()

        calls shouldBe sortedCalls
    }

    test("concurrent call graph iterates outgoing calls in sorted order regardless of insertion order") {
        val calls = callGraph.outgoing[startSignature]!!.toList()
        val sortedCalls = calls.sorted()

        calls shouldBe sortedCalls
    }

    test("adding calls in reverse order yields the same iteration order") {
        // Collect calls from the original call graph
        val originalIncoming = callGraph.incoming[startSignature]!!.toList()
        val originalOutgoing = callGraph.outgoing[startSignature]!!.toList()

        // Create a new concurrent call graph and add the same calls in reverse order
        val reverseGraph = CallGraph.concurrentCallGraph()
        for (call in originalIncoming.reversed()) {
            reverseGraph.addCall(call)
        }
        for (call in originalOutgoing.reversed()) {
            reverseGraph.addCall(call)
        }

        // The iteration order should be the same (sorted), regardless of insertion order
        reverseGraph.incoming[startSignature]!!.toList() shouldBe originalIncoming
        reverseGraph.outgoing[startSignature]!!.toList() shouldBe originalOutgoing
    }

    test("successor order is deterministic with concurrent call graph") {
        val succ1 = MethodSignature("A", "succ1", "()V")
        val succ2 = MethodSignature("A", "succ2", "()V")
        val succ3 = MethodSignature("A", "succ3", "()V")

        val successors: Set<MethodSignature> = CallGraphWalker.getSuccessors(callGraph, startSignature, 100)
        val orderedSuccessors = successors.toList()

        // startSignature is always first (root of exploration), then successors in sorted order
        orderedSuccessors shouldBe listOf(startSignature, succ1, succ2, succ3)
    }

    test("predecessor order is deterministic with concurrent call graph") {
        val pred1 = MethodSignature("A", "predA", "()V")
        val pred2 = MethodSignature("A", "predB", "()V")
        val pred3 = MethodSignature("A", "predC", "()V")

        val predecessors: Set<MethodSignature> = CallGraphWalker.getPredecessors(callGraph, startSignature, 100)
        val orderedPredecessors = predecessors.toList()

        // startSignature is always first (root of exploration), then predecessors in sorted order
        orderedPredecessors shouldBe listOf(startSignature, pred1, pred2, pred3)
    }
})
