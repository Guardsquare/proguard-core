package proguard.evaluation

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.MethodDescriptor
import proguard.classfile.MethodSignature
import proguard.evaluation.executor.Executor
import proguard.evaluation.executor.MethodExecutionInfo
import proguard.evaluation.executor.StringReflectionExecutor
import java.util.stream.Collectors

class ExecutorLookupTest : FunSpec({

    val signatures = listOf(
        MethodSignature("C", null, null as String?),
        MethodSignature("C", "a", null as String?),
        MethodSignature("C", "a", MethodDescriptor("V()")),
        MethodSignature("B", null, null as String?),
        MethodSignature("B", "a", null as String?),
        MethodSignature("B", "a", MethodDescriptor("V()")),
        MethodSignature("A", null, null as String?),
        MethodSignature("A", "a", null as String?),
        MethodSignature("A", "a", MethodDescriptor("V()")),
        MethodSignature(null, "a", null as String?),
        MethodSignature(null, "a", MethodDescriptor("V()")),
        MethodSignature("D", "a", null as String?),

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

            override fun getSupportedMethodSignatures(): MutableList<MethodSignature> {
                return signatures.stream().collect(Collectors.toList())
            }
        }

        // construct the object to be tested
        val lookup = ExecutorLookup(
            listOf(
                StringReflectionExecutor.Builder().build(),
                testExecutor,
            ),
        )

        // this should not be matched
        lookup.hasExecutorFor(
            MethodSignature(
                "dummy",
                "dummy",
                null as String?,
            ),
        ) shouldBe false
        lookup.hasExecutorFor(MethodSignature("D", "b", null as String?)) shouldBe false

        // this should be found
        lookup.hasExecutorFor(MethodSignature("A", "dummy", null as String?)) shouldBe true
        lookup.hasExecutorFor(
            MethodSignature(
                "D",
                "a",
                MethodDescriptor("V()"),
            ),
        ) shouldBe true
        lookup.hasExecutorFor(
            MethodSignature(
                "A",
                null,
                MethodDescriptor("V()"),
            ),
        ) shouldBe true
    }
})
