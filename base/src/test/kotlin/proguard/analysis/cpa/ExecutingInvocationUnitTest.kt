package proguard.analysis.cpa

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING
import proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUILDER
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.util.ClassUtil
import proguard.classfile.visitor.AllMethodVisitor
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.executor.MethodExecutionInfo
import proguard.evaluation.executor.StringReflectionExecutor
import proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ReferenceValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownIntegerValue
import proguard.evaluation.value.UnknownReferenceValue
import proguard.evaluation.value.Value
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.ClassPoolBuilder.Companion.libraryClassPool
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil
import proguard.testutils.findMethod
import java.lang.ClassCastException

private val javaLangString = libraryClassPool.getClass("java/lang/String")
private val javaLangStringBuilder = libraryClassPool.getClass("java/lang/StringBuilder")
private val valueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())
private val stringExecutor = StringReflectionExecutor()
private val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory)
private fun Int.toValue(): Value =
    valueFactory.createIntegerValue(this.toInt())

private fun Any?.toValue(id: Int? = null): Value = when (this) {
    null -> valueFactory.createReferenceValueNull()
    else -> when (id) {
        null -> valueFactory.createReferenceValue(libraryClassPool.getClass(ClassUtil.internalClassName(this.javaClass.canonicalName)), this)
        else -> valueFactory.createReferenceValueForId(
            ClassUtil.internalTypeFromClassName(ClassUtil.internalClassName(this.javaClass.canonicalName)),
            libraryClassPool.getClass(ClassUtil.internalClassName(this.javaClass.canonicalName)),
            true,
            true,
            id,
            this,
        )
    }
}
private fun UnknownString(): ReferenceValue =
    valueFactory.createReferenceValue("Ljava/lang/String;", javaLangString, false, false)

