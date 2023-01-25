package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.classfile.MethodSignature

class SourceSinkTest : FreeSpec({
    "Taint sources" - {
        val source1 = JvmTaintSource(
            MethodSignature("Test", "source1", "(Ljava/lang/String;II)Ljava/lang/String;"),
            true,
            true,
            setOf(1, 3),
            setOf("Test.field", "Test.other")
        )
        val source1Copy = JvmTaintSource(
            MethodSignature("Test", "source1", "(Ljava/lang/String;II)Ljava/lang/String;"),
            true,
            true,
            setOf(1, 3),
            setOf("Test.field", "Test.other")
        )
        val source2 = JvmTaintSource(
            MethodSignature("Test", "source2", "()Ljava/lang/String;"),
            false,
            true,
            emptySet(),
            emptySet()
        )

        "Should be comparable" {
            source1 shouldBe source1Copy
            source1 shouldNotBe source2
        }

        "Should have an informative String representation" {
            source1.toString() shouldBe "[TaintSource] LTest;source1(Ljava/lang/String;II)Ljava/lang/String;, taints this, taints return, taints args (1, 3), taints globals (Test.field, Test.other)"
            source2.toString() shouldBe "[TaintSource] LTest;source2()Ljava/lang/String;, taints return"
        }
    }

    "Taint sinks" - {
        val sink1 = JvmInvokeTaintSink(
            MethodSignature("Test", "sink1", "(Ljava/lang/String;II)V"),
            true,
            setOf(1, 3),
            setOf("Test.field", "Test.other")
        )
        val sink1Copy = JvmInvokeTaintSink(
            MethodSignature("Test", "sink1", "(Ljava/lang/String;II)V"),
            true,
            setOf(1, 3),
            setOf("Test.field", "Test.other")
        )
        val sink2 = JvmInvokeTaintSink(
            MethodSignature("Test", "sink2", "(Ljava/lang/String;)V"),
            false,
            setOf(1),
            emptySet()
        )

        "Should be comparable" {
            sink1 shouldBe sink1Copy
            sink1.hashCode() shouldBe sink1Copy.hashCode()

            sink1 shouldNotBe sink2
        }

        "Should have an informative String representation" {
            sink1.toString() shouldBe "[JvmInvokeTaintSink] LTest;sink1(Ljava/lang/String;II)V, takes instance, takes args (1, 3), takes globals (Test.field, Test.other)"
            sink2.toString() shouldBe "[JvmInvokeTaintSink] LTest;sink2(Ljava/lang/String;)V, takes args (1)"
        }
    }
})
