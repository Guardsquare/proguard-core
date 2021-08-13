/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.TypedReferenceValue
import proguard.util.MethodWithStack
import proguard.util.PartialEvaluatorHelper
import testutils.ClassPoolBuilder
import testutils.JavaSource

class ParticularReferenceTest : FreeSpec({

    "Simple empty value in Constructor" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void ctor(){
                        String s = new String();
                        System.out.println(s);
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "ctor", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 2
            invocationsWithStack.keys shouldContain 4
            invocationsWithStack.keys shouldContain 12
        }

        "Correct parameter in Constructor" {
            invocationsWithStack[4]!!.methodSignature shouldBe "java/lang/String.<init>.()V"
            if (invocationsWithStack[4]!!.stack[0] !is TypedReferenceValue) { // we lose all information at the generalization
                throw AssertionError("Unexpected type: " + invocationsWithStack[4]!!.stack[0].javaClass.simpleName)
            }

            (invocationsWithStack[4]!!.stack[0] as TypedReferenceValue).referencedClass shouldNotBe null
            (invocationsWithStack[4]!!.stack[0] as TypedReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }

        "Correct parameter in last sysout" {
            invocationsWithStack[12]!!.methodSignature shouldBe "java/io/PrintStream.println.(Ljava/lang/String;)V"
            if (invocationsWithStack[12]!!.stack[0] !is TypedReferenceValue) { // we lose all information at the generalization
                throw AssertionError("Unexpected type: " + invocationsWithStack[12]!!.stack[0].javaClass.simpleName)
            }

            (invocationsWithStack[12]!!.stack[0] as TypedReferenceValue).referencedClass shouldNotBe null
            (invocationsWithStack[12]!!.stack[0] as TypedReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }
    }

    "Unknown value if different possibilities exist - loop" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void loop(){
                        StringBuilder sb = new StringBuilder();
                        long t = 0;
                        while (t < System.currentTimeMillis()){
                            sb.append("x");
                        }
                        System.out.println(sb.toString());
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "loop", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 5
            invocationsWithStack.keys shouldContain 35
        }

        "Correct parameter in last sysout" {
            invocationsWithStack[35]!!.methodSignature shouldBe "java/io/PrintStream.println.(Ljava/lang/String;)V"
            if (invocationsWithStack[35]!!.stack[0] !is TypedReferenceValue) { // we lose all information at the generalization
                throw AssertionError("Unexpected type: " + invocationsWithStack[35]!!.stack[0].javaClass.simpleName)
            }

            (invocationsWithStack[35]!!.stack[0] as TypedReferenceValue).referencedClass shouldNotBe null
            (invocationsWithStack[35]!!.stack[0] as TypedReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }
    }

    "Unknown value if different possibilities exist - branch" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void branch(int i){
                        StringBuilder sb = new StringBuilder();
                        if (i == 0)
                        {
                            sb.append("a");
                        }
                        else
                        {
                            sb.append("x");
                        }
                        System.out.println(sb.toString());
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "branch", "(I)V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 5
            invocationsWithStack.keys shouldContain 36
        }

        "Correct parameter in last sysout" {
            invocationsWithStack[36]!!.methodSignature shouldBe "java/io/PrintStream.println.(Ljava/lang/String;)V"
            invocationsWithStack[36]!!.stack[0].isParticular shouldBe false

            (invocationsWithStack[36]!!.stack[0] as TypedReferenceValue).referencedClass shouldNotBe null
            (invocationsWithStack[36]!!.stack[0] as TypedReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }
    }

    "Unknown value for non-final field" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public String SOME_STRING = "ASDF";
                    public void field(){
                        StringBuilder sb = new StringBuilder(SOME_STRING);
                        System.out.println(sb.toString());
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "field", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 3
            invocationsWithStack.keys shouldContain 19
        }

        "Correct parameter in last sysout" {
            invocationsWithStack[19]!!.methodSignature shouldBe "java/io/PrintStream.println.(Ljava/lang/String;)V"
            if (invocationsWithStack[19]!!.stack[0] !is IdentifiedReferenceValue) {
                throw AssertionError("Unexpected type: " + invocationsWithStack[36]!!.stack[0].javaClass.simpleName)
            }

            (invocationsWithStack[19]!!.stack[0] as TypedReferenceValue).referencedClass shouldNotBe null
            (invocationsWithStack[19]!!.stack[0] as TypedReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }
    }

    "Simple usage" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                    class A {
                        public void append()
                        {
                            System.out.println("StringValue");
                        }
                    }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "append", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 1
            invocationsWithStack.keys shouldContain 5
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 5, 0, "StringValue")
        }
    }

    "Simple concat" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void append()
                    {
                        String s = "asd";
                        s += "fgh";
                        System.out.println(s);
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "append", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 5
            invocationsWithStack.keys shouldContain 27
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 27, 0, "asdfgh")
        }
    }

    "Simple append" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void append()
                    {
                        StringBuilder sb;
                        sb = new StringBuilder("asd");
                        sb.append("fgh");
                        System.out.println(sb.toString());
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "append", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 4
            invocationsWithStack.keys shouldContain 24
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 24, 0, "asdfgh")
        }
    }

    "Append length" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void stringLength()
                    {
                        String s = "123";
                        StringBuilder sb = new StringBuilder();
                        sb.append("asdf");
                        sb.append(s.length());
                        System.out.println(sb.toString());
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "stringLength", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 6
            invocationsWithStack.keys shouldContain 34
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 34, 0, "asdf3")
        }
    }

    "Simple String concat" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void concat()
                    {
                        String a = "hello";
                        String b = " ";
                        String c = "world";
                        System.out.println(a + b + c);
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "concat", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 6
            invocationsWithStack.keys shouldContain 34
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 34, 0, "hello world")
        }
    }
    "Static final field" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public static final String SECRET = " world";
                    public void concat()
                    {
                        String a = "hello";
                        System.out.println(a + SECRET);
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "concat", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 5
            invocationsWithStack.keys shouldContain 25
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 25, 0, "hello world")
        }
    }

    "StringBuilder functions" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions()
                    {
                        StringBuilder sb = new StringBuilder("Xhello world");
                        sb.append("! ");
                        sb.append(sb.length());
                        sb.deleteCharAt(0);
                        sb.reverse();
                        System.out.println(sb.toString());
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 8
            invocationsWithStack.keys shouldContain 44
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 44, 0, "41 !dlrow olleh")
        }
    }

    "StringBuffer functions" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions()
                    {
                        StringBuffer sb = new StringBuffer("Xhello world");
                        sb.append("! ");
                        sb.append(sb.length());
                        sb.deleteCharAt(0);
                        sb.reverse();
                        System.out.println(sb.toString());
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 8
            invocationsWithStack.keys shouldContain 44
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 44, 0, "41 !dlrow olleh")
        }
    }

    "String functions" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public final String REGEXP = "[0-9]+";

                    public void functions()
                    {
                        String s = "1ThXrX2is3nx4";
                        s = s.concat("spxxn!56 7");
                        s = s.replace('x', 'o');
                        s = s.toLowerCase();
                        s = s.replace('x', 'e');
                        s = s.replaceAll(REGEXP, " ");
                        s = s.trim();
                        System.out.println(s);
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 7
            invocationsWithStack.keys shouldContain 51
        }

        "Correct parameter in last sysout" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 51, 0, "there is no spoon!")
        }
    }

    "Failed casting" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void cast()
                    {
                        Object o = new Integer(1);
                        System.out.println((String) o);
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "cast", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 2
            invocationsWithStack.keys shouldContain 16
        }

        "Correct parameter in last sysout" {
            val value = invocationsWithStack[16]!!.stack[0]
            if (value !is IdentifiedReferenceValue) {
                throw AssertionError("Unexpected type: " + value.javaClass.simpleName)
            }
        }
    }

    "Correct internal stack state after partial evaluation - StringBuilder" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions()
                    {
                        StringBuilder sb = new StringBuilder("Xhello world");
                        sb.append("! ");
                        sb.append(sb.length());
                        sb.deleteCharAt(0);
                        sb.reverse();
                        System.out.println(sb.toString());
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 8

            invocationsWithStack.keys shouldContain 6
            invocationsWithStack.keys shouldContain 13
            invocationsWithStack.keys shouldContain 19
            invocationsWithStack.keys shouldContain 22
            invocationsWithStack.keys shouldContain 28
            invocationsWithStack.keys shouldContain 33
            invocationsWithStack.keys shouldContain 41
            invocationsWithStack.keys shouldContain 44
        }

        "Correct parameters" {
            val uniqueIDs = HashSet<Any>()

            var value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 6, 0, "Xhello world")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 13, 0, "! ")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 13, 1, "Xhello world")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 19, 0, "Xhello world! ")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            checkExpectedValueParticularReferenceValue(invocationsWithStack, 22, 1, "Xhello world! ")

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 28, 1, "Xhello world! 14")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 33, 0, "hello world! 14")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 41, 0, "41 !dlrow olleh")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            checkExpectedValueParticularReferenceValue(invocationsWithStack, 44, 0, "41 !dlrow olleh")

            uniqueIDs shouldHaveSize 7
        }
    }

    "Correct internal stack state after partial evaluation - StringBuffer" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions()
                    {
                        StringBuffer sb = new StringBuffer("Xhello world");
                        sb.append("! ");
                        sb.append(sb.length());
                        sb.deleteCharAt(0);
                        sb.reverse();
                        System.out.println(sb.toString());
                    }

                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Expected number of instructions" {
            invocationsWithStack.keys shouldHaveSize 8

            invocationsWithStack.keys shouldContain 6
            invocationsWithStack.keys shouldContain 13
            invocationsWithStack.keys shouldContain 19
            invocationsWithStack.keys shouldContain 22
            invocationsWithStack.keys shouldContain 28
            invocationsWithStack.keys shouldContain 33
            invocationsWithStack.keys shouldContain 41
            invocationsWithStack.keys shouldContain 44
        }

        "Correct parameters" {
            val uniqueIDs = HashSet<Any>()

            var value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 6, 0, "Xhello world")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 13, 0, "! ")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 13, 1, "Xhello world")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 19, 0, "Xhello world! ")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            checkExpectedValueParticularReferenceValue(invocationsWithStack, 22, 1, "Xhello world! ")

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 28, 1, "Xhello world! 14")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 33, 0, "hello world! 14")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            value = checkExpectedValueParticularReferenceValue(invocationsWithStack, 41, 0, "41 !dlrow olleh")
            uniqueIDs += getIdentifiedReferenceValueID(value)

            checkExpectedValueParticularReferenceValue(invocationsWithStack, 44, 0, "41 !dlrow olleh")

            uniqueIDs shouldHaveSize 7
        }
    }
})

fun checkExpectedValueParticularReferenceValue(invocationsWithStack: HashMap<Int, MethodWithStack>, instructionOffset: Int, stackPositionFromTop: Int, expected: String?): ParticularReferenceValue {
    val value = invocationsWithStack[instructionOffset]!!.stack[stackPositionFromTop]
    if (value !is ParticularReferenceValue) {
        throw AssertionError("Unexpected type: " + value.javaClass.simpleName)
    }
    if (expected != null) {
        value.value() shouldNotBe null
        value.value().toString() shouldBe expected
    } else {
        value.value() shouldBe null
    }
    return value
}

fun getIdentifiedReferenceValueID(value: IdentifiedReferenceValue): Int {
    val field = IdentifiedReferenceValue::class.java.getDeclaredField("id")

    field.isAccessible = true
    return field.get(value) as Int
}
