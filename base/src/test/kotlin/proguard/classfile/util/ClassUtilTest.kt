package proguard.classfile.util

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.util.ClassUtil.externalClassVersion
import proguard.classfile.util.ClassUtil.internalClassVersion

class ClassUtilTest : BehaviorSpec({
    Given("Class file version 19") {
        val version19 = 63 shl 16
        Then("The external version should be 19") {
            externalClassVersion(version19) shouldBe "19"
        }

        And("Given a class file with version 19") {
            val programClass = ClassBuilder(
                version19,
                PUBLIC,
                "Test",
                "java/lang/Object",
            ).programClass
            Then("The internal version should be 19") {
                programClass.u4version shouldBe internalClassVersion("19")
            }
        }
    }

    Given("Class file version 23") {
        val version = 67 shl 16
        Then("The external version should be 23") {
            externalClassVersion(version) shouldBe "23"
        }

        And("Given a class file with version 23") {
            val programClass = ClassBuilder(
                version,
                PUBLIC,
                "Test",
                "java/lang/Object",
            ).programClass
            Then("The internal version should be 23") {
                programClass.u4version shouldBe internalClassVersion("23")
            }
        }
    }

    Given("A valid magic number") {
        Then("No exception should be thrown") {
            shouldNotThrowAny {
                ClassUtil.checkMagicNumber(0xcafebabe.toInt())
            }
        }
    }

    Given("An invalid magic number") {
        Then("An UnsupportedOperationException should be thrown") {
            shouldThrow<UnsupportedOperationException> {
                ClassUtil.checkMagicNumber(0xbeefbabe.toInt())
            }
        }
    }

    Given("An external class name") {
        val someClassName = "java.lang.reflect.Constructor"
        Then("Converting it to an internal class name should replace the package separators") {
            ClassUtil.internalClassName(someClassName) shouldBe "java/lang/reflect/Constructor"
        }
    }

    Given("An internal class name") {
        val someClassName = "java/lang/reflect/Constructor"
        Then("Converting it to an external class name should replace the package separators") {
            ClassUtil.externalClassName(someClassName) shouldBe "java.lang.reflect.Constructor"
        }
    }

    Given("A class signature") {
        val sig = "Lcom/test/MyTest;"
        Then("Converting it to an internal class name should result in a class name") {
            ClassUtil.internalClassNameFromClassSignature(sig) shouldBe "com/test/MyTest"
        }
    }

    Given("A class signature with generics") {
        val sig = "<T:Ljava/lang/Object;>Lcom/test/MyTest<TT;>;"
        Then("Converting it to an internal class name should remove generic type parameters") {
            ClassUtil.internalClassNameFromClassSignature(sig) shouldBe "com/test/MyTest"
        }
    }

    Given("A class signature with generics and an inner class") {
        val sig = "<T:Ljava/lang/Object;>Lcom/test/MyTest<TT;>.Inner;"
        Then("Converting it to an internal class name should remove generic type parameters and replace '.' by '$'") {
            ClassUtil.internalClassNameFromClassSignature(sig) shouldBe "com/test/MyTest\$Inner"
        }
    }
})
