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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import proguard.analysis.datastructure.callgraph.Call
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.MethodSignature
import proguard.classfile.Signature
import proguard.evaluation.value.Value.ALWAYS
import proguard.evaluation.value.Value.MAYBE
import proguard.evaluation.value.Value.NEVER
import testutils.ClassPoolBuilder
import java.nio.file.Paths

class CallResolverTest : FreeSpec({
    testOrder = TestCaseOrder.Sequential

    val SUPER_TEST =
        MethodSignature(
            "Super",
            "test",
            "()V"
        )
    val SUPER_STATIC_TEST = MethodSignature(
        "Super",
        "staticTest",
        "()V"
    )
    val A_TEST =
        MethodSignature("A", "test", "()V")
    val A_STATIC_TEST = MethodSignature(
        "A",
        "staticTest",
        "()V"
    )
    val B_TEST =
        MethodSignature("B", "test", "()V")
    val B_STATIC_TEST = MethodSignature(
        "B",
        "staticTest",
        "()V"
    )
    val NOT_OVERRIDDEN_TEST =
        MethodSignature(
            "NotOverridden",
            "test",
            "()V"
        )
    val SUPERINTERFACE_DEFAULT_TEST =
        MethodSignature(
            "SuperInterface",
            "defaultTest",
            "()V"
        )
    val SUPERINTERFACE_ABSTRACT_TEST =
        MethodSignature(
            "SuperInterface",
            "abstractTest",
            "()V"
        )
    val SUBINTERFACE_DEFAULT_TEST =
        MethodSignature(
            "SubInterface",
            "defaultTest",
            "()V"
        )
    val NORMAL_IMPL_DEFAULT_TEST =
        MethodSignature(
            "NormalImplementor",
            "defaultTest",
            "()V"
        )
    val NORMAL_IMPL_ABSTRACT_TEST =
        MethodSignature(
            "NormalImplementor",
            "abstractTest",
            "()V"
        )
    val SUPER_IMPL_DEFAULT_TEST =
        MethodSignature(
            "SuperImplementor",
            "defaultTest",
            "()V"
        )
    val SUB_IMPL_DEFAULT_TEST =
        MethodSignature(
            "SubImplementor",
            "defaultTest",
            "()V"
        )
    val PRINTLN =
        MethodSignature(
            "java/io/PrintStream",
            "println",
            "()V"
        )
    val LAMBDA = MethodSignature("Main", "lambda\$dynamic$0", "(Ljava/lang/String;)Z")
    val topLevel = Paths.get("src", "test", "resources", "callResolver", "hierarchy")

    val classPools = ClassPoolBuilder.fromDirectory(topLevel.toFile())

    val callGraph = CallGraph()
    val resolver =
        CallResolver.Builder(classPools.programClassPool, classPools.libraryClassPool, callGraph)
            .setClearCallValuesAfterVisit(false)
            .setUseDominatorAnalysis(true)
            .setEvaluateAllCode(true)
            .setIncludeSubClasses(true)
            .setMaxPartialEvaluations(50)
            .build()

    classPools.programClassPool.classesAccept(resolver)

    val unchecked = HashMap<Signature, MutableList<Call>>()

    callGraph.outgoing.forEach { (caller, targets) ->
        val outgoing = targets.filter { e -> "<init>" !in e.target.fqn }.toMutableList()
        if (outgoing.isNotEmpty()) {
            unchecked[caller] = outgoing
        }
    }

    infix fun Call.andThrow(expected: Int): Call {
        throwsNullptr shouldBe expected
        return this
    }

    infix fun Call.controlFlowDependent(expected: Boolean): Call {
        controlFlowDependent shouldBe expected
        return this
    }

    infix fun Call.typeDependent(expected: Boolean): Call {
        runtimeTypeDependent shouldBe expected
        return this
    }

    infix fun MethodSignature.shouldCall(expected: MethodSignature): Call {
        val caller = this
        callGraph.outgoing shouldHaveKey caller
        val matches =
            callGraph.outgoing[caller]!!.filter { it.caller.signature == caller && it.target == expected }
        matches shouldHaveSize 1

        if (caller in unchecked) {
            unchecked[caller]!!.removeAll { it in matches }
            if (unchecked[caller]!!.isEmpty()) {
                unchecked.remove(caller)
            }
        }
        return matches[0]
    }

    infix fun MethodSignature.shouldNotCall(expected: Signature) {
        val caller = this
        callGraph.outgoing shouldHaveKey caller
        callGraph.outgoing[caller]!!.none { it.caller.signature == caller && it.target == expected } shouldBe true
    }

    "Ambiguous virtual call" {
        val caller = MethodSignature(
            "Main",
            "ambiguous",
            "()V"
        )
        caller shouldCall A_TEST andThrow MAYBE controlFlowDependent false typeDependent true
        caller shouldCall B_TEST andThrow MAYBE controlFlowDependent false typeDependent true
        caller shouldCall SUPER_TEST andThrow ALWAYS controlFlowDependent false typeDependent true

        caller shouldCall SUPER_STATIC_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall A_STATIC_TEST
        caller shouldNotCall B_STATIC_TEST
    }

    "Exact virtual call" {
        val a =
            MethodSignature(
                "Main",
                "a",
                "()V"
            )
        val b =
            MethodSignature(
                "Main",
                "b",
                "()V"
            )
        val s =
            MethodSignature(
                "Main",
                "s",
                "()V"
            )

        a shouldCall A_TEST andThrow NEVER controlFlowDependent false typeDependent false
        a shouldNotCall SUPER_TEST

        b shouldCall B_TEST andThrow NEVER controlFlowDependent false typeDependent false
        b shouldNotCall SUPER_TEST

        s shouldCall SUPER_TEST andThrow NEVER controlFlowDependent false typeDependent false
    }

    "Call to non-overridden method" {
        val caller = MethodSignature(
            "Main",
            "notOverridden",
            "()V"
        )
        caller shouldCall SUPER_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall NOT_OVERRIDDEN_TEST
    }

    "External call" {
        val caller = MethodSignature(
            "Main",
            "external",
            "()V"
        )
        caller shouldCall PRINTLN andThrow MAYBE controlFlowDependent false typeDependent true
    }

    "Call on null object should throw" {
        val caller = MethodSignature(
            "Main",
            "alwaysNull",
            "()V"
        )
        caller shouldCall SUPER_TEST andThrow ALWAYS controlFlowDependent false typeDependent false
    }

    "Invokespecial" {
        var caller = MethodSignature(
            "SuperCall",
            "test",
            "()V"
        )
        caller shouldCall SUPER_TEST andThrow NEVER controlFlowDependent false typeDependent false

        caller = MethodSignature(
            "SubNotOverridden",
            "test",
            "()V"
        )
        caller shouldCall SUPER_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall MethodSignature(
            "NotOverridden",
            "test",
            "()V"
        )
    }

    "Invokedynamic" {
        val caller = MethodSignature("Main", "dynamic", "()V")
        caller shouldCall LAMBDA andThrow NEVER

        LAMBDA shouldCall MethodSignature(
            "java/lang/String",
            "length",
            "()I"
        ) andThrow MAYBE
    }

    "Invokeinterface" {
        val caller = MethodSignature(
            "Main",
            "invokeInterface",
            "()V"
        )
        caller shouldCall SUPERINTERFACE_DEFAULT_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall NORMAL_IMPL_DEFAULT_TEST

        caller shouldCall NORMAL_IMPL_ABSTRACT_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall SUPERINTERFACE_ABSTRACT_TEST
    }

    "Most specific default method" {
        var caller = MethodSignature(
            "Main",
            "mostSpecificDefaultSub",
            "()V"
        )
        caller shouldCall SUBINTERFACE_DEFAULT_TEST andThrow NEVER controlFlowDependent false typeDependent false
        caller shouldNotCall SUPERINTERFACE_DEFAULT_TEST
        caller shouldNotCall SUB_IMPL_DEFAULT_TEST

        caller = MethodSignature(
            "Main",
            "mostSpecificDefaultSuper",
            "()V"
        )
        caller shouldCall SUBINTERFACE_DEFAULT_TEST controlFlowDependent false typeDependent false
        caller shouldNotCall SUPERINTERFACE_DEFAULT_TEST
        caller shouldNotCall SUPER_IMPL_DEFAULT_TEST
    }

    "Invoke method which parameter has subclasses" {
        val caller = MethodSignature(
            "Main",
            "makeNoise",
            "(LVehicle;)V"
        )
        val VEHICLE_HONK = MethodSignature(
            "Vehicle",
            "honk",
            "()V"
        )
        val BIKE_HONK = MethodSignature(
            "Bike",
            "honk",
            "()V"
        )
        val CAR_HONK = MethodSignature(
            "Car",
            "honk",
            "()V"
        )
        caller shouldCall VEHICLE_HONK andThrow MAYBE
        caller shouldCall BIKE_HONK andThrow MAYBE
        caller shouldCall CAR_HONK andThrow MAYBE
    }

    "Do not populate subclasses for java/lang/Object" {
        val caller = MethodSignature(
            "Main",
            "makeString",
            "(Ljava/lang/Object;)V"
        )
        callGraph.outgoing shouldHaveKey caller
        val matches = callGraph.outgoing[caller]!!.filter { it.caller.signature == caller }
        matches shouldHaveSize 1
        matches[0].target shouldBe MethodSignature(
            "java/lang/Object",
            "toString",
            "()Ljava/lang/String;"
        )
    }

    "All calls have been checked" {
        val missing = unchecked.values.flatten().filter { !it.target.fqn.startsWith("Ljava/") }
        missing shouldHaveSize 0
    }
})
