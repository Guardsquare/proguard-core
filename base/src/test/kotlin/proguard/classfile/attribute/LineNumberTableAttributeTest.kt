package proguard.classfile.attribute

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.MethodSignature

class LineNumberTableAttributeTest : BehaviorSpec({

    given("A line number table with lines with and without source") {
        val sourceInfo = LineNumberInfoSource(MethodSignature("com/example/Foo", "bar", "()V"))

        val table = LineNumberTableAttribute(
            -1,
            2,
            arrayOf(
                LineNumberInfo(10, 100),
                LineNumberInfo(20, 200, sourceInfo),
            ),
        )

        `when`("Getting the source of a non-existing line") {
            val source = table.getSource(0)

            then("The result should be null") {
                source shouldBe null
            }
        }

        `when`("Getting the source info of a non-existing line") {
            val source = table.getLineNumberInfoSource(0)

            then("The result should be null") {
                source shouldBe null
            }
        }

        `when`("Getting the source of the line without a source") {
            val source = table.getSource(10)

            then("The result should be null") {
                source shouldBe null
            }
        }

        `when`("Getting the source info of the line without a source") {
            val source = table.getLineNumberInfoSource(10)

            then("The result should be null") {
                source shouldBe null
            }
        }

        `when`("Getting the source of the line with a source") {
            val source = table.getSource(20)

            then("The result should match") {
                source shouldBe "com/example/Foo.bar()V"
            }
        }

        `when`("Getting the source info of the line without a source") {
            val source = table.getLineNumberInfoSource(20)

            then("The result should match") {
                source shouldBe sourceInfo
            }
        }
    }
})