class ExecutingInvocationUnitTest : FreeSpec({
    "String method tests" - {
        val length = MethodExecutionInfo(javaLangString, javaLangString.findMethod("length"), null)
        val concat = MethodExecutionInfo(javaLangString, javaLangString.findMethod("concat"), null)
        "Unknown reference String length" {
            invocationUnit.executeMethod(stringExecutor, length, UnknownReferenceValue()) shouldBe UnknownIntegerValue()
            invocationUnit.executeMethod(stringExecutor, length, UnknownString()) shouldBe UnknownIntegerValue()
        }

        "Unknown String length" {
            invocationUnit.executeMethod(stringExecutor, length, UNKNOWN_VALUE) shouldBe UnknownIntegerValue()
            invocationUnit.executeMethod(stringExecutor, length, UnknownString()) shouldBe UnknownIntegerValue()
        }

        "Particular string length" {
            invocationUnit.executeMethod(stringExecutor, length, "Hello".toValue()) shouldBe 5.toValue()
        }

        "Concat Hello with World" {
            val result = invocationUnit.executeMethod(stringExecutor, concat, "Hello".toValue(), " World".toValue())
            result.shouldBeInstanceOf<ParticularReferenceValue>()
            result.referenceValue().value() shouldBe "Hello World"
        }

        "Concat Hello with unknown string" {
            val result = invocationUnit.executeMethod(stringExecutor, concat, "Hello".toValue(), UnknownString())
            result.shouldBeInstanceOf<TypedReferenceValue>()
            result.shouldNotBeInstanceOf<ParticularReferenceValue>()
            result.internalType() shouldBe TYPE_JAVA_LANG_STRING
        }

        "Static valueOf(I)" {
            val valueOf = MethodExecutionInfo(javaLangString, javaLangString.findMethod("valueOf", "(I)Ljava/lang/String;"), null)
            val result = invocationUnit.executeMethod(stringExecutor, valueOf, 1.toValue())
            result.shouldBeInstanceOf<ParticularReferenceValue>()
            result.internalType() shouldBe TYPE_JAVA_LANG_STRING
            result.value() shouldBe "1"
        }
    }

    "StringBuilder tests" - {
        val append = MethodExecutionInfo(javaLangStringBuilder, javaLangStringBuilder.findMethod("append"), null)
        val toString = MethodExecutionInfo(javaLangStringBuilder, javaLangStringBuilder.findMethod("toString"), null)
        val length = MethodExecutionInfo(javaLangStringBuilder, javaLangStringBuilder.findMethod("length"), null)
        val substring = MethodExecutionInfo(javaLangStringBuilder, javaLangStringBuilder.findMethod("substring", "(I)Ljava/lang/String;"), null)

        "StringBuilder multiple appends to same ID" {
            val stringBuilder = StringBuilder().toValue() as ParticularReferenceValue
            val result = invocationUnit.executeMethod(stringExecutor, append, stringBuilder, "Hello".toValue())

            result.shouldBeInstanceOf<ParticularReferenceValue>()
            result.id shouldBe stringBuilder.id
            result.value().toString() shouldBe "Hello"

            val result2 = invocationUnit.executeMethod(stringExecutor, append, result.value().toValue(stringBuilder.id as Int), " World".toValue())
            result2.shouldBeInstanceOf<ParticularReferenceValue>()
            result2.internalType() shouldBe TYPE_JAVA_LANG_STRING_BUILDER
            result2.id shouldBe stringBuilder.id

            val result3 = invocationUnit.executeMethod(stringExecutor, toString, result2.value().toValue(stringBuilder.id as Int))
            result3.shouldBeInstanceOf<ParticularReferenceValue>()
            result3.id shouldNotBe stringBuilder.id
            result3.value().shouldBeInstanceOf<String>()
            result3.value() shouldBe "Hello World"
        }

        "StringBuilder with unknown instance calling append" {
            val stringBuilder = valueFactory.createReferenceValue(TYPE_JAVA_LANG_STRING_BUILDER, javaLangStringBuilder, false, false) as IdentifiedReferenceValue
            val result = invocationUnit.executeMethod(stringExecutor, append, stringBuilder, "Hello".toValue())

            result.shouldNotBeInstanceOf<ParticularReferenceValue>()
            result.shouldBeInstanceOf<IdentifiedReferenceValue>()
            result.internalType() shouldBe TYPE_JAVA_LANG_STRING_BUILDER
            // The same ID should be returned
            result.id shouldBe stringBuilder.id
            result.value() shouldBe null
        }

        "StringBuilder with unknown instance calling length" {
            val stringBuilder = valueFactory.createReferenceValue(TYPE_JAVA_LANG_STRING_BUILDER, javaLangStringBuilder, false, false) as IdentifiedReferenceValue
            val result = invocationUnit.executeMethod(stringExecutor, length, stringBuilder)

            result.shouldNotBeInstanceOf<ParticularReferenceValue>()
            result.shouldBeInstanceOf<UnknownIntegerValue>()
            result.internalType() shouldBe "I"
        }

        "StringBuilder with unknown instance calling substring" {
            val stringBuilder = valueFactory.createReferenceValue(TYPE_JAVA_LANG_STRING_BUILDER, javaLangStringBuilder, false, false) as IdentifiedReferenceValue
            val result = invocationUnit.executeMethod(stringExecutor, substring, stringBuilder, 1.toValue())

            result.shouldNotBeInstanceOf<ParticularReferenceValue>()
            result.shouldBeInstanceOf<IdentifiedReferenceValue>()
            result.internalType() shouldBe TYPE_JAVA_LANG_STRING
            // The same ID should be not returned
            result.id shouldNotBe stringBuilder.id
            result.value() shouldBe null
        }

        "StringBuilder with unknown parameter" {
            val stringBuilder = StringBuilder().toValue() as ParticularReferenceValue
            val result = invocationUnit.executeMethod(stringExecutor, append, stringBuilder, UNKNOWN_VALUE)

            result.shouldBeInstanceOf<IdentifiedReferenceValue>()
            result.id shouldBe stringBuilder.id
            result.internalType() shouldBe TYPE_JAVA_LANG_STRING_BUILDER
        }
    }

    "Test String static method valueOf(I) with the PartialEvaluator" {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                import java.math.BigDecimal;
                public class Test {
                    public static String test() {
                        return String.valueOf(1);       
                    }
                }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "8", "-target", "8"),
        )

        val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)

        programClassPool.classAccept("Test", AllMethodVisitor(AllAttributeVisitor(partialEvaluator)))
        val stackTop = partialEvaluator.getStackAfter(1).getTop(0)
        stackTop.shouldBeInstanceOf<ParticularReferenceValue>()
        stackTop.value() shouldBe "1"
    }

    "Test String static method join and format" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                    public class Test {
                        public void test() {
                            String a = String.join(", ", new String[] {"Hello", "World!"});
                            String b = String.join("", "Foo", "Bar");
                            
                            String c = String.format("Hello, %s!", "World");
                        }
                    }
                """.trimIndent(),
            ),
            javacArguments = listOf("-g", "-source", "8", "-target", "8"),
        )

        val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

        val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
            "Test",
            "test",
            "()V",
            programClassPool,
            partialEvaluator,
        )

        val (instruction, _) = instructions.last()

        "Test String static method join" {
            with(partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["a"]!!)) {
                shouldBeInstanceOf<ParticularReferenceValue>()
                internalType() shouldBe TYPE_JAVA_LANG_STRING
                value() shouldBe "Hello, World!"
            }

            with(partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["b"]!!)) {
                shouldBeInstanceOf<ParticularReferenceValue>()
                internalType() shouldBe TYPE_JAVA_LANG_STRING
                value() shouldBe "FooBar"
            }
        }

        "Test String static method format" {
            with(partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["c"]!!)) {
                shouldBeInstanceOf<ParticularReferenceValue>()
                internalType() shouldBe TYPE_JAVA_LANG_STRING
                value() shouldBe "Hello, World!"
            }
        }
    }

    "Check resilience against incorrect instance types" - {
        val stringBufferClazz = libraryClassPool.getClass("java/lang/StringBuffer")
        val executionInfo = MethodExecutionInfo(stringBufferClazz, stringBufferClazz.findMethod("toString"), null)
        val stringBuilderValue = valueFactory.createReferenceValue(TYPE_JAVA_LANG_STRING_BUILDER, javaLangStringBuilder, false, false) as IdentifiedReferenceValue

        "No class cast exception for incorrect instance type" {
            shouldNotThrow<ClassCastException> {
                invocationUnit.executeMethod(stringExecutor, executionInfo, stringBuilderValue)
            }
        }
    }
})
