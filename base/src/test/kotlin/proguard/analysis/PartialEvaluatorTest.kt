package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.attribute.Attribute.CODE
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ParticularValueFactory.ReferenceValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class PartialEvaluatorTest : FreeSpec({
    "Test partial evaluation computing mayBeExtension correctly" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "FinalFieldStringClass.java",
                """
                public class FinalFieldStringClass {
                    private String myString;
                
                    public String foo() {
                        return this.myString; 
                    }
                    public void bar() {
                        foo();
                    }
                    
                    public String baz(String myString) {
                        return myString;
                    }
                }
                """.trimIndent()
            ),
            JavaSource(
                "NonFinalFieldClass.java",
                """
                public class NonFinalFieldClass {
                    private Foo myFoo;
                
                    public Foo foo() {
                        return this.myFoo; 
                    }
                    public void bar() {
                        foo();
                    }
                    
                    public Foo baz(Foo myFoo) {
                        return myFoo;
                    }
                    
                    public void exception() {
                        try {
                            System.out.println("Test");
                        } catch (FooException e) {
                            System.out.println(e); 
                        }
                    }
                }
                
                class Foo { }
                class FooException extends RuntimeException { }
                """.trimIndent()
            ),
            JavaSource(
                "FinalFieldClass.java",
                """
                public class FinalFieldClass {
                    private FinalFoo myFoo;
                
                    public FinalFoo foo() {
                        return this.myFoo; 
                    }

                    public void bar() {
                        foo();
                    }
                    
                    public FinalFoo baz(FinalFoo myFoo) {
                        return myFoo;
                    }
                    
                    public void exception() {
                        try {
                            System.out.println("Test");
                        } catch (FinalFooException e) {
                            System.out.println(e); 
                        }
                    }
                }
                
                final class FinalFoo { }
                final class FinalFooException extends RuntimeException { }
                """.trimIndent()
            ),
            // Target Java 8 only to ensure consistent bytecode sequences.
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val evaluateAllCode = true
        val maxPartialEvaluations = 50
        val particularValueFactory = ParticularValueFactory(
            ArrayReferenceValueFactory(),
            ReferenceValueFactory()
        )
        val particularValueInvocationUnit = BasicInvocationUnit(particularValueFactory)
        val particularValueEvaluator = PartialEvaluator.Builder.create()
            .setValueFactory(particularValueFactory)
            .setInvocationUnit(particularValueInvocationUnit)
            .setEvaluateAllCode(evaluateAllCode)
            .stopAnalysisAfterNEvaluations(maxPartialEvaluations)
            .build()

        "Field with a String type" {

            programClassPool.classesAccept(
                "FinalFieldStringClass",
                NamedMethodVisitor(
                    "foo", "()Ljava/lang/String;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after getfield should contain a String which is a final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Field with a non final type" {
            programClassPool.classesAccept(
                "NonFinalFieldClass",
                NamedMethodVisitor(
                    "foo", "()LFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after getfield should contain a Foo which is not a final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe true
        }

        "Field with a final type" {
            programClassPool.classesAccept(
                "FinalFieldClass",
                NamedMethodVisitor(
                    "foo", "()LFinalFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after getfield should contain a FinalFoo which is a final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Method return value with a final String type" {
            programClassPool.classesAccept(
                "FinalFieldStringClass",
                NamedMethodVisitor(
                    "bar", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after foo() should contain a String which is a final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Method return value with a non-final type" {
            programClassPool.classesAccept(
                "NonFinalFieldClass",
                NamedMethodVisitor(
                    "bar", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after foo() should contain a Foo which is a non-final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe true
        }

        "Method return value with a final type" {
            programClassPool.classesAccept(
                "FinalFieldClass",
                NamedMethodVisitor(
                    "bar", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after foo() should contain a FinalFoo which is a final class
            val value = particularValueEvaluator
                .getStackAfter(1)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Method parameter value with a final String type" {
            programClassPool.classesAccept(
                "FinalFieldStringClass",
                NamedMethodVisitor(
                    "baz", "(Ljava/lang/String;)Ljava/lang/String;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after load parameter should contain a String which is a final class
            val value = particularValueEvaluator
                .getStackAfter(0)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Method parameter value with a non-final type" {
            programClassPool.classesAccept(
                "NonFinalFieldClass",
                NamedMethodVisitor(
                    "baz", "(LFoo;)LFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after load parameter should contain a Foo which is a non-final class
            val value = particularValueEvaluator
                .getStackAfter(0)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe true
        }

        "Method parameter value with a final type" {
            programClassPool.classesAccept(
                "FinalFieldClass",
                NamedMethodVisitor(
                    "baz", "(LFinalFoo;)LFinalFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack after load parameter should contain a FinalFoo which is a final class
            val value = particularValueEvaluator
                .getStackAfter(0)
                .getBottom(0) as IdentifiedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "Exception with non-final type" {
            programClassPool.classesAccept(
                "NonFinalFieldClass",
                NamedMethodVisitor(
                    "exception", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack in the catch block should contain a FooException which is a non-final class
            val value = particularValueEvaluator
                .getStackAfter(15)
                .getTop(0) as TypedReferenceValue

            value.mayBeExtension() shouldBe true
        }

        "Exception with final type" {
            programClassPool.classesAccept(
                "FinalFieldClass",
                NamedMethodVisitor(
                    "exception", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator)
                    )
                )
            )
            // The stack in the catch block should contain a FinalFooException which is a final class
            val value = particularValueEvaluator
                .getStackAfter(15)
                .getTop(0) as TypedReferenceValue

            value.mayBeExtension() shouldBe false
        }
    }
})
