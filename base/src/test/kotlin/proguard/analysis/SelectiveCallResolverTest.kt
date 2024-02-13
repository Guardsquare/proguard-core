/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import proguard.analysis.datastructure.callgraph.Call
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.MethodSignature
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import java.util.function.Predicate

class SelectiveCallResolverTest : FunSpec({

    val SIG_INTERESTING = MethodSignature(
        "java/io/PrintStream",
        "println",
        "(Ljava/lang/String;)V",
    )

    val SIG_PREDICATE_I = MethodSignature(
        "A",
        "test",
        "(Ljava/lang/String;)V",
    )

    val SIG_PREDICATE_II = MethodSignature(
        "B",
        "test",
        "()V",
    )

    val SIG_IGNORE = MethodSignature("C", "notEvaluated", "()V")

    val code = JavaSource(
        "Test.java",
        """public class Test {

  public static void main(String[] args) {
    A a = new A();
    B b = new B();
    C c = new C();

    a.test("text");
    b.test();
    c.notEvaluated();
  }
}

class A {

  public void test(String text) {
    System.out.println("text");
    System.out.println(text);
    new C().notEvaluated();
  }

  public void test() {
    new A().test("other text");
    new B().test();
  }
}

class B {

  public void test() {
    new A().test("other text");
    new C().notEvaluated();
  }
}

class C {

  public void notEvaluated() {
    System.out.println("notEvaluated");
  }
}
""",

    )

    val classPools = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

    class CallSaver : CallVisitor {

        val incompleteCalls = ArrayList<Call>()
        val completeCalls = ArrayList<Call>()
        override fun visitCall(call: Call) {
            if (call.hasIncompleteTarget()) {
                incompleteCalls.add(call)
            } else {
                completeCalls.add(call)
            }
        }
    }

    val callSaver = CallSaver()

    val interestingMethods: Set<MethodSignature> =
        setOf(SIG_INTERESTING)

    val interestingCallPredicates: Set<Predicate<Call>> =
        setOf(Predicate { c -> "test" == c.target.method })

    val callGraph = CallGraph()
    val resolver =
        CallResolver.Builder(
            classPools.programClassPool,
            classPools.libraryClassPool,
            callGraph,
            callSaver,
        )
            .setClearCallValuesAfterVisit(false)
            .setUseDominatorAnalysis(true)
            .setEvaluateAllCode(true)
            .setIncludeSubClasses(true)
            .setMaxPartialEvaluations(50)
            .setSkipIncompleteCalls(false)
            .useSelectiveParameterReconstruction(interestingMethods, interestingCallPredicates)
            .build()

    classPools.programClassPool.classesAccept(resolver)

    test("Selective visited interesting signature") {
        val interestingMethodCalls = callSaver.completeCalls.filter { call -> call.target == SIG_INTERESTING }.toList()

        interestingMethodCalls.size shouldBe 18
        interestingMethodCalls.forEach { call -> call.getArgument(0) shouldBe instanceOf<IdentifiedReferenceValue>() }

        val count1 = callSaver.completeCalls.count { call -> call.target == SIG_PREDICATE_I }
        count1 shouldBe 3

        val count2 = callSaver.completeCalls.count { call -> call.target == SIG_PREDICATE_II }
        count2 shouldBe 2

        val ignored = callSaver.completeCalls.count { call -> call.target == SIG_IGNORE }
        ignored shouldBe 0
    }

    test("CallGraph not affected by selective parameter reconstruction") {
        val countInterestingMethodCalls = callGraph.incoming[SIG_INTERESTING]!!.size

        // there is some deduplication in the call graph sets that makes the numbers different
        countInterestingMethodCalls shouldBe 3

        val count1 = callGraph.incoming[SIG_PREDICATE_I]!!.size
        count1 shouldBe 3

        val ignored = callGraph.incoming[SIG_IGNORE]!!.size
        ignored shouldBe 3
    }
})
