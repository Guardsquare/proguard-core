package proguard.classfile.attribute.signature

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class MethodDescriptorGrammarTest : FunSpec({
    val descriptors = listOf(
        "(IDLjava/lang/Thread;)Ljava/lang/Object;",
        "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;",
        "(Ljava/util/Set;Ljava/lang/String;Ljava/security/Key;Ljava/security/AlgorithmParameters;)Z",
        "(BCDFIJSZ)V",
    )

    descriptors.forEach({ sig ->
        test("parse and convert back") {
            MethodSignatureGrammar.parse(sig) shouldNotBeNull {
                toString() shouldBe sig
            }
        }
    })
})
