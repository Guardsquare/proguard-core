package proguard.analysis.cpa

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.bam.BamCacheImpl
import proguard.analysis.cpa.bam.BlockAbstraction
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist
import proguard.analysis.cpa.defaults.DefaultReachedSet
import proguard.classfile.MethodSignature
import proguard.testutils.cpa.IntegerAbstractState
import java.lang.UnsupportedOperationException

class BamCacheTestImpl : FreeSpec({

    "getAllMethods works correctly" - {
        val cache = BamCacheImpl<MethodSignature>()

        val signatureA = MethodSignature("a", "test", "()V")
        val signatureB = MethodSignature("b", "test", "()V")

        cache.put(IntegerAbstractState(42), null, signatureA, BlockAbstraction(DefaultReachedSet(), BreadthFirstWaitlist()))
        cache.put(IntegerAbstractState(43), null, signatureA, BlockAbstraction(DefaultReachedSet(), BreadthFirstWaitlist()))
        cache.put(IntegerAbstractState(42), null, signatureB, BlockAbstraction(DefaultReachedSet(), BreadthFirstWaitlist()))

        "Correct set" {
            cache.allMethods shouldBe setOf(signatureA, signatureB)
        }

        "Unmodifiable set" {
            shouldThrow<UnsupportedOperationException> { cache.allMethods.add(signatureA) }
            shouldThrow<UnsupportedOperationException> { cache.allMethods.remove(signatureA) }
        }
    }
})
