package proguard.classfile.editor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import proguard.classfile.ProgramClass
import proguard.classfile.constant.Utf8Constant
import proguard.classfile.util.MemberRenamer
import proguard.classfile.visitor.ClassCleaner
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.RequiresJavaVersion

class ConstantPoolShrinkerTest : BehaviorSpec({
    Given("A class Clazz with a single method test and a single field a") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Clazz.java",
                """
                public class Clazz {
                    private int a;
                    public void test() {
                        System.out.println("Clazz");
                    }
                }
                """.trimIndent(),
            ),
        )
        val clazz = programClassPool.getClass("Clazz") as ProgramClass
        When("the method test name is changed and the constant pool shrinker is executed") {
            clazz.methodAccept("test", "()V", MemberRenamer { _, _ -> "renamed" })
            // check that it's there before
            clazz.constantPool.any { it is Utf8Constant && it.string == "test" } shouldBe true
            clazz.allConstantsDoNotHaveProcessingInfoSet()
            clazz.accept(ConstantPoolShrinker())
            Then("The constant pool shrinker should remove the dangling method name constant") {
                clazz.constantPool.any { it is Utf8Constant && it.string == "test" } shouldBe false
            }
            Then("all constants are marked as used") {
                clazz.allConstantsHaveProcessingInfoSet()
            }
        }

        When("the field a name is changed and the constant pool shrinker is executed") {
            // clean up old info
            clazz.accept(ClassCleaner())
            clazz.fieldAccept("a", "I", MemberRenamer { _, _ -> "renamed" })
            // check that the old name constant is there before
            clazz.constantPool.any { it is Utf8Constant && it.string == "a" } shouldBe true
            clazz.allConstantsDoNotHaveProcessingInfoSet()
            clazz.accept(ConstantPoolShrinker())
            Then("the dangling method name constant should be removed") {
                clazz.constantPool.any { it is Utf8Constant && it.string == "a" } shouldBe false
            }
            Then("all constants are marked as used") {
                clazz.allConstantsHaveProcessingInfoSet()
            }
        }
    }
    Given("a class containing nested classes") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Clazz.java",
                """
                public class Clazz {
                private int a;
                 class InnerClass {}
                 static class NestedClass {}
                }
                """.trimIndent(),
            ),
        )
        val clazz = programClassPool.getClass("Clazz") as ProgramClass

        Then("The constant pool shrinker should mark all constants as used")
        clazz.allConstantsDoNotHaveProcessingInfoSet()
        clazz.accept(ConstantPoolShrinker())
        clazz.allConstantsHaveProcessingInfoSet()
    }
    Given("A class containing lambda expressions and method references") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Main.java",
                """
                import java.util.Arrays;
                import java.util.List;
                
                public class Main {
                 public static void main(String[] args) {
                   List<Integer> l = Arrays.asList(1,2,3,4);
                   l.forEach(System.out::println);
                   l.forEach(n -> System.out.println(n+1));
                 }
                }
                """.trimIndent(),
            ),
        )
        val clazz = programClassPool.getClass("Main") as ProgramClass
        Then("The constant pool shrinker should mark all constants as used")
        clazz.allConstantsDoNotHaveProcessingInfoSet()
        clazz.accept(ConstantPoolShrinker())
        clazz.allConstantsHaveProcessingInfoSet()
    }
})

@RequiresJavaVersion(from = 17)
class Java17ConstantPoolShrinkerTest : BehaviorSpec({
    Given("A record class with a compact constructor") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Rectangle.java",
                """
             record Rectangle(double length, double width) {
                public Rectangle {
                    if (length <= 0 || width <= 0) {
                        throw new java.lang.IllegalArgumentException(
                            String.format("Invalid dimensions: %f, %f", length, width));
                    }
                }
             }
                """.trimIndent(),
            ),
        )
        val clazz = programClassPool.getClass("Rectangle") as ProgramClass
        Then("The constant pool shrinker should mark all constants as used") {
            clazz.allConstantsDoNotHaveProcessingInfoSet()
            clazz.accept(ConstantPoolShrinker())
            clazz.allConstantsHaveProcessingInfoSet()
        }
    }
})

@RequiresJavaVersion(from = 21)
class Java21ConstantPoolShrinkerTest : BehaviorSpec({
    Given("A class containing a pattern matching switch statement referencing enum constants") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                """Main.java""",
                """
                    public class Main {
                    public boolean isInterProcedural(Property... properties) {
                             boolean isInterProcedural = false;
                             for (var property : properties) {
                             switch (property) {
                                 case Scope.LOCAL -> isInterProcedural = false;
                                 case Scope.INTERPROCEDURAL -> isInterProcedural = true;
                                 case null,default -> throw new IllegalArgumentException("Incorrect property: " + property);
                             }
                           }
                        return isInterProcedural;
                      }
                    }
                """.trimIndent(),
            ),
            JavaSource(
                "Scope.java",
                """
                    public enum Scope implements Property {
                    	LOCAL,
                    	INTERPROCEDURAL,
                    }
                """.trimIndent(),
            ),
            JavaSource(
                "Property.java",
                """
                    public interface Property {}
                """.trimIndent(),
            ),
        )
        val clazz = programClassPool.getClass("Main") as ProgramClass
        Then("The constant pool shrinker should mark all constants as used") {
            clazz.allConstantsDoNotHaveProcessingInfoSet()
            clazz.accept(ConstantPoolShrinker())
            clazz.allConstantsHaveProcessingInfoSet()
        }
    }
})

fun ProgramClass.allConstantsHaveProcessingInfoSet() {
    this.constantPool.mapNotNull { it }.map { it.processingInfo }.shouldNotContain(null)
}
fun ProgramClass.allConstantsDoNotHaveProcessingInfoSet() {
    this.constantPool.mapNotNull { it }.map { it.processingInfo }.shouldContainOnly(null)
}
