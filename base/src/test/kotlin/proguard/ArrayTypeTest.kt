/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.TypedReferenceValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class ArrayTypeTest : FreeSpec({
    val valueFactory = TypedReferenceValueFactory()
    val invocationUnit = BasicInvocationUnit(valueFactory)
    val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)

    "Array types evaluated correctly" - {

        val code = JavaSource(
            "Test.java",
            """
        public class Test {
        
            public void objectArray() {
                Object[] array = new Object[10];
            }
            
            public void primitiveArray() {
                int[] array = new int[10];
            }
        }
        """
        )

        val (classPool, _) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

        "Reference type" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "objectArray",
                "()V",
                classPool,
                partialEvaluator
            )

            val (methodEnd, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["array"]!!) as TypedReferenceValue
            s.type shouldBe "[Ljava/lang/Object;"
        }

        "Primitive type" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "primitiveArray",
                "()V",
                classPool,
                partialEvaluator
            )

            val (methodEnd, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(methodEnd)
                .getValue(variableTable["array"]!!) as TypedReferenceValue
            s.type shouldBe "[I"
        }
    }
})
