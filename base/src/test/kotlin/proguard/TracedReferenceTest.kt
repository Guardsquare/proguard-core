package proguard

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.buildClass
import proguard.classfile.AccessConstants
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.ReferenceTracingInvocationUnit
import proguard.evaluation.ReferenceTracingValueFactory
import proguard.evaluation.value.TracedReferenceValue
import proguard.evaluation.value.Value

class TracedReferenceTest : BehaviorSpec({
    Given("A partial evaluator with a TracingReferenceInvocationUnit") {
        val valueFactory = ReferenceTracingValueFactory(ParticularReferenceValueFactory())
        val invocationUnit = ReferenceTracingInvocationUnit(BasicInvocationUnit(valueFactory))
        val partialEvaluator =
            PartialEvaluator.Builder.create().setValueFactory(valueFactory).setInvocationUnit(invocationUnit)
                .setExtraInstructionVisitor(valueFactory).build()

        When("Evaluating aconst_null") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                it.aconst_null().pop().return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("The value on the stack should be considered initialized") {
                val stack = partialEvaluator.getStackAfter(0)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Loading a reference from a parameter") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "(Ljava/lang/Object;)I", 50) {
                it.aload_1().invokevirtual("java/lang/Object", "hashCode", "()I").ireturn()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Then reading the reference after the first load") {
                val vars = partialEvaluator.getVariablesBefore(0)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Creating a new reference on the stack") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                it
                    .new_("java/lang/Object")
                    .dup()
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .pop()
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val stack = partialEvaluator.getStackAfter(3)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
                (stack.getTop(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference after it is initialized") {
                val stack = partialEvaluator.getStackAfter(4)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Creating a new reference on the stack and initializing in a backwards jump") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                val forward = it.createLabel()
                val backward = it.createLabel()
                it
                    .goto_(forward)
                    .label(backward)
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .pop()
                    .return_()
                    .label(forward)
                    .new_("java/lang/Object")
                    .dup()
                    .goto_(backward)
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val stack = partialEvaluator.getStackAfter(11)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
                (stack.getTop(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference after it is initialized") {
                val stack = partialEvaluator.getStackAfter(3)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Creating a new reference and storing it in a variable") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                it
                    .new_("java/lang/Object")
                    .astore_1()
                    .aload_1()
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(4)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference after it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(8)
                println(vars)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Creating a new reference in a variable and initializing in a backwards jump") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                val forward = it.createLabel()
                val backward = it.createLabel()
                it
                    .goto_(forward)
                    .label(backward)
                    .aload_1()
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .return_()
                    .label(forward)
                    .new_("java/lang/Object")
                    .astore_1()
                    .goto_(backward)
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(11)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference after it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(4)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }
        }

        When("Creating a new reference on the stack and initializing in a single branch") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                val left = it.createLabel()
                val right = it.createLabel()
                it
                    .new_("java/lang/Object")
                    .dup()
                    .iconst_0()
                    .ifeq(left)
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .goto_(right)
                    .label(left)
                    .pop()
                    .label(right)
                    .pop()
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val stack = partialEvaluator.getStackAfter(3)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
                (stack.getTop(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference in the branch where it is not initialized") {
                val stack = partialEvaluator.getStackAfter(14)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference in the branch where it is initialized") {
                val stack = partialEvaluator.getStackAfter(8)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }

            Then("Reading the reference where the branches converge") {
                val stack = partialEvaluator.getStackBefore(15)
                (stack.getTop(0) as TracedReferenceValue).isInitialized shouldBe Value.MAYBE
            }
        }

        When("Creating a new reference in a variable and initializing in a single branch") {
            val clazz = buildClass()
            val method = clazz.addAndReturnMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                val left = it.createLabel()
                val right = it.createLabel()
                it
                    .new_("java/lang/Object")
                    .astore_1()
                    .iconst_0()
                    .ifeq(left)
                    .aload_1()
                    .invokespecial("java/lang/Object", "<init>", "()V")
                    .goto_(right)
                    .label(left)
                    .aload_1()
                    .pop()
                    .label(right)
                    .return_()
            }

            evaluate(clazz.programClass, method, partialEvaluator)

            Then("Reading the reference before it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(3)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference in the branch where it is not initialized") {
                val vars = partialEvaluator.getVariablesAfter(15)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.NEVER
            }

            Then("Reading the reference in the branch where it is initialized") {
                val vars = partialEvaluator.getVariablesAfter(9)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.ALWAYS
            }

            Then("Reading the reference where the branches converge") {
                val vars = partialEvaluator.getVariablesBefore(17)
                (vars.getValue(1) as TracedReferenceValue).isInitialized shouldBe Value.MAYBE
            }
        }
    }
})

fun evaluate(clazz: ProgramClass, method: ProgramMethod, partialEvaluator: PartialEvaluator) {
    method.accept(clazz, AllAttributeVisitor(AttributeNameFilter(Attribute.CODE, partialEvaluator)))
}
