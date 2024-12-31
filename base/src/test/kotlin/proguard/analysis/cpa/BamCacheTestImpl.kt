package proguard.analysis.cpa

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import proguard.analysis.cpa.bam.BamCacheImpl
import proguard.analysis.cpa.bam.BlockAbstraction
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.defaults.ListAbstractState
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet
import proguard.analysis.cpa.defaults.StackAbstractState
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState
import proguard.classfile.MethodSignature
import proguard.testutils.cpa.IntegerAbstractState
import java.lang.UnsupportedOperationException

class BamCacheTestImpl : FreeSpec({

    "getAllMethods works correctly" - {
        val cache = BamCacheImpl<IntegerAbstractState>()

        val localVariables = ListAbstractState<IntegerAbstractState>()
        val staticVariables = HashMapAbstractState<String, IntegerAbstractState>()
        val heap = JvmForgetfulHeapAbstractState<IntegerAbstractState>(null)

        val mockedLocation = mockk<JvmCfaNode>()

        val stack1 = StackAbstractState<IntegerAbstractState>()
        stack1.push(IntegerAbstractState(42))
        val frame1 = JvmFrameAbstractState(localVariables, stack1)
        val state1 = JvmAbstractState(mockedLocation, frame1, heap, staticVariables)

        val stack2 = StackAbstractState<IntegerAbstractState>()
        stack2.push(IntegerAbstractState(43))
        val frame2 = JvmFrameAbstractState(localVariables, stack2)
        val state2 = JvmAbstractState(mockedLocation, frame2, heap, staticVariables)

        val stack3 = StackAbstractState<IntegerAbstractState>()
        stack3.push(IntegerAbstractState(42))
        val frame3 = JvmFrameAbstractState(localVariables, stack3)
        val state3 = JvmAbstractState(mockedLocation, frame3, heap, staticVariables)

        val signatureA = MethodSignature("a", "test", "()V")
        val signatureB = MethodSignature("b", "test", "()V")

        cache.put(state1, null, signatureA, BlockAbstraction(ProgramLocationDependentReachedSet(), BreadthFirstWaitlist()))
        cache.put(state2, null, signatureA, BlockAbstraction(ProgramLocationDependentReachedSet(), BreadthFirstWaitlist()))
        cache.put(state3, null, signatureB, BlockAbstraction(ProgramLocationDependentReachedSet(), BreadthFirstWaitlist()))

        "Correct set" {
            cache.allMethods shouldBe setOf(signatureA, signatureB)
        }

        "Unmodifiable set" {
            shouldThrow<UnsupportedOperationException> { cache.allMethods.add(signatureA) }
            shouldThrow<UnsupportedOperationException> { cache.allMethods.remove(signatureA) }
        }
    }
})
