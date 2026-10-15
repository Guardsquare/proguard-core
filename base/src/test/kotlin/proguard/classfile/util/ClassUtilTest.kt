package proguard.classfile.util

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.JavaTypeConstants
import proguard.classfile.TypeConstants
import proguard.classfile.VersionConstants
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

    Given("Preview class file version 23") {
        val version = 67 shl 16 or 65535
        Then("The external version should be 23") {
            externalClassVersion(version) shouldBe "23"
        }
    }

    Given("Max class file version") {
        val version = VersionConstants.MAX_SUPPORTED_VERSION
        Then("The external version should not be null") {
            externalClassVersion(version) shouldNotBe null
        }
    }

    Given("Unrecognized too low class file version") {
        val version = 20 shl 16 // minimum is 45
        Then("The external version should be null") {
            externalClassVersion(version) shouldBe null
        }

        Then("checkVersionNumbers should throw") {
            val exception = shouldThrow<UnsupportedOperationException> {
                ClassUtil.checkVersionNumbers(version)
            }
            exception shouldHaveMessage Regex("""Unsupported version number \[20\.0] \(maximum \d+.\d+, Java \d+\)""")
        }
    }

    Given("Unrecognized too high class file version") {
        val version = 65535 shl 16
        Then("The external version should be null") {
            externalClassVersion(version) shouldBe null
        }

        Then("checkVersionNumbers should throw") {
            val exception = shouldThrow<UnsupportedOperationException> {
                ClassUtil.checkVersionNumbers(version)
            }
            exception shouldHaveMessage Regex("""Unsupported version number \[65535\.0] \(maximum \d+.\d+, Java \d+\)""")
        }
    }

    Given("Unrecognized external version") {
        val version = "0.1"
        Then("The internal version should be 0") {
            internalClassVersion(version) shouldBe 0
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

    Given("An internal type with spaces") {
        val someType = "Lcom/example/Sample Class;"
        Then("Converting it to an external type with quoted true should add quotes") {
            ClassUtil.externalType(someType, true) shouldBe "'com.example.Sample Class'"
        }
        Then("Converting it to an external type with quoted false should not add quotes") {
            ClassUtil.externalType(someType, false) shouldBe "com.example.Sample Class"
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

    Given("An internal class name with spaces") {
        val someClassName = "com/example/Sample Class"
        Then("Converting it to an external class name with quoted true should add quotes") {
            ClassUtil.externalClassName(someClassName, true) shouldBe "'com.example.Sample Class'"
        }
        Then("Converting it to an external class name with quoted false should not add quotes") {
            ClassUtil.externalClassName(someClassName, false) shouldBe "com.example.Sample Class"
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

    Given("Java primitive types") {
        Then("The right internal primitive type should be returned") {
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.BOOLEAN) shouldBe TypeConstants.BOOLEAN.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.BYTE) shouldBe TypeConstants.BYTE.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.CHAR) shouldBe TypeConstants.CHAR.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.SHORT) shouldBe TypeConstants.SHORT.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.INT) shouldBe TypeConstants.INT.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.FLOAT) shouldBe TypeConstants.FLOAT.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.LONG) shouldBe TypeConstants.LONG.toString()
            ClassUtil.internalTypeFromClassName(JavaTypeConstants.DOUBLE) shouldBe TypeConstants.DOUBLE.toString()
        }
    }
    Given("An unquoted identifier") {
        val identifier = "method name"
        Then("It should be quoted") {
            ClassUtil.quotedIdentifier(identifier) shouldBe "'method name'"
        }
    }
    Given("An unquoted identifier containing escape chars") {
        val identifier = "method \\name"
        Then("It should be escaped when quoted") {
            ClassUtil.quotedIdentifier(identifier) shouldBe "'method \\\\name'"
        }
    }
    Given("An identifier containing quotes") {
        val identifier = "met'hod name"
        val identifier2 = "'method' name"
        val identifier3 = "method name'"
        val identifier4 = "'method name'"
        Then("It should be escaped when quoted") {
            ClassUtil.quotedIdentifier(identifier) shouldBe "'met\\'hod name'"
            ClassUtil.quotedIdentifier(identifier2) shouldBe "'\\'method\\' name'"
            ClassUtil.quotedIdentifier(identifier3) shouldBe "'method name\\''"
            ClassUtil.quotedIdentifier(identifier4) shouldBe "'\\'method name\\''"
        }
    }

    Given("An internal field description") {
        val internalFieldName = "my field"
        val internalFieldDesc = "Lcom/sample/Some Class;"
        Then("Converting it to an external description with quoted false should not add quotes") {
            ClassUtil.externalFullFieldDescription(PUBLIC, internalFieldName, internalFieldDesc, false) shouldBe "public com.sample.Some Class my field"
        }
        Then("Converting it to an external description with quoted true should add quotes") {
            ClassUtil.externalFullFieldDescription(PUBLIC, internalFieldName, internalFieldDesc, true) shouldBe "public 'com.sample.Some Class' 'my field'"
        }
    }

    Given("An internal method descriptor") {
        val internalClassName = "Lcom/sample/Some Class;"
        val internalMethodName = "my method"
        val internalMethodDescriptor = "(Lcom/sample/Some Type;I)Lcom/sample/Other Type;"
        Then("Converting it to an external description with quoted false should not add quotes") {
            ClassUtil.externalFullMethodDescription(internalClassName, PUBLIC, internalMethodName, internalMethodDescriptor, false) shouldBe "public com.sample.Other Type my method(com.sample.Some Type,int)"
        }
        Then("Converting it to an external description with quoted true should add quotes") {
            ClassUtil.externalFullMethodDescription(internalClassName, PUBLIC, internalMethodName, internalMethodDescriptor, true) shouldBe "public 'com.sample.Other Type' 'my method'('com.sample.Some Type',int)"
        }
    }

    Given("An internal method arguments descriptor") {
        val internalMethodDescriptor = "(Lcom/sample/Some Type;Ljava/lang/String;)V"
        Then("Converting it to an external method arguments description with quoted false should not add quotes") {
            ClassUtil.externalMethodArguments(internalMethodDescriptor, false) shouldBe "com.sample.Some Type,java.lang.String"
        }
        Then("Converting it to an external method arguments description with quoted true should add quotes") {
            ClassUtil.externalMethodArguments(internalMethodDescriptor, true) shouldBe "'com.sample.Some Type',java.lang.String"
        }
    }

    Given("An empty internal method arguments descriptor") {
        val internalMethodDescriptor = "()V"
        Then("Converting it to an external method arguments description should return empty") {
            ClassUtil.externalMethodArguments(internalMethodDescriptor, false) shouldBe ""
            ClassUtil.externalMethodArguments(internalMethodDescriptor, true) shouldBe ""
        }
    }

    Given("An any-arguments internal method arguments descriptor") {
        val internalMethodDescriptor = "(...)V"
        Then("Converting it to an external method arguments description should return any-arguments") {
            ClassUtil.externalMethodArguments(internalMethodDescriptor, false) shouldBe "..."
            ClassUtil.externalMethodArguments(internalMethodDescriptor, true) shouldBe "..."
        }
    }

    Given("Class names starting with a char representing a primitive") {
        Then("It should not be considered a primitive") {
            checkAll(listOf('Z', 'B', 'S', 'C', 'I', 'F', 'D', 'J').exhaustive(), Arb.string(minSize = 1, maxSize = 20)) { prefix, name ->
                val className = "$prefix$name"
                ClassUtil.isInternalPrimitiveType(className) shouldBe false
            }
        }
    }
})
