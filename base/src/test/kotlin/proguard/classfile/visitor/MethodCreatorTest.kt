package proguard.classfile.visitor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.classfile.AccessConstants
import proguard.classfile.ClassPool
import proguard.classfile.util.ClassReferenceInitializer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.classfile.extensions.addMethod
import proguard.testutils.classfile.extensions.buildClass

class MethodCreatorTest : BehaviorSpec({
    Given("a class A which overrides a method from C and an intermediate class B which doesn't override it") {
        val (classC, _) = buildClass("C") {
            addMethod("method") {
                return_()
            }
        }
        // Add some stuff into the method to trigger a discrepancy in the constant pools between B and C
        val (classB, _) = buildClass("B", superName = classC.name) {
            addMethod("methodX") {
                return_()
            }
        }
        val classA = buildClass("A", superName = classB.name)

        val programClassPool = ClassPoolBuilder.fromClasses(classA, classB, classA)
        val libraryClassPool = ClassPool()

        When("a method is created in class A") {
            classA.findMethod("method", "()V") shouldBe null
            classA.accept(MethodCreator(programClassPool, libraryClassPool, "method", "()V", 0, 0, true))

            Then("the method should be successfully created") {
                classA.findMethod("method", "()V") shouldNotBe null
            }
        }
    }

    Given("a class which extends a class with a final method") {

        val (classB, methodB) = buildClass("B") {
            addMethod(accessFlags = AccessConstants.PUBLIC or AccessConstants.FINAL, name = "method", descriptor = "()V") {
                return_()
            }
        }
        val classA = buildClass("A", superName = classB.name)

        val programClassPool = ClassPoolBuilder.fromClasses(classA, classB)
        val libraryClassPool = ClassPool()
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        When("a method with the same name is created") {
            classA.accept(MethodCreator(programClassPool, libraryClassPool, "method", "()V", 0, 0, true))

            Then("the super-method should no longer be final") {
                (methodB.u2accessFlags) and AccessConstants.FINAL shouldBe 0
            }
        }
    }
})
