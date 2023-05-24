package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class ExecutingInvocationUnitTest : FreeSpec({

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
            """
    )

    "Expected reference ids" - {

        "Approximate id method returns same type as instance" - {
            val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test",
                "()V",
                programClassPool,
                partialEvaluator
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
                false
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "test",
                "()V",
                programClassPool,
                partialEvaluator
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
})
