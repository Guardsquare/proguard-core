package proguard.analysis.cpa

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.defaults.ListAbstractState
import proguard.analysis.cpa.defaults.StackAbstractState
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.operators.DefaultReduceOperator
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState
import proguard.analysis.datastructure.callgraph.Call
import proguard.classfile.MethodSignature
import proguard.testutils.cpa.IntegerAbstractState

class DefaultReduceOperatorTest : BehaviorSpec({
    Given("A JVM state") {
        val localVariables = ListAbstractState<IntegerAbstractState>()
        val staticVariables = HashMapAbstractState<String, IntegerAbstractState>()
        val heap = JvmForgetfulHeapAbstractState<IntegerAbstractState>(null)

        val stack = StackAbstractState<IntegerAbstractState>()
        stack.push(IntegerAbstractState(42)) // instance
        stack.push(IntegerAbstractState(43)) // parameter

        val frame = JvmFrameAbstractState(localVariables, stack)
        val mockedCallSite = mockk<JvmCfaNode>()
        val callState = JvmAbstractState(mockedCallSite, frame, heap, staticVariables)

        When("The state is reduced") {
            val testSignature = MethodSignature("Test", "foo", "(I)V")

            val call = mockk<Call>()
            every { call.target } returns testSignature
            every { call.isStatic } returns false
            every { call.jvmArgumentSize } answers { callOriginal() }

            val mockedEntryState = mockk<JvmCfaNode>()
            val reducedState = DefaultReduceOperator<IntegerAbstractState>().reduce(callState, mockedEntryState, call)

            Then("The stack has been correctly moved to the variables") {
                val expectedLocalVariables = ListAbstractState<IntegerAbstractState>()
                expectedLocalVariables.add(0, IntegerAbstractState(42))
                expectedLocalVariables.add(1, IntegerAbstractState(43))

                val expectedStaticVariables = HashMapAbstractState<String, IntegerAbstractState>()
                val expectedHeap = JvmForgetfulHeapAbstractState<IntegerAbstractState>(null)
                val expectedStack = StackAbstractState<IntegerAbstractState>()
                val expectedFrame = JvmFrameAbstractState(expectedLocalVariables, expectedStack)
                val expectedReducedState = JvmAbstractState(mockedEntryState, expectedFrame, expectedHeap, expectedStaticVariables)

                reducedState shouldBe expectedReducedState
            }
        }
    }
})
