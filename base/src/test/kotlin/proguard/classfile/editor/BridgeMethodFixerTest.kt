package proguard.classfile.editor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.classfile.AccessConstants
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6

class BridgeMethodFixerTest : BehaviorSpec({
    Given("A bridge method invoking only the correct bridged method") {
        val classBuilder = ClassBuilder(
            CLASS_VERSION_1_6,
            AccessConstants.PUBLIC,
            "TestClass",
            "java/lang/Object",
        )
        val bridgedMethod = classBuilder.addAndReturnMethod(AccessConstants.PUBLIC, "test", "(Ljava/lang/String;)V", 50) {
            it
                .return_()
        }

        val bridgeMethod = classBuilder.addAndReturnMethod((AccessConstants.PUBLIC or AccessConstants.BRIDGE or AccessConstants.SYNTHETIC), "test", "(Ljava/lang/Object;)V", 50) {
            it
                .aload(0)
                .aload(1)
                .checkcast("java/lang/String")
                .invokevirtual(classBuilder.programClass, bridgedMethod)
                .return_()
        }

        When("BridgeMethodFixer is applied") {
            classBuilder.programClass.attributesAccept(
                BridgeMethodFixer(),
            )

            Then("The bridge flag is not removed") {
                (bridgeMethod.u2accessFlags and AccessConstants.BRIDGE) shouldNotBe 0
            }
        }
    }

    Given("A bridge method invoking the wrong bridged method") {
        val classBuilder = ClassBuilder(
            CLASS_VERSION_1_6,
            AccessConstants.PUBLIC,
            "TestClass",
            "java/lang/Object",
        )
        val incorrectBridgedMethod = classBuilder.addAndReturnMethod(AccessConstants.PUBLIC, "someOtherName", "(Ljava/lang/String;)V", 50) { code ->
            code
                .return_()
        }
        val bridgeMethod = classBuilder
            .addMethod((AccessConstants.PUBLIC or AccessConstants.SYNTHETIC or AccessConstants.BRIDGE), "test", "(Ljava/lang/Object;)V", 50) { code ->
                code
                    .aload(0)
                    .aload(1)
                    .checkcast("java/lang/String")
                    .invokevirtual(classBuilder.programClass, incorrectBridgedMethod)
                    .return_()
            }
            .programClass

        When("BridgeMethodFixer is applied") {
            classBuilder.programClass.attributesAccept(
                BridgeMethodFixer(),
            )

            Then("The bridge flag is removed") {
                bridgeMethod.u2accessFlags and AccessConstants.BRIDGE shouldBe 0
            }
        }
    }

    Given("A bridge method invoking an incorrect and correct bridged method") {
        val classBuilder = ClassBuilder(
            CLASS_VERSION_1_6,
            AccessConstants.PUBLIC,
            "TestClass",
            "java/lang/Object",
        )
        val bridgedMethod = classBuilder.addAndReturnMethod(AccessConstants.PUBLIC, "test", "(Ljava/lang/String;)V", 50) { code ->
            code
                .return_()
        }
        val incorrectBridgedMethod = classBuilder.addAndReturnMethod(AccessConstants.PUBLIC, "someOtherName", "(Ljava/lang/String;)V", 50) { code ->
            code
                .return_()
        }
        val bridgeMethod = classBuilder
            .addAndReturnMethod(AccessConstants.PUBLIC or AccessConstants.SYNTHETIC or AccessConstants.BRIDGE, "test", "(Ljava/lang/Object;)V", 50) { code ->
                code
                    .aload(0)
                    .aload(1)
                    .checkcast("java/lang/String")
                    .invokevirtual(classBuilder.programClass, incorrectBridgedMethod)
                    .aload(0)
                    .aload(1)
                    .checkcast("java/lang/String")
                    .invokevirtual(classBuilder.programClass, bridgedMethod)
                    .return_()
            }

        When("BridgeMethodFixer is applied") {
            classBuilder.programClass.attributesAccept(
                BridgeMethodFixer(),
            )

            Then("The bridge flag is not removed") {
                (bridgeMethod.u2accessFlags and AccessConstants.BRIDGE) shouldNotBe 0
            }
        }
    }
})
