package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.util.InstructionSequenceMatcher
import testutils.ClassPoolBuilder
import testutils.InstructionBuilder

class CannotMergeTest : FreeSpec({
    "Can not merge z and i test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "MultiSelectListPreference.smali",
                """
                    .class Landroid/preference/MultiSelectListPreference;
                    .super Ljava/lang/Object;
                    # virtual methods
                    .method test(Ljava/util/Set;Ljava/lang/Object;)V
                        .registers 3
                        invoke-interface {p1, p2}, Ljava/util/Set;->add(Ljava/lang/Object;)Z
                        move-result p1
                        invoke-static {p0, p1}, Landroid/preference/MultiSelectListPreference;->access${'$'}076(Landroid/preference/MultiSelectListPreference;I)Z
                        return-void
                    .end method
                """.trimIndent()
            )
        )
        val testClass = programClassPool.getClass("android/preference/MultiSelectListPreference")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class android/preference/MultiSelectListPreference" {
            testClass shouldNotBe null
        }

        "Check if the class contains method test" - {
            testClass
                .findMethod("test", "(Ljava/util/Set;Ljava/lang/Object;)V") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = with(InstructionBuilder()) {
                aload(0)
                aload(1)
                aload(2)
                invokeinterface("java/util/Set", "add", "(Ljava/lang/Object;)Z")
                invokestatic("android/preference/MultiSelectListPreference", "access$076", "(Landroid/preference/MultiSelectListPreference;I)Z")
                pop()
                return_()
            }
            val matcher = InstructionSequenceMatcher(instructionBuilder.constants(), instructionBuilder.instructions())

            testClass.methodsAccept(
                AllAttributeVisitor(
                AllInstructionVisitor(matcher)
                )
            )

            matcher.isMatching shouldBe true
        }
    }
})
