package proguard.classfile.editor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.util.inject.location.FirstBlock
import proguard.classfile.util.inject.location.LastBlocks
import proguard.classfile.util.inject.location.SpecificOffset
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class InjectionStrategyTest : BehaviorSpec({
    Given("a class targeted for injection") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "InjectionTarget.java",
                """
                public class InjectionTarget {
                    public InjectionTarget() {}
                    public InjectionTarget(int dummyInt, float dummyFloat) {}
                    public int singleReturn() { return 0; }
                    public int multipleReturns() {
                        int i = (int) System.currentTimeMillis();
                        if (i > 1000) {
                            return i;
                        }
                        else {
                            return -1;
                        }
                    }
                    public int throwOrReturn() {
                        int i = (int) System.nanoTime();
                        if (i > 1000) {
                            return i;
                        }
                        throw new RuntimeException();
                    }
                }
                """.trimIndent(),
            ),
        )
        val injectTargetClass = programClassPool.getClass("InjectionTarget") as ProgramClass
        val defaultConstructor = injectTargetClass.findMethod("<init>", "()V") as ProgramMethod
        val constructorWithParam = injectTargetClass.findMethod("<init>", "(IF)V") as ProgramMethod
        val singleReturnMethod = injectTargetClass.findMethod("singleReturn", "()I") as ProgramMethod
        val multipleReturnsMethod = injectTargetClass.findMethod("multipleReturns", "()I") as ProgramMethod
        val throwOrReturnMethod = injectTargetClass.findMethod("throwOrReturn", "()I") as ProgramMethod

        When("The FirstBlock injection strategy visits each method") {
            Then("There should be one injection location for each method") {
                FirstBlock().getAllSuitableInjectionLocation(injectTargetClass, defaultConstructor).size shouldBe 1
                FirstBlock().getAllSuitableInjectionLocation(injectTargetClass, constructorWithParam).size shouldBe 1
                FirstBlock().getAllSuitableInjectionLocation(injectTargetClass, singleReturnMethod).size shouldBe 1
                FirstBlock().getAllSuitableInjectionLocation(injectTargetClass, multipleReturnsMethod).size shouldBe 1
                FirstBlock().getAllSuitableInjectionLocation(injectTargetClass, throwOrReturnMethod).size shouldBe 1
            }
        }

        When("The LastBlocks injection strategy visits each method") {
            Then("There should be one injection location for method with one return statement") {
                LastBlocks().getAllSuitableInjectionLocation(injectTargetClass, defaultConstructor).size shouldBe 1
                LastBlocks().getAllSuitableInjectionLocation(injectTargetClass, constructorWithParam).size shouldBe 1
                LastBlocks().getAllSuitableInjectionLocation(injectTargetClass, singleReturnMethod).size shouldBe 1
            }
            Then("There should be two injection locations for methods with two exit blocks") {
                LastBlocks().getAllSuitableInjectionLocation(injectTargetClass, multipleReturnsMethod).size shouldBe 2
                LastBlocks().getAllSuitableInjectionLocation(injectTargetClass, throwOrReturnMethod).size shouldBe 2
            }
        }

        When("The SpecificOffset injection strategy visits each method") {
            Then("There should be one injection location for each method") {
                SpecificOffset(0, true).getAllSuitableInjectionLocation(injectTargetClass, defaultConstructor).size shouldBe 1
                SpecificOffset(1, false).getAllSuitableInjectionLocation(injectTargetClass, constructorWithParam).size shouldBe 1
                SpecificOffset(2, true).getAllSuitableInjectionLocation(injectTargetClass, singleReturnMethod).size shouldBe 1
                SpecificOffset(3, false).getAllSuitableInjectionLocation(injectTargetClass, multipleReturnsMethod).size shouldBe 1
                SpecificOffset(4, true).getAllSuitableInjectionLocation(injectTargetClass, throwOrReturnMethod).size shouldBe 1
            }
        }
    }
})
