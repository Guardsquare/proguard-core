package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUILDER
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.IdentifiedValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TopValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownIntegerValue
import proguard.evaluation.value.UnknownValue
import proguard.evaluation.value.Value
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool

class ValueAbstractStateTest : FreeSpec({

    "Abstract states with particular strings" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())

        val myString = valueFactory.createString("myString")
        val myOtherString = valueFactory.createString("myOtherString")

        "Two abstract states with the same string should be equal" {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myString)
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with the different strings should not be equal" {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myOtherString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with different instances but equal strings should be equal" {
            val a = ValueAbstractState(myString)
            val b =
                ValueAbstractState(valueFactory.createString("myString"))
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    "Abstract states with identified strings" - {
        val valueFactory = IdentifiedValueFactory()

        val myString = valueFactory.createString("myString")
        val myOtherString = valueFactory.createString("myOtherString")

        "Two abstract states with the same string should be equal" {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myString)
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            b.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with the different strings should not be equal" {
            val b = ValueAbstractState(myOtherString)
            val a = ValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with different instances but equal strings should not be equal" {
            val a = ValueAbstractState(myString)
            val b =
                ValueAbstractState(valueFactory.createString("myString"))
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    "Abstract states with particular StringBuilders" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())

        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder())
        val myOtherStringBuilder = valueFactory.createStringBuilder(StringBuilder())

        "Two abstract states with the same string builder should be equal" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myStringBuilder)
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with the different string builders should not be equal" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myOtherStringBuilder)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/StringBuilder;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    "Abstract states with particular StringBuilders with the same ID" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())

        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder(), 1)
        val myOtherStringBuilder = valueFactory.createStringBuilder(StringBuilder(), 1)

        "Two abstract states with the same string builder instances should be equal" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myStringBuilder)
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with the different string builder instances but the same ID should be equal" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myOtherStringBuilder)
            a shouldBe b
            a.hashCode() shouldBe b.hashCode()
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldBe a
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Two abstract states with the different string builder instances and the different ID should not be equal" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(valueFactory.createStringBuilder(StringBuilder(), 2))
            a shouldNotBe b
            a.hashCode() shouldNotBe b.hashCode()
            a.isLessOrEqual(b) shouldNotBe true
            val leastUpperBound = a.join(b)
            leastUpperBound shouldNotBe a
            val value = a.join(b).value
            value.shouldBeInstanceOf<TypedReferenceValue>()
            value.shouldNotBeInstanceOf<IdentifiedReferenceValue>()
            value.internalType() shouldBe TYPE_JAVA_LANG_STRING_BUILDER
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    "Abstract states with mixed types" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())

        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder())
        val myString = valueFactory.createString("MyString")

        "Joining a string builder with a string should result a common type" {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            // Serializable because the common class is object,
            // so then the last (alphabetically) common interface is used.
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/io/Serializable;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Joining a string with a string builder should result a common type" {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myStringBuilder)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
            (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/io/Serializable;"
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Joining a string with an unknown type should result in an unknown type" {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(UNKNOWN_VALUE)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<UnknownValue>()
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Joining an unknown type with a string should result in an unknown type" {
            val a = ValueAbstractState(UNKNOWN_VALUE)
            val b = ValueAbstractState(myString)
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            val leastUpperBound = a.join(b)
            leastUpperBound.value.shouldBeInstanceOf<UnknownValue>()
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }

        "Joining a String and a variable with different computational type" {
            val a = ValueAbstractState(valueFactory.createString(""))
            val b = ValueAbstractState(valueFactory.createIntegerValue(0))

            val leastUpperBound = a.join(b)
            leastUpperBound.value shouldBe UNKNOWN_VALUE
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    "Joining top with unknown should result in unknown" {
        val a = ValueAbstractState(TopValue())
        val b = ValueAbstractState(UNKNOWN_VALUE)

        a shouldNotBe b
        a.isLessOrEqual(b) shouldBe true
        val leastUpperBound = a.join(b)
        leastUpperBound.value shouldBe UNKNOWN_VALUE
        leastUpperBound.join(a) shouldBe leastUpperBound
        leastUpperBound.join(b) shouldBe leastUpperBound
    }

    "Joining a know and unknown integer" {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())

        val a = ValueAbstractState(valueFactory.createIntegerValue())
        val b = ValueAbstractState(valueFactory.createIntegerValue(0))

        val leastUpperBound = a.join(b)
        leastUpperBound.value shouldBe UnknownIntegerValue()
        leastUpperBound.join(a) shouldBe leastUpperBound
        leastUpperBound.join(b) shouldBe leastUpperBound
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

private fun ValueFactory.createStringBuilder(sb: StringBuilder, id: Int): Value = createReferenceValueForId(
    "Ljava/lang/StringBuilder;",
    libraryClassPool.getClass("java/lang/StringBuilder"),
    false,
    false,
    id,
    sb
)
