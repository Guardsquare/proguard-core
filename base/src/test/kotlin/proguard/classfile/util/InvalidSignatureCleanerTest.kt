package proguard.classfile.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.attribute.signature.grammars.ClassSignatureGrammar
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class InvalidSignatureCleanerTest : BehaviorSpec({
    Given("A program class") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyClass.java",
                """
                import java.io.Serializable;
                
                class MyClass<T> implements java.io.Serializable {}
                """.trimIndent(),
            ),
        )

        val myClass = programClassPool.getClass("MyClass")

        When("Comparing a valid signature") {
            val signature = ClassSignatureGrammar.parse("<T::Ljava/lang/Object;>Ljava/lang/Object;Ljava/io/Serializable;")

            Then("The signature should be considered valid") {
                InvalidSignatureCleaner.isValidClassSignature(myClass, signature) shouldBe true
            }
        }

        When("Comparing an invalid signature due to an incorrect superclass") {
            val signature = ClassSignatureGrammar.parse("<T::Ljava/lang/Object;>Ljava/lang/String;Ljava/io/Serializable;")

            Then("The signature should be considered invalid") {
                InvalidSignatureCleaner.isValidClassSignature(myClass, signature) shouldBe false
            }
        }

        When("Comparing an invalid signature due to an incorrect number of interfaces") {
            val signature = ClassSignatureGrammar.parse("<T::Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Object;Ljava/io/Serializable;")

            Then("The signature should be considered invalid") {
                InvalidSignatureCleaner.isValidClassSignature(myClass, signature) shouldBe false
            }
        }

        When("Comparing an invalid signature due to an incorrect interface type") {
            val signature = ClassSignatureGrammar.parse("<D::Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Object;")

            Then("The signature should be considered invalid") {
                InvalidSignatureCleaner.isValidClassSignature(myClass, signature) shouldBe false
            }
        }
    }
})
