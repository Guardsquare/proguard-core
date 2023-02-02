package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.classfile.ClassPool
import proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE
import proguard.evaluation.value.IdentifiedValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ParticularValueFactory.ReferenceValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownValue
import proguard.evaluation.value.Value
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool

class JvmValueAbstractStateTest : FreeSpec({
    beforeContainer {
        // TODO(D17756): for now, this will trigger initialization of the libraryClassPool
        ClassPoolBuilder.initialize(ClassPool(), false)
    }

    "Abstract states with particular strings" - {
        val valueFactory = ParticularValueFactory(ReferenceValueFactory())

        val myString = valueFactory.createString("myString")
        val myOtherString = valueFactory.createString("myOtherString")

        "Two abstract states with the same string should be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(myString)
            a shouldBe b
            a.isLessOrEqual(b) shouldBe true
            a.join(b) shouldBe a
        }

        "Two abstract states with the different strings should not be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(myOtherString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
        }

        "Two abstract states with different instances but equal strings should be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(valueFactory.createString("myString"))
            a shouldBe b
            a.isLessOrEqual(b) shouldBe true
            a.join(b) shouldBe a
        }
    }

    "Abstract states with identified strings" - {
        val valueFactory = IdentifiedValueFactory()

        val myString = valueFactory.createString("myString")
        val myOtherString = valueFactory.createString("myOtherString")

        "Two abstract states with the same string should be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(myString)
            a shouldBe JvmValueAbstractState(myString)
            b.isLessOrEqual(b) shouldBe true
            a.join(b) shouldBe a
        }

        "Two abstract states with the different strings should not be equal" {
            val b = JvmValueAbstractState(myOtherString)
            val a = JvmValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
        }

        "Two abstract states with different instances but equal strings should not be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(valueFactory.createString("myString"))
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
        }
    }

    "Abstract states with particular StringBuilders" - {
        val valueFactory = ParticularValueFactory(ReferenceValueFactory())

        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder())
        val myOtherStringBuilder = valueFactory.createStringBuilder(StringBuilder())

        "Two abstract states with the same string builder should be equal" {
            val a = JvmValueAbstractState(myStringBuilder)
            val b = JvmValueAbstractState(myStringBuilder)
            a shouldBe b
            a.isLessOrEqual(b) shouldBe true
            a.join(b) shouldBe a
        }

        "Two abstract states with the different string builders should not be equal" {
            val a = JvmValueAbstractState(myStringBuilder)
            val b = JvmValueAbstractState(myOtherStringBuilder)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/lang/StringBuilder;"
        }
    }

    "Abstract states with mixed types" - {
        val valueFactory = ParticularValueFactory(ReferenceValueFactory())

        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder())
        val myString = valueFactory.createString("MyString")

        "Joining a string builder with a string should result a common type" {
            val a = JvmValueAbstractState(myStringBuilder)
            val b = JvmValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            // Serializable because the common class is object,
            // so then the last (alphabetically) common interface is used.
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/io/Serializable;"
        }

        "Joining a string with a string builder should result a common type" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(myStringBuilder)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<TypedReferenceValue>()
            (a.join(b).value as TypedReferenceValue).type shouldBe "Ljava/io/Serializable;"
        }

        "Joining a string with an unknown type should result in an unknown type" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(UNKNOWN_VALUE)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<UnknownValue>()
        }

        "Joining an unknown type with a string should result in an unknown type" {
            val a = JvmValueAbstractState(UNKNOWN_VALUE)
            val b = JvmValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b).value.shouldBeInstanceOf<UnknownValue>()
        }
    }
})

private fun ValueFactory.createString(s: String): Value = createReferenceValue(
    "Ljava/lang/String;",
    libraryClassPool.getClass("java/lang/String"),
    false,
    false,
    s
)
private fun ValueFactory.createStringBuilder(sb: StringBuilder): Value = createReferenceValue(
    "Ljava/lang/StringBuilder;",
    libraryClassPool.getClass("java/lang/StringBuilder"),
    false,
    false,
    sb
)
