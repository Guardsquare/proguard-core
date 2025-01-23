package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import io.mockk.mockk
import proguard.analysis.cpa.jvm.cfa.JvmCfa
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUILDER
import proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.TopValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownIntegerValue
import proguard.evaluation.value.UnknownValue
import proguard.evaluation.value.Value
import proguard.evaluation.value.ValueFactory
import proguard.evaluation.value.`object`.AnalyzedObjectFactory
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool

class ValueAbstractStateTest : BehaviorSpec({

    val node = mockk<JvmCfaNode>()
    val node1 = mockk<JvmCfaNode>()
    val cfa = mockk<JvmCfa>()
    val valueFactory = JvmCfaReferenceValueFactory(cfa)

    Given("Particular string states") {
        val myString = valueFactory.createString("myString", node)
        val myOtherString = valueFactory.createString("myOtherString", node1)

        When("Two states contain the same string value") {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myString)
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states contain different string values") {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myOtherString)
            Then("The states are different and join into a typed value") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe false
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
                (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states contain the same string value but have different reference ids") {
            // The analysis compares strings by value, not by reference
            val a = ValueAbstractState(myString)
            val b =
                ValueAbstractState(valueFactory.createString("myString", node1))
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }
    }

    Given("Identified string states") {
        val myString = valueFactory.createIdentifiedString(node)
        val myOtherString = valueFactory.createIdentifiedString(node1)

        When("Two states have the same reference id") {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myString)
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                b.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states have different reference ids") {
            val b = ValueAbstractState(myOtherString)
            val a = ValueAbstractState(myString)
            Then("The states are different and join into a typed value") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe false
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
                (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/String;"
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }
    }

    Given("Particular string builder states") {
        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder(), node)
        val myOtherStringBuilder = valueFactory.createStringBuilder(StringBuilder(), node1)

        When("Two states contain the same string builder value") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myStringBuilder)
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states contain the same string builder value but have different reference ids") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myOtherStringBuilder)
            Then("The states are different and join into a typed value") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe false
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
                (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/lang/StringBuilder;"
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }
    }

    Given("Identified string builder states") {
        val myStringBuilder = valueFactory.createIdentifiedStringBuilder(node)
        val myOtherStringBuilder = valueFactory.createIdentifiedStringBuilder(node)

        When("Two states contain the same value object") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myStringBuilder)
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states have the same reference id") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myOtherStringBuilder)
            Then("The states are equal and join into the same state") {
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound shouldBe a
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("Two states have different reference ids") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(valueFactory.createStringBuilder(StringBuilder(), node1))
            Then("The states are different and join into a typed value") {
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
    }

    Given("States with mixed types") {
        val myStringBuilder = valueFactory.createStringBuilder(StringBuilder(), node)
        val myString = valueFactory.createString("MyString", node)

        When("Two states contain objects with a common ancestor") {
            val a = ValueAbstractState(myStringBuilder)
            val b = ValueAbstractState(myString)
            Then("The states join into a typed value with the ancestor type") {
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
        }

        When("Two states contain objects with a common ancestor (states switched)") {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(myStringBuilder)
            Then("The states join into a typed value with the ancestor type") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe false
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<TypedReferenceValue>()
                (leastUpperBound.value as TypedReferenceValue).type shouldBe "Ljava/io/Serializable;"
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("A state is known and another is unknown") {
            val a = ValueAbstractState(myString)
            val b = ValueAbstractState(UNKNOWN_VALUE)
            Then("The states join into an unknown value") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe true
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<UnknownValue>()
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("A state is known and another is unknown (states switched)") {
            val a = ValueAbstractState(UNKNOWN_VALUE)
            val b = ValueAbstractState(myString)
            Then("The states join into an unknown value") {
                a shouldNotBe b
                a.isLessOrEqual(b) shouldBe false
                val leastUpperBound = a.join(b)
                leastUpperBound.value.shouldBeInstanceOf<UnknownValue>()
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }

        When("A state a reference and the other a primitive type") {
            val a = ValueAbstractState(valueFactory.createString("", node))
            val b = ValueAbstractState(valueFactory.createIntegerValue(0))
            Then("The states join into an unknown value") {
                val leastUpperBound = a.join(b)
                leastUpperBound.value shouldBe UNKNOWN_VALUE
                leastUpperBound.join(a) shouldBe leastUpperBound
                leastUpperBound.join(b) shouldBe leastUpperBound
            }
        }
    }

    Given("An unknown state and a top state") {
        val a = ValueAbstractState(TopValue())
        val b = ValueAbstractState(UNKNOWN_VALUE)
        Then("The states join into an unknown value") {
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe true
            val leastUpperBound = a.join(b)
            leastUpperBound.value shouldBe UNKNOWN_VALUE
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }

    Given("A known and an unknown integer") {
        val a = ValueAbstractState(valueFactory.createIntegerValue())
        val b = ValueAbstractState(valueFactory.createIntegerValue(0))
        Then("The states join into an unknown integer") {
            val leastUpperBound = a.join(b)
            leastUpperBound.value shouldBe UnknownIntegerValue()
            leastUpperBound.join(a) shouldBe leastUpperBound
            leastUpperBound.join(b) shouldBe leastUpperBound
        }
    }
})

private fun ValueFactory.createString(s: String, id: JvmCfaNode): Value = createReferenceValueForId(
    libraryClassPool.getClass("java/lang/String"),
    false,
    false,
    id,
    AnalyzedObjectFactory.createPrecise(s),
)

private fun ValueFactory.createIdentifiedString(id: JvmCfaNode): Value = createReferenceValueForId(
    "Ljava/lang/String;",
    libraryClassPool.getClass("Ljava/lang/String;"),
    false,
    false,
    id,
)

private fun ValueFactory.createStringBuilder(sb: StringBuilder, id: JvmCfaNode): Value = createReferenceValueForId(
    libraryClassPool.getClass("java/lang/StringBuilder"),
    false,
    false,
    id,
    AnalyzedObjectFactory.createPrecise(sb),
)

private fun ValueFactory.createIdentifiedStringBuilder(id: JvmCfaNode): Value = createReferenceValueForId(
    "Ljava/lang/StringBuilder;",
    libraryClassPool.getClass("java/lang/StringBuilder"),
    false,
    false,
    id,
)
