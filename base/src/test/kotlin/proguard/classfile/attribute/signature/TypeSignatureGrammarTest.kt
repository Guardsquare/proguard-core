package proguard.classfile.attribute.signature

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import proguard.classfile.attribute.signature.parsing.ParserContext

class TypeSignatureGrammarTest : FunSpec({
    val signatures = listOf(
        "Ljava/util/concurrent/CopyOnWriteArrayList<Landroidx/core/util/Consumer<Landroid/content/res/Configuration;>;>;",
        "Ljava/util/concurrent/CopyOnWriteArrayList<Landroidx/core/util/Consumer<Landroidx/core/app/MultiWindowModeChangedInfo;>;>;",
        "Ljava/util/List<-Ljava/lang/Number;>;",
        "Ljava/util/List<+Ljava/lang/Number;>;",
        "Ljava/util/List<*>;",
        "Ljava/util/Map<Ljava/lang/String;Ljava/util/Set<Ljava/lang/Double;>;>;",
        "[[Ljava/lang/String;",
        "[I",
        "[[I",
        "I",
    )

    val invalid = listOf(
        "",
        "L",
        "[]",
        "Ljava/lang/Object", // missing semicolon
        "LI",
        "TI",
        "V",
    )

    signatures.forEach { sig ->
        test("Parsing and converting back should lead to the same as original") {
            TypeSignatureGrammar.parse(sig) shouldNotBeNull {
                toString() shouldBe sig
            }
        }
    }

    invalid.forEach { sig ->
        test("Invalid signatures should not be parseable") {
            TypeSignatureGrammar.parse(sig) shouldBe null
        }
    }

    test("correctly parsing package specifier") {
        val ctx =
            ParserContext("java/util/concurrent/CopyOnWriteArrayList")
        val out = TypeSignatureGrammar.PACKAGE_SPECIFIER.parse(ctx)

        out shouldNotBeNull {}
        out.toString() shouldBe "java/util/concurrent/"
        ctx.remainingLength() shouldBe "CopyOnWriteArrayList".length
    }

    test("correctly parsing simple class type signature") {
        val ctx =
            ParserContext("Consumer<Landroid/content/res/Configuration;>")
        val out = TypeSignatureGrammar.SIMPLE_CLASS_TYPE_SIGNATURE.parse(ctx)

        out shouldNotBeNull {}
        out!!.identifier shouldBe "Consumer"
    }
})
