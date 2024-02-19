package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
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

        "Approximate id method returns same type as instance" - {
            val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

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
            val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(false).build(valueFactory)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

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
        val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
        val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory)
        val partialEvaluator = PartialEvaluator(
            valueFactory,
            invocationUnit,
            false,
        )

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
})
