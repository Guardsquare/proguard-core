package proguard.classfile.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.constant.Constant
import proguard.classfile.constant.StringConstant
import proguard.classfile.constant.visitor.AllConstantVisitor
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class DynamicClassReferenceInitializerTest : BehaviorSpec({

    given("A class that reflectively calls another class using Class.forName(String)") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public class Test {
                    public static void main(String[] args) {
                        try {
                            Class.forName("Bar").getDeclaredMethod("foo").invoke(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                class Bar {
                    public static void foo() {
                        System.out.println("foo");
                    }
                }
                """.trimIndent()
            ),
        )

        // Initialize the subclass hierarchies (we ignore the library class pool as it's not used).
        val classSubHierarchyInitializer = ClassSubHierarchyInitializer()
        programClassPool.accept(classSubHierarchyInitializer)

        `when`("Initializing the constant references to classes in the program class pool") {
            // Initialize reflective class references.
            programClassPool.classesAccept(
                DynamicClassReferenceInitializer(programClassPool, libraryClassPool, null, null, null, null)
            )

            then("The reference of the string constant referring to the program class is correctly initialized") {
                val constantVisitor = mockk<ConstantVisitor>()
                every { constantVisitor.visitStringConstant(any(), any()) } returns Unit

                programClassPool.classesAccept(
                    "Test",
                    AllConstantVisitor(object : ConstantVisitor {
                        override fun visitAnyConstant(clazz: Clazz, constant: Constant) {
                        }

                        override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                            if (stringConstant.getString(clazz) contentEquals "Bar") {
                                stringConstant.accept(clazz, constantVisitor)
                                stringConstant.referencedClass shouldBe programClassPool.getClass("Bar")
                            }
                        }
                    })
                )

                verify(exactly = 1) { constantVisitor.visitStringConstant(any(), any()) }
            }
        }
    }

    given("A class that reflectively calls another class using Class.forName(String, boolean, ClassLoader)") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                public class Test {
                    public static void main(String[] args) {
                        try {
                            Class.forName("Bar", true, Bar.class.getClassLoader()).getDeclaredMethod("foo").invoke(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                class Bar {
                    public static void foo() {
                        System.out.println("foo");
                    }
                }
                """.trimIndent()
            ),
        )

        // Initialize the subclass hierarchies (we ignore the library class pool as it's not used).
        val classSubHierarchyInitializer = ClassSubHierarchyInitializer()
        programClassPool.accept(classSubHierarchyInitializer)

        `when`("Initializing the constant references to classes in the program class pool") {
            // Initialize reflective class references.
            programClassPool.classesAccept(
                DynamicClassReferenceInitializer(programClassPool, libraryClassPool, null, null, null, null)
            )

            then("The reference of the string constant referring to the program class is correctly initialized") {
                val constantVisitor = mockk<ConstantVisitor>()
                every { constantVisitor.visitStringConstant(any(), any()) } returns Unit

                programClassPool.classesAccept(
                    "Test",
                    AllConstantVisitor(object : ConstantVisitor {
                        override fun visitAnyConstant(clazz: Clazz, constant: Constant) {
                        }

                        override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                            if (stringConstant.getString(clazz) contentEquals "Bar") {
                                stringConstant.accept(clazz, constantVisitor)
                                stringConstant.referencedClass shouldBe programClassPool.getClass("Bar")
                            }
                        }
                    })
                )

                verify(exactly = 1) { constantVisitor.visitStringConstant(any(), any()) }
            }
        }
    }
})
