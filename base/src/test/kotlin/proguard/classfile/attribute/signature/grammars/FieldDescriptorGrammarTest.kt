package proguard.classfile.attribute.signature.grammars

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class FieldDescriptorGrammarTest : FunSpec({
    val descriptors = listOf(
        "Ljava/lang/Object;",
        "[[B",
        "[[[D",
        "[[Ljava/lang/Object;",
    )

    descriptors.forEach({ sig ->
        test("parse and convert back") {
            FieldDescriptorGrammar.parse(sig) shouldNotBeNull {
                toString() shouldBe sig
            }
        }
    })
})
