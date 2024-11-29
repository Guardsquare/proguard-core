package proguard.normalize

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.egyptianHieroglyphs
import io.kotest.property.arbitrary.string
import proguard.classfile.ProgramClass
import proguard.classfile.constant.Utf8Constant
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.runMethod
import proguard.util.StringUtil

class LargeStringSplitterTest : BehaviorSpec({
    Given("a class with methods that return some long strings") {
        val rs = RandomSource.default()
        // Note: Arb.string generates printable ASCII strings by default, so each character is 1 byte.
        val tests = arrayOf(
            Triple("testMaxLengthString", Arb.string(size = 65535).sample(rs).value, false),
            Triple("testOneMoreThanMaxLengthString", Arb.string(size = 65536).sample(rs).value, true),
            Triple("testSuperLongString", Arb.string(size = 200000).sample(rs).value, true),
            Triple("testWithSurrogateCrossingBoundary", Arb.string(size = 65532).sample(rs).value + "\uD83C\uDF0A" + "E", true),
            Triple("testLongEgyptianString", Arb.string(size = 100000, codepoints = Codepoint.egyptianHieroglyphs()).sample(rs).value, true),
            Triple("testStringThatFitsInUtf8ButNotInModifiedUtf8", Arb.string(size = 65531).sample(rs).value + "\uD83D\uDC1B", true),
            // Starts with a low surrogate, then follows with high surrogates.
            Triple("testUnpairedSurrogates", "\uDF0A" + "\uD83C".repeat(21844), false),
            Triple("testSnug7f", "\u007f".repeat(65535), false),
            Triple("testSpilling80", "\u0080".repeat(32768), true),
            Triple("testSnug7ff", "\u07ff".repeat(32767), false),
            Triple("testSpilling800", "\u0800".repeat(21846), true),
        )
        // Each entry contains the string and whether it should be split.
        val testStringMap = tests.associate { it.first to it.second }
        val testSplittableMap = tests.associate { it.first to it.third }

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Foo.java",
                "public class Foo { " + testStringMap.keys.joinToString(separator = "") { "public static String $it() { return \"str$it\"; }" } + "}",
            ),
        )

        // Directly inserting the strings into the source is annoying because they have to be escaped,
        // so we put substitution strings in the source. We replace these with the actual strings here.
        // Note: we added a "str" prefix to differentiate the string from the method name, so we need to strip it.
        val fooClass = programClassPool.getClass("Foo") as ProgramClass
        fooClass.constantPool.filterIsInstance<Utf8Constant>().forEach { constant ->
            if (constant.string.startsWith("str")) {
                val string = constant.string.replace(Regex("^str"), "")
                if (string in testStringMap) {
                    constant.string = testStringMap[string]
                }
            }
        }

        When("the string splitter is applied") {
            fooClass.accept(LargeStringSplitter(programClassPool, libraryClassPool))

            Then("all strings should be at most 65535 bytes long") {
                fooClass.constantPool.filterIsInstance<Utf8Constant>().forEach { constant ->
                    StringUtil.getModifiedUtf8Length(constant.string) shouldBeLessThan 0x10000
                }
            }

            testSplittableMap.forEach { (method, splittable) ->
                Then("$method should return the correct string") {
                    fooClass.runMethod(method) shouldBe testStringMap[method]
                }

                if (splittable) {
                    Then("the string in $method should be split") {
                        fooClass.constantPool shouldNotContain Utf8Constant(testStringMap[method])
                    }
                } else {
                    Then("the string in $method should not be split") {
                        fooClass.constantPool shouldContain Utf8Constant(testStringMap[method])
                    }
                }
            }
        }
    }
})
