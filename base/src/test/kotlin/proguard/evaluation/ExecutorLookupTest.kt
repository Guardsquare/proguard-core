package proguard.evaluation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.MethodDescriptor
import proguard.classfile.MethodSignature
import proguard.evaluation.executor.Executor
import proguard.evaluation.executor.MethodExecutionInfo
import proguard.evaluation.executor.StringReflectionExecutor
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool
import java.util.stream.Collectors

class ExecutorLookupTest : FunSpec({

    val signatures = listOf(
        MethodSignature("A", "a", MethodDescriptor("V()")),
    )

    test("Method lookup works correctly") {
        // prepare dummy test executor
        val testExecutor = object : Executor {
            override fun getMethodResult(
                methodData: MethodExecutionInfo?,
                valueCalculator: ValueCalculator?,
            ): MethodResult {
                throw UnsupportedOperationException("Mocked!")
            }

            override fun getSupportedMethodSignatures(): MutableSet<MethodSignature> {
                return signatures.stream().collect(Collectors.toSet())
            }
        }

        // construct the object to be tested
        val lookup = ExecutorLookup(
            listOf(
                StringReflectionExecutor.Builder(libraryClassPool).build(),
                testExecutor,
            ),
        )

        // this should not be matched
        lookup.hasExecutorFor(
            MethodSignature(
                "dummy",
                "dummy",
                "()V",
            ),
        ) shouldBe false
        lookup.hasExecutorFor(
            MethodSignature(
                "A",
                "a",
                MethodDescriptor("V()"),
            ),
        ) shouldBe true
    }
})
