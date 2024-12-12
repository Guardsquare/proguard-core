package proguard.evaluation.executor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.`object`.model.ClassModel
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil
import proguard.util.BasicHierarchyProvider

class ReflectiveModelExecutorTest : BehaviorSpec({
    Given("Test model executor") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Foo.java",
                """
                package com.example;
                public class Foo {
                    public static void main(String[] args) throws Exception {
                        // getSuperclass()
                        Class<?> clz = SubFoo.class.getSuperclass();
                    }
                }

                class SubFoo extends Foo {
                }

                """.trimIndent(),
            ),
            javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"),
        )

        When("It is partially evaluated with a JavaReflectionExecutor") {
            val particularValueFactory = ParticularValueFactory(
                ArrayReferenceValueFactory(),
                ParticularReferenceValueFactory(),
            )

            val executorBuilder = ReflectiveModelExecutor
                .Builder(BasicHierarchyProvider(programClassPool, libraryClassPool))
                .addSupportedModel(ReflectiveModelExecutor.SupportedModelInfo(ClassModel::class.java, false))

            val particularValueEvaluator = PartialEvaluator.Builder.create()
                .setValueFactory(particularValueFactory)
                .setInvocationUnit(
                    ExecutingInvocationUnit.Builder(programClassPool, libraryClassPool)
                        .setEnableSameInstanceIdApproximation(true)
                        .useDefaultStringReflectionExecutor(true)
                        .addExecutor(executorBuilder)
                        .build(particularValueFactory),
                )
                .setEvaluateAllCode(true)
                .stopAnalysisAfterNEvaluations(50)
                .build()

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "com/example/Foo",
                "main",
                "([Ljava/lang/String;)V",
                programClassPool,
                particularValueEvaluator,
            )

            val (instruction, _) = instructions.last()
            val clz = particularValueEvaluator.getVariablesBefore(instruction).getValue(variableTable["clz"]!!)

            Then("Then the retrieved class is the super class") {
                clz.shouldBeInstanceOf<ParticularReferenceValue>()
                val fooValue = clz.referenceValue().value.modeledValue
                fooValue.shouldBeInstanceOf<ClassModel>()
                fooValue.clazz shouldBe programClassPool.getClass("com/example/Foo")
            }
        }
    }
})
