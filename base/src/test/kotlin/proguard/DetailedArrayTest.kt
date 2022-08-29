package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.DetailedArrayReferenceValue
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ParticularValueFactory.ReferenceValueFactory
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil


class DetailedArrayTest : FreeSpec ({
    val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ReferenceValueFactory())
    val invocationUnit = ExecutingInvocationUnit(valueFactory)
    val partialEvaluator = PartialEvaluator(valueFactory,
        invocationUnit,
        false)

    "Array values evaluated correctly" - {

        "Regression test: future update does not change previous value" {

            val code = JavaSource(
                "Test.java",
                """
            public class Test {

                public void arrayTest() {
                    int[] array = new int[5];
                    array[0] = 4;
                    array[1] = 42;
                    array[1] = 2;
                }
            }
            """
            )

            val (classPool, _) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                classPool,
                partialEvaluator
            )

            /*
                [0] iconst_5
                [1] newarray 10
                [3] astore_1 v1
                [4] aload_1 v1
                [5] iconst_0
                [6] iconst_4
                [7] iastore
                [8] aload_1 v1
                [9] iconst_1
                [10] bipush 42
                [12] iastore
                [13] aload_1 v1
                [14] iconst_1
                [15] iconst_2
                [16] iastore
                [17] return
             */

            val (instruction, _) = instructions[13]
            val s = partialEvaluator.getVariablesBefore(instruction)
                .getValue(variableTable["array"]!!) as DetailedArrayReferenceValue
            s.type shouldBe "[I"
            s.integerArrayLoad(valueFactory.createIntegerValue(1), valueFactory).integerValue().value() shouldBe 42
        }
    }
})