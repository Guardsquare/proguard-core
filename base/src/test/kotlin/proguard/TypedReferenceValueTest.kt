package proguard

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.testutils.ClassPoolBuilder

class TypedReferenceValueTest : StringSpec({

    "Regression test: generalizing two types into parent should set the `mayBeExtension` flag" {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
        val a = valueFactory.createReferenceValue(
            "Ljava/lang/StringBuilder;",
            ClassPoolBuilder.libraryClassPool.getClass("java/lang/StringBuilder"),
            false,
            true
        )
        val b = valueFactory.createReferenceValue(
            "Ljava/lang/String;",
            ClassPoolBuilder.libraryClassPool.getClass("java/lang/String"),
            false,
            true
        )

        a.generalize(b) shouldBe TypedReferenceValue(
            "Ljava/io/Serializable;",
            ClassPoolBuilder.libraryClassPool.getClass("java/io/Serializable"),
            true,
            true
        )
    }
})
