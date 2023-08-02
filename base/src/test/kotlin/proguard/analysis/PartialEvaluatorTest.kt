package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.classfile.AccessConstants
import proguard.classfile.attribute.Attribute.CODE
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.util.jsonprinter.JsonPrinter
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.TypedReferenceValueFactory
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.findMethod

class PartialEvaluatorTest : FreeSpec({
    "Test lifecycle" - {
        "With branches" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val elseLabel = it.createLabel()
                    it
                        .iconst(50)
                        .label(startLabel)
                        .dup()
                        .iconst_5()
                        .ificmple(elseLabel)
                        .iconst_5()
                        .isub()
                        .goto_(startLabel)
                        .label(elseLabel)
                        .ireturn()
                }
                .programClass

            val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
            val tracker = JsonPrinter()
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setInvocationUnit(ExecutingInvocationUnit.Builder().build(valueFactory))
                .setEvaluateAllCode(true).setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()I",
            )
        }

        "With method filter" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test0", "()I", 50) {
                    it
                        .iconst_0()
                        .ireturn()
                }
                .addMethod(AccessConstants.PUBLIC, "test1", "()I", 50) {
                    it
                        .iconst_1()
                        .ireturn()
                }
                .programClass

            val valueFactory = ParticularValueFactory()
            val tracker = JsonPrinter(
                programClass.superClass,
                programClass.findMethod("test0"),
            )
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test0",
                "()I",
            )
            evaluateProgramClass(
                programClass,
                pe,
                "test1",
                "()I",
            )
        }

        "2 methods" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test0", "()I", 50) {
                    it
                        .iconst_0()
                        .ireturn()
                }
                .addMethod(AccessConstants.PUBLIC, "test0", "()V", 50) {
                    it
                        .iconst_1()
                        .return_()
                }
                .programClass

            val valueFactory = ParticularValueFactory()
            val tracker = JsonPrinter()
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test0",
                "()I",
            )
            evaluateProgramClass(
                programClass,
                pe,
                "test0",
                "()V",
            )
        }

        "simple throw and catch" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val midLabel = it.createLabel()
                    val endLabel = it.createLabel()
                    it
                        .label(startLabel)
                        .aload_0()
                        .label(midLabel)
                        .athrow()
                        .label(endLabel)
                        // .catch_(startLabel, endLabel, "appel", null)
                        .catchAll(startLabel, endLabel)
                        .iconst_1()
                        .ireturn()
                }
                .programClass

            val tracker = JsonPrinter()
            val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setInvocationUnit(ExecutingInvocationUnit.Builder().build(valueFactory))
                .setEvaluateAllCode(true).setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()I",
            )
        }

        "simple catch, no throw" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val endLabel = it.createLabel()
                    it
                        .label(startLabel)
                        .iconst_2()
                        .ireturn()
                        .label(endLabel)
                        .catchAll(startLabel, endLabel)
                        .iconst_1()
                        .ireturn()
                }
                .programClass

            val tracker = JsonPrinter()
            val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setInvocationUnit(ExecutingInvocationUnit.Builder().build(valueFactory))
                .setEvaluateAllCode(false).setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()I",
            )
        }

        "Complete" {
            val build = buildClass()
                .addMethod(AccessConstants.PRIVATE or AccessConstants.STATIC, "initializer", "()I", 50) {
                    it.iconst(50).ireturn()
                }
            val programClass = build
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val elseLabel = it.createLabel()
                    val loadLabel = it.createLabel()
                    val endLabel = it.createLabel()
                    it
                        .invokestatic(
                            "PartialEvaluatorDummy",
                            "initializer",
                            "()I",
                            it.targetClass,
                            it.targetClass.findMethod("initializer"),
                        )
                        .label(startLabel)
                        .dup()
                        .iconst_5()
                        .ificmple(elseLabel)
                        .iconst_5()
                        .isub()
                        .goto_(startLabel)
                        .label(elseLabel)
                        .jsr(loadLabel)
                        .athrow()
                        .label(loadLabel)
                        .astore_1()
                        .aload_0()
                        .ret(1)
                        .label(endLabel)
                        .catchAll(startLabel, endLabel)
                        .iconst_5()
                        .ireturn()
                        .catchAll(startLabel, elseLabel)
                        .iconst_1()
                        .ireturn()
                }
                .programClass

            val tracker = JsonPrinter()
            val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
            val pe = PartialEvaluator.Builder.create()
                .setValueFactory(valueFactory)
                .setInvocationUnit(ExecutingInvocationUnit.Builder().build(valueFactory))
                .setEvaluateAllCode(false).setStateTracker(tracker).build()
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()I",
            )
        }
    }

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
                """.trimIndent(),
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
                """.trimIndent(),
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
                """.trimIndent(),
            ),
            JavaSource(
                "StringBuilderBranchClass.java",
                """
                public class StringBuilderBranchClass {
                    public String foo() {
                        StringBuilder sb = new StringBuilder();
                        if (System.currentTimeMillis() > 0) {
                            sb.append("x");
                        } else {
                            sb.append("y");
                        }
                        return sb.toString();
                    }
                }
                """.trimIndent(),
            ),
            // Target Java 8 only to ensure consistent bytecode sequences.
            javacArguments = listOf("-source", "8", "-target", "8"),
        )

        val evaluateAllCode = true
        val maxPartialEvaluations = 50
        val particularValueFactory = ParticularValueFactory(
            ArrayReferenceValueFactory(),
            ParticularReferenceValueFactory(),
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
                    "foo",
                    "()Ljava/lang/String;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "foo",
                    "()LFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "foo",
                    "()LFinalFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "bar",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "bar",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "bar",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "baz",
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "baz",
                    "(LFoo;)LFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "baz",
                    "(LFinalFoo;)LFinalFoo;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "exception",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
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
                    "exception",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
            )
            // The stack in the catch block should contain a FinalFooException which is a final class
            val value = particularValueEvaluator
                .getStackAfter(15)
                .getTop(0) as TypedReferenceValue

            value.mayBeExtension() shouldBe false
        }

        "StringBuilderBranchClass" {
            programClassPool.classesAccept(
                "StringBuilderBranchClass",
                NamedMethodVisitor(
                    "foo",
                    "()Ljava/lang/String;",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
            )
            val stackTopAfterStringBuilderInit = particularValueEvaluator
                .getStackAfter(0)
                .getTop(0) as IdentifiedReferenceValue
            val stackTopAfterGeneralize = particularValueEvaluator
                .getStackAfter(33)
                .getTop(0)

            // The instance should be tracked from the creation to the last usage.
            stackTopAfterGeneralize.shouldBeInstanceOf<IdentifiedReferenceValue>()
            stackTopAfterGeneralize.id shouldBe stackTopAfterStringBuilderInit.id
        }
    }

    "ParticularValueFactory should delegate to enclosed reference value factory" {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Test.jbc",
                """
            version 1.8;
            public class Test extends java.lang.Object {
                public static void a()
                {
                    aconst_null
                    astore_0
                    return
                }
            }
                """.trimIndent(),
            ),
        )

        val typedReferenceValueFactory = TypedReferenceValueFactory()
        val particularValueFactory = ParticularValueFactory(
            ArrayReferenceValueFactory(),
            typedReferenceValueFactory,
        )
        val particularValueInvocationUnit = BasicInvocationUnit(particularValueFactory)
        val particularValueEvaluator = PartialEvaluator.Builder.create()
            .setValueFactory(particularValueFactory)
            .setInvocationUnit(particularValueInvocationUnit)
            .build()

        programClassPool.classesAccept(
            "Test",
            NamedMethodVisitor(
                "a",
                "()V",
                AllAttributeVisitor(
                    AttributeNameFilter(CODE, particularValueEvaluator),
                ),
            ),
        )

        val variablesAfterAconstNull = particularValueEvaluator.getVariablesAfter(1)
        val value = variablesAfterAconstNull.getValue(0)
        value shouldBe typedReferenceValueFactory.createReferenceValueNull()
    }
})
