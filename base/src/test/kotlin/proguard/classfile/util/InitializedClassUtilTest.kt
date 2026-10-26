package proguard.classfile.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class InitializedClassUtilTest : BehaviorSpec({
    Given("An initialized class hierarchy") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource("I0.java", "public interface I0 {}"),
            JavaSource("I1.java", "public interface I1 extends I0 {}"),
            JavaSource("A.java", "public class A implements I1 {}"),
            JavaSource("B.java", "public class B extends A {}"),
        )
        val a = programClassPool.getClass("A")!!
        val b = programClassPool.getClass("B")!!
        val i0 = programClassPool.getClass("I0")!!
        val objectClass = libraryClassPool.getClass("java/lang/Object")!!

        When("Checking inheritance relationships") {
            Then("the same type is an instance") {
                InitializedClassUtil.isInstanceOf("LB;", b) shouldBe true
            }
            Then("a superclass is detected") {
                InitializedClassUtil.isInstanceOf("LB;", a) shouldBe true
            }
            Then("transitively implemented interfaces are detected") {
                InitializedClassUtil.isInstanceOf("LA;", i0) shouldBe true
                InitializedClassUtil.isInstanceOf("LB;", i0) shouldBe true
            }
            Then("the reverse direction is not an instance") {
                InitializedClassUtil.isInstanceOf("LA;", b) shouldBe false
            }
            Then("an array type is checked via its referenced type") {
                InitializedClassUtil.isInstanceOf("[LB;", a) shouldBe true
                InitializedClassUtil.isInstanceOf("[LB;", i0) shouldBe true
            }
            Then("an unknown type is not an instance") {
                InitializedClassUtil.isInstanceOf("Lcom/does/not/Exist;", objectClass) shouldBe false
            }
        }

        When("Memoizing results") {
            Then("repeated calls return consistent results") {
                InitializedClassUtil.clearCache()
                val first = InitializedClassUtil.isInstanceOf("LB;", a)
                val second = InitializedClassUtil.isInstanceOf("LB;", a)
                first shouldBe true
                second shouldBe first
            }
            Then("clearCache resets the memoization") {
                InitializedClassUtil.clearCache()
                InitializedClassUtil.isInstanceOf("LB;", a) shouldBe true
            }
        }
    }
})
