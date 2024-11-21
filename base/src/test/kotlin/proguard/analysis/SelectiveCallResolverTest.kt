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
import proguard.analysis.datastructure.callgraph.Call
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.MethodSignature
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.TracedStack
import proguard.evaluation.TracedVariables
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.Value
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

    val SIG_UNINTERESTING = MethodSignature("C", "uninteresting", "()V")

    val code = JavaSource(
        "Test.java",
        """
public class Test {

  public static void main(String[] args) {
    A a = new A();
    B b = new B();
    C c = new C();

    a.test();
    b.test();
    c.uninteresting();
  }
}

class A {
  
  public void test() {
    this.test("A: test()");
    new B().test();
    new C().uninteresting();
  }
  
  public void test(String value) {
    System.out.println(value);
  }
}

class B {

  public void test() {
    new C().uninteresting();
  }
}

class C {

  public void uninteresting() {
    System.out.println("C: evaluated inside uninteresting()");
  }
}
""",

    )

    val classPools = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

    class CallSaver : CallHandler {

        val incompleteCalls = ArrayList<Call>()
        val completeCalls = HashMap<Call, Value?>()
        override fun handleCall(call: Call, stack: TracedStack, localVariables: TracedVariables) {
            if (call.hasIncompleteTarget()) {
                incompleteCalls.add(call)
            } else {
                completeCalls[call] = call.getArgument(0)
            }
        }
    }

    val callSaver = CallSaver()

    val interestingMethods: Set<MethodSignature> = setOf(SIG_INTERESTING)

    val interestingCallPredicates: Set<Predicate<Call>> = setOf(Predicate { c -> "test" == c.target.method })

    val callGraph = CallGraph()
    val resolver =
        CallResolver.Builder(
            classPools.programClassPool,
            classPools.libraryClassPool,
            callGraph,
            callSaver,
        )
            .setClearCallValuesAfterVisit(true)
            .setUseDominatorAnalysis(false)
            .setEvaluateAllCode(true)
            .setIncludeSubClasses(false)
            .setMaxPartialEvaluations(50)
            .setSkipIncompleteCalls(false)
            .setArrayValueFactory(DetailedArrayValueFactory(ParticularReferenceValueFactory()))
            .useSelectiveParameterReconstruction(interestingMethods, interestingCallPredicates)
            .build()

    classPools.programClassPool.classesAccept(resolver)

    test("Selective visited interesting signature") {
        val interestingCalls = callSaver.completeCalls.filter { it.key.target == SIG_INTERESTING }.map { it.value }.toList()
        interestingCalls.size shouldBe 2
        interestingCalls.filterIsInstance<IdentifiedReferenceValue>().count() shouldBe 2
        interestingCalls.filterIsInstance<ParticularReferenceValue>().count() shouldBe 1

        callSaver.completeCalls.count { call -> call.key.target == SIG_PREDICATE_I } shouldBe 1

        callSaver.completeCalls.count { call -> call.key.target == SIG_PREDICATE_II } shouldBe 2

        callSaver.completeCalls.count { call -> call.key.target == SIG_UNINTERESTING } shouldBe 0
    }

    test("CallGraph not affected by selective parameter reconstruction") {
        callGraph.incoming[SIG_INTERESTING]!!.size shouldBe 2

        callGraph.incoming[SIG_PREDICATE_I]!!.size shouldBe 1

        callGraph.incoming[SIG_UNINTERESTING]!!.size shouldBe 3
    }
})
