package proguard.classfile.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.ClassPool
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class ClassPoolClassLoaderTest : FreeSpec({
    "Given a ProGuard ProgramClass" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public class Test {
                    public static String test() {
                        return "Hello World";
                    }
                }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        )

        "When loaded with the ClassPoolClassLoader" - {
            val clazz = ClassPoolClassLoader(programClassPool).loadClass("Test")
            "Then the main method can be executed" {
                clazz
                    .declaredMethods
                    .single { it.name == "test" }
                    .invoke(null) shouldBe "Hello World"
            }
        }
    }

    "Given a class not in the class pool" - {
        "When calling findClass with the ClassPoolClassLoader" - {
            val emptyClassPool = ClassPool()
            "Then the class loader should throw a ClassNotFoundException" {
                shouldThrow<ClassNotFoundException> {
                    ClassPoolClassLoader(emptyClassPool).findClass("Test")
                }
            }
        }
    }
})
