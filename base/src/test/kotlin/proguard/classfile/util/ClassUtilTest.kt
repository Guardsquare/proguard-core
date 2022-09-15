package proguard.classfile.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.util.ClassUtil.externalClassVersion
import proguard.classfile.util.ClassUtil.internalClassVersion

class ClassUtilTest : FreeSpec({
    val version19 = 63 shl 16
    "Given class file version 19" - {
        "The external version should be 19" {
            externalClassVersion(version19) shouldBe "19"
        }
    }

    "Given a class file with version 19" - {
        val programClass = ClassBuilder(
            version19,
            PUBLIC,
            "Test",
            "java/lang/Object"
        ).programClass
        "The internal version should be 19" {
            programClass.u4version shouldBe internalClassVersion("19")
        }
    }
})
