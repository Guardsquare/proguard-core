package proguard.classfile.attribute

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.exception.ProguardCoreException

class LineNumberInfoSourceTest : BehaviorSpec({

    given("A source without ranges") {
        val source = "com/example/Foo.bar(Ljava/lang/String;I)V"

        `when`("Creating a LineNumberInfoSource from this String") {
            val info = LineNumberInfoSource.fromString(source)

            then("The internal representation should be the same") {
                info.toInternalString() shouldBe source
            }

            then("The transformed range should be NONE") {
                info.transformedStart shouldBe LineNumberInfoSource.NONE
                info.transformedEnd shouldBe LineNumberInfoSource.NONE
            }

            then("The original range should be NONE") {
                info.originalStart shouldBe LineNumberInfoSource.NONE
                info.originalEnd shouldBe LineNumberInfoSource.NONE
            }
        }
    }

    given("A source with an original range") {
        val source = "com/example/Foo.bar()I:100:200"

        `when`("Creating a LineNumberInfoSource from this String") {
            val info = LineNumberInfoSource.fromString(source)

            then("The internal representation should be the same") {
                info.toInternalString() shouldBe source
            }

            then("The transformed range should be NONE") {
                info.transformedStart shouldBe LineNumberInfoSource.NONE
                info.transformedEnd shouldBe LineNumberInfoSource.NONE
            }

            then("The original range should be the same") {
                info.originalStart shouldBe 100
                info.originalEnd shouldBe 200
            }
        }
    }

    given("A source with a transformed range") {
        val source = "100:200:com/example/Foo.bar()Ljava/lang/String"

        `when`("Creating a LineNumberInfoSource from this String") {
            val info = LineNumberInfoSource.fromString(source)

            then("The internal representation should be the same") {
                info.toInternalString() shouldBe source
            }

            then("The transformed range should be the same") {
                info.transformedStart shouldBe 100
                info.transformedEnd shouldBe 200
            }

            then("The original range should be NONE") {
                info.originalStart shouldBe LineNumberInfoSource.NONE
                info.originalEnd shouldBe LineNumberInfoSource.NONE
            }
        }
    }

    given("A source with both transformed and original ranges") {
        val source = "100:200:Foo.bar(I)V:300:400"

        `when`("Creating a LineNumberInfoSource from this String") {
            val info = LineNumberInfoSource.fromString(source)

            then("The internal representation should be the same") {
                info.toInternalString() shouldBe source
            }

            then("The transformed range should be the same") {
                info.transformedStart shouldBe 100
                info.transformedEnd shouldBe 200
            }

            then("The original range should be the same") {
                info.originalStart shouldBe 300
                info.originalEnd shouldBe 400
            }
        }
    }

    given("An invalid source string") {
        val source = "invalid"

        `when`("Parsing the invalid source String") {
            then("A ProguardCoreException should be thrown") {
                shouldThrow<ProguardCoreException> {
                    LineNumberInfoSource.fromString(source)
                }
            }
        }
    }
})
