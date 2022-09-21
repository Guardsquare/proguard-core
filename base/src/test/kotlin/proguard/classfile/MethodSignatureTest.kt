package proguard.classfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MethodSignatureTest : FreeSpec({

    "Direct comparison" - {
        val m1 = MethodSignature(
            "java/lang/String",
            "foo",
            "(ILjava/lang/Object;)V"
        )
        val m2 = MethodSignature(
            "java/lang/String",
            "foo",
            MethodDescriptor(
                "V",
                listOf("I", "Ljava/lang/Object;")
            )
        )
        m1 shouldBe m2
        m2 shouldBe m1
        m1.hashCode() shouldBe m2.hashCode()
        MethodSignature.matchesIgnoreNull(m2, m1) shouldBe true
        MethodSignature.matchesIgnoreNull(m1, m2) shouldBe true

        m1.getPrettyFqn() shouldBe m2.getPrettyFqn()
        m2.getPrettyFqn() shouldBe "void String.foo(int,Object)"
    }

    "NULL clazz comparison" - {
        val m1 = MethodSignature(
            "java/lang/String",
            "foo",
            "(ILjava/lang/Object;)V"
        )
        val m2 = MethodSignature(
            null,
            "foo",
            "(ILjava/lang/Object;)V"
        )
        MethodSignature.matchesIgnoreNull(m2, m1) shouldBe false
        MethodSignature.matchesIgnoreNull(m1, m2) shouldBe true
    }

    "NULL method comparison" - {
        val m1 = MethodSignature(
            "java/lang/String",
            "foo",
            "(ILjava/lang/Object;)V"
        )
        val m2 = MethodSignature(
            "java/lang/String",
            null,
            "(ILjava/lang/Object;)V"
        )
        MethodSignature.matchesIgnoreNull(m2, m1) shouldBe false
        MethodSignature.matchesIgnoreNull(m1, m2) shouldBe true
    }

    "NULL descriptor comparison" - {
        val m1 = MethodSignature(
            "java/lang/String",
            "foo",
            "(ILjava/lang/Object;)V"
        )
        val m2 = MethodSignature(
            "java/lang/String",
            "foo",
            null as MethodDescriptor?
        )
        MethodSignature.matchesIgnoreNull(m2, m1) shouldBe false
        MethodSignature.matchesIgnoreNull(m1, m2) shouldBe true
    }
})
