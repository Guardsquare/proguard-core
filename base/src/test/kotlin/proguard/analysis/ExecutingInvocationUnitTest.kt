package proguard.analysis

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.classfile.ClassConstants
import proguard.classfile.MethodSignature
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.MethodResult
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.ValueCalculator
import proguard.evaluation.executor.Executor
import proguard.evaluation.executor.MethodExecutionInfo
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class ExecutingInvocationUnitTest : FreeSpec({

    "Expected reference ids" - {

        val code = JavaSource(
            "Test.java",
            """
            public class Test {
            
                public Test(String a, String b) {
                    super();
                }

                public void test() {
                    Test.Builder x = new Test.Builder();
                    Test.Builder y = x.setA("");
                    Test.Builder z = y.setB("");
                    z.build();
                }
                
                public class Builder {
                    
                    String a = "";
                    String b = "";
                    
                    public Test build() {
                        return new Test(a, b);
                    }
                    
                    public Builder setA(String a) {
                        this.a = a;
                        return this;
                    }
                    
                    public Builder setB(String b) {
                        this.b = b;
                        return this;
                    }
                }
            }
            """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

        "Approximate id method returns same type as instance" - {
            val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false,
            )

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val x = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["x"]!!)
            val y = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["y"]!!)
            val z = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["z"]!!)

            "Builder methods always return same identifier" {
                x.shouldBeInstanceOf<IdentifiedReferenceValue>()
                y.shouldBeInstanceOf<IdentifiedReferenceValue>()
                z.shouldBeInstanceOf<IdentifiedReferenceValue>()
                x.id shouldBe y.id
                y.id shouldBe z.id
            }
        }

        "No approximation" - {
            val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(false).build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false,
            )

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val x = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["x"]!!)
            val y = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["y"]!!)
            val z = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["z"]!!)

            "Method calls always return object with new identifier" {
                x.shouldBeInstanceOf<IdentifiedReferenceValue>()
                y.shouldBeInstanceOf<IdentifiedReferenceValue>()
                z.shouldBeInstanceOf<IdentifiedReferenceValue>()
                x.id shouldNotBe y.id
                y.id shouldNotBe z.id
            }
        }
    }

    "Arrays with value null" - {
        val code = JavaSource(
            "Test.java",
            """
                    import java.util.Random;
                    public class Test {
                    
                        public Test(String a, String b) {
                            super();
                        }

                        public void test1() {
                            byte[] array = null;
                            String s = null;
                            try {
                                s = new String(array, "UTF-8");
                            }
                            catch (Exception e){}
                        }
                        
                        public void test2() {
                            byte[] array = new byte[]{0 , 1};
                            array = null;
                            String s = null;
                            try {
                                s = new String(array, "UTF-8");
                            } catch (Exception e ) {}
                        }
                        
                        public void test3() {
                            byte[] array = null;
                            String s = null;
                            if (new java.util.Random().nextInt() > 0) {
                                array = java.util.Base64.getDecoder().decode("hello");
                            }
                            try {
                                s = new String(array, "UTF-8");
                            }
                            catch (Exception e){}
                        }
                        
                    }
                """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))
        val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
        val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

        "Typed value null" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test1",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()

            val array = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["array"]!!)
            val string = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["s"]!!)

            // null references are represented by a TypedReferenceValue where the value is known and null
            array.shouldBeInstanceOf<TypedReferenceValue>()
            array.isParticular shouldBe true
            array.value() shouldBe null

            string.shouldBeInstanceOf<TypedReferenceValue>()
        }

        "Reassign value null" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test2",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()

            val array = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["array"]!!)
            val string = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["s"]!!)

            // null references are represented by a TypedReferenceValue where the value is known and null
            array.shouldBeInstanceOf<TypedReferenceValue>()
            array.isParticular shouldBe true
            array.value() shouldBe null

            string.shouldBeInstanceOf<TypedReferenceValue>()
        }

        "Generalized value maybe null" - {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test3",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()

            val array = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["array"]!!)
            val string = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["s"]!!)

            "Correct" {
                array.shouldBeInstanceOf<TypedReferenceValue>()
                array.type shouldBe "[B"
                string.shouldBeInstanceOf<TypedReferenceValue>()
            }

            "Should not be identified since can also be null reference".config(enabled = false) {
                array.shouldNotBeInstanceOf<IdentifiedReferenceValue>()
            }
        }
    }

    "Runtime type taken into consideration when generalizing" - {

        val code = JavaSource(
            "Test.java",
            """
                import java.util.Random;
                class Test {
                    public void test(){
                        CharSequence s = null;
                        if (new Random().nextInt() > 0)
                        {
                            s = "42";
                        }
                        else {
                            // return type is CharSequence, dynamically returns String
                            s = "hello".subSequence(0, 2);
                        }
                        System.out.println(s);
                    }
                }
                """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))
        val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
        val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

        val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
            "Test",
            "test",
            "()V",
            programClassPool,
            partialEvaluator,
        )

        val (instruction, _) = instructions.last()
        val s = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["s"]!!)

        "Type should be String" {
            s.shouldBeInstanceOf<TypedReferenceValue>()
            s.shouldNotBeInstanceOf<IdentifiedReferenceValue>()
            s.type shouldBe "Ljava/lang/String;"
        }
    }

    "Regression test: no exception when analyzing methods returning void" - {

        val code = JavaSource(
            "Test.java",
            """
                import java.util.Random;
                class Test {
                    public void test(){
                        "42".getChars(0, 2, new char[2], 0);
                    }
                }
                """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))
        val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())
        val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

        shouldNotThrowAny {
            PartialEvaluatorUtil.evaluate(
                "Test",
                "test",
                "()V",
                programClassPool,
                partialEvaluator,
            )
        }
    }

    "Regression test: StringBuilder in variables updated" - {

        val code = JavaSource(
            "Test.java",
            """
                class Test {
                    public void test(){
                        StringBuilder sb = new StringBuilder("hello");
                        sb.append(someUnknowString());
                        String str = sb.toString();
                    }
                    
                    public String someUnknowString() {
                        return null;
                    }
                }
                """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))
        val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())
        val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

        val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
            "Test",
            "test",
            "()V",
            programClassPool,
            partialEvaluator,
        )

        val (instruction, _) = instructions.last()

        val str = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["str"]!!)
        str.shouldNotBeInstanceOf<ParticularReferenceValue>()
    }

    "Static method with no parameters executed" - {

        val code = JavaSource(
            "Test.java",
            """
                class Test {
                    public void test(){
                        String str = foo();
                    }
                    
                    public static String foo() {
                        return "42";
                    }
                }
                """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))
        val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())

        val fooExecutor = object : Executor {

            override fun getMethodResult(
                methodData: MethodExecutionInfo,
                valueCalculator: ValueCalculator,
            ): MethodResult {
                return MethodResult.Builder()
                    .setReturnValue(
                        valueCalculator.apply(
                            ClassConstants.TYPE_JAVA_LANG_STRING,
                            libraryClassPool.getClass(ClassConstants.NAME_JAVA_LANG_STRING),
                            true,
                            "42",
                            false,
                            null,
                        ),
                    ).build()
            }

            override fun getSupportedMethodSignatures(): MutableSet<MethodSignature> {
                return mutableSetOf(MethodSignature("Test", "foo", "()Ljava/lang/String;"))
            }
        }

        val invocationUnit = ExecutingInvocationUnit.Builder().addExecutor { fooExecutor }.build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

        val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
            "Test",
            "test",
            "()V",
            programClassPool,
            partialEvaluator,
        )

        val (instruction, _) = instructions.last()

        val str = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["str"]!!)
        str.shouldBeInstanceOf<ParticularReferenceValue>()
        str.value.preciseValue shouldBe "42"
    }
})
