package proguard.analysis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class ExecutingInvocationUnitDynamicDispatchTest : FunSpec({
    test("Dynamic type used to match executors") {

        val code = JavaSource(
            "Test.java",
            """
        public class Test {

            public void test(Object a) {
                a = "42";
                String b = a.toString();
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
            "(Ljava/lang/Object;)V",
            programClassPool,
            partialEvaluator,
        )

        val (instruction, _) = instructions.last()
        val b = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["b"]!!)

        b.isParticular shouldBe true
        b.referenceValue().value.preciseValue shouldBe "42"
    }
})
