package proguard.classfile.attribute.signature.parsing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class GeneralParserTest : FunSpec({
    test("fixed string parser") {
        val input = "aaa"
        val ctx = ParserContext(input)
        Parsers.fixedString("a").parse(ctx) shouldNotBeNull {}
        ctx.remainingLength() shouldBe 2
    }

    test("repeated fixed string") {
        val ctx = ParserContext("aaa")
        val out = Combinators.repeat(
            Parsers.fixedString("a"),
        ).parse(ctx)

        out shouldNotBeNull {}
        out!!.size shouldBe 3
        out!!.forEach({ it shouldNotBeNull {} })
        ctx.remainingLength() shouldBe 0
    }

    test("chained fixed string") {
        val ctx = ParserContext("ab")
        val out = Combinators.chain(
            Parsers.fixedString("a"),
            Parsers.fixedString("b"),
            { a, b -> listOf(a, b) },
        ).parse(ctx)

        out shouldNotBeNull {}
        out!!.size shouldBe 2
        out!!.forEach({ it shouldNotBeNull {} })
        ctx.remainingLength() shouldBe 0
    }

    test("early return from firstSuccessful") {
        val ctx = ParserContext("aa")
        val out = Combinators.oneOf(
            Parsers.fixedString("aa"),
            Parsers.fixedString("a"),
        ).parse(ctx)

        out shouldNotBeNull {}
        ctx.remainingLength() shouldBe 0
    }

    test("can parse the second one") {
        val ctx = ParserContext("aba")
        val out = Combinators.oneOf(
            Parsers.fixedString("aa"),
            Parsers.fixedString("ab"),
        ).parse(ctx)

        out shouldNotBeNull {}
        ctx.remainingLength() shouldBe 1
    }
})
