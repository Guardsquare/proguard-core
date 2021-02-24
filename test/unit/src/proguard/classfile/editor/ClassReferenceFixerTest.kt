package proguard.classfile.editor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.*
import proguard.classfile.ClassConstants.*
import proguard.classfile.editor.ClassReferenceFixer.*

class ClassReferenceFixerTest : FreeSpec({
    "Kotlin nested class short names should be generated correctly" - {
        "with a valid Java name" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$innerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "innerClass", referencedClass) shouldBe "innerClass"
        }

        // dollar symbols are valid in Kotlin when surrounded by backticks `$innerClass`
        "with 1 dollar symbol" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$\$innerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "\$innerClass", referencedClass) shouldBe "\$innerClass"
        }

        "with multiple dollar symbols" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$\$\$inner\$Class", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "\$\$inner\$Class", referencedClass) shouldBe "\$\$inner\$Class"
        }
    }
})
