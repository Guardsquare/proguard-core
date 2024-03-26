package proguard.evaluation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.classfile.attribute.Attribute.CODE
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.`object`.ClassModel
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class ClassValueTest : BehaviorSpec({
    Given("A method which uses a .class-constant") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
           public class Test {
               public static void main(String[] args) throws Exception {
                   Class<?> clazz = Test.class;
                   clazz.getDeclaredConstructors();
               }
           } 
                """.trimIndent(),
            ),
            javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"),
        )

        When("It is partially evaluated") {
            val particularValueFactory = ParticularValueFactory(
                ArrayReferenceValueFactory(),
                ParticularReferenceValueFactory(),
            )
            val particularValueInvocationUnit = BasicInvocationUnit(particularValueFactory)
            val particularValueEvaluator = PartialEvaluator.Builder.create()
                .setValueFactory(particularValueFactory)
                .setInvocationUnit(particularValueInvocationUnit)
                .build()

            programClassPool.classesAccept(
                "Test",
                NamedMethodVisitor(
                    "main",
                    "([Ljava/lang/String;)V",
                    AllAttributeVisitor(
                        AttributeNameFilter(CODE, particularValueEvaluator),
                    ),
                ),
            )

            Then("The constant can be retrieved") {
                val stackBeforeGetDeclaredConstructor = particularValueEvaluator.getStackBefore(4)
                val topValue = stackBeforeGetDeclaredConstructor.getTop(0).referenceValue().value.modeledValue
                topValue.shouldBeInstanceOf<ClassModel>()
                topValue.clazz shouldBe programClassPool.getClass("Test")
            }
        }
    }
})
