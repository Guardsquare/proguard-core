package proguard.analysis.cpa.jvm.domain.value

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.evaluation.value.IdentifiedValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ParticularValueFactory.ReferenceValueFactory
import proguard.evaluation.value.Value
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool

class JvmValueAbstractStateTest : FreeSpec({

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
            a.join(b) shouldBe JvmValueAbstractState.top
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
            a.join(b) shouldBe JvmValueAbstractState.top
        }

        "Two abstract states with different instances but equal strings should not be equal" {
            val a = JvmValueAbstractState(myString)
            val b = JvmValueAbstractState(valueFactory.createString("myString"))
            a shouldNotBe b
            a.isLessOrEqual(b) shouldBe false
            a.join(b) shouldBe JvmValueAbstractState.top
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
            a.join(b) shouldBe JvmValueAbstractState.top
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
