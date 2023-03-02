/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.VersionConstants.CLASS_VERSION_1_8
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.util.ClassUtil
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.IdentifiedReferenceValue
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TypedReferenceValue
import proguard.evaluation.value.UnknownReferenceValue
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.util.MethodWithStack
import proguard.util.PartialEvaluatorHelper

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

            uniqueIDs shouldHaveSize 3
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

            uniqueIDs shouldHaveSize 3
        }
    }

    "Regression test inheritance sanity check" - {
        val charSequenceClass = ClassBuilder(CLASS_VERSION_1_8, PUBLIC, "java/lang/CharSequence", "java/lang/Object").programClass
        val stringClass = ClassBuilder(CLASS_VERSION_1_8, PUBLIC, "java/lang/String", "java/lang/Object").programClass
        charSequenceClass.addSubClass(stringClass)

        "No exception" {
            shouldNotThrowAny {
                ParticularReferenceValue(
                    ClassUtil.internalTypeFromClassName(charSequenceClass.name),
                    charSequenceClass,
                    null,
                    0,
                    ""
                )
            }
        }
    }

    "Load primitive value from static final field" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "A.jbc",
                """
                    version 1.8;
                    public class A extends java.lang.Object [
                        SourceFile "A.java";
                        ] 
                    {
                        public static final int answer = 42;
            
                        public static void staticfield() 
                        {
                            getstatic java.lang.System#java.io.PrintStream out
                            getstatic A#int answer
                            invokevirtual java.io.PrintStream#void println(java.lang.String)
                            return
                        }
                    }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "staticfield", "()V", programClassPool)

        "getstatic loaded int parameter" {
            val value = invocationsWithStack[6]!!.stack[0]
            value.shouldBeInstanceOf<ParticularIntegerValue>()
            value.value() shouldNotBe null
            value.value() shouldBe 42
        }
    }

    "Load reference value from static final field" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "A.jbc",
                """
                    version 1.8;
                    public class A extends java.lang.Object [
                        SourceFile "A.java";
                        ] 
                    {
                        public static final java.lang.String answer = "42";
            
                        public static void staticfield() 
                        {
                            getstatic java.lang.System#java.io.PrintStream out
                            getstatic A#java.lang.String answer
                            invokevirtual java.io.PrintStream#void println(java.lang.String)
                            return
                        }
                    }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "staticfield", "()V", programClassPool)

        "getstatic loaded String parameter" {
            checkExpectedValueParticularReferenceValue(invocationsWithStack, 6, 0, "42")
            (invocationsWithStack[6]!!.stack[0] as ParticularReferenceValue).referencedClass.name shouldBe "java/lang/String"
        }
    }

    "String function returning reference array " - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions()
                    {
                        String str = "42-43";
                        String[] arr = str.split("-");
                        System.out.println(arr);
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "String array evaluated correctly" {
            val value = invocationsWithStack[14]!!.stack[0]
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.type shouldBe "[Ljava/lang/String;"
            value.value() shouldBe arrayOf("42", "43")
        }
    }

    "String function returning primitive array " - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions() throws java.io.UnsupportedEncodingException
                    {
                        String str = "42";
                        byte[] arr = str.getBytes("UTF-8");
                        System.out.println(arr);
                    }
                }
                """
            ),
            javacArguments = listOf("-source", "8", "-target", "8")
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool)

        "Primitive array evaluated correctly" {
            val value = invocationsWithStack[14]!!.stack[0]
            value.shouldBeInstanceOf<ParticularReferenceValue>()
            value.type shouldBe "[B"
            value.value() shouldBe arrayOf(52.toByte(), 50.toByte())
        }
    }

    "Given an identified and a particular value" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
        val stringBuilderClazz = ClassPoolBuilder.libraryClassPool.getClass("java/lang/StringBuilder")
        val identified = IdentifiedReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            false,
            false,
            valueFactory,
            0
        )
        val particular = ParticularReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            valueFactory,
            0,
            StringBuilder()
        )

        "Then identified.generalize(particular) should be identified" {
            val generalized = identified.generalize(particular)
            generalized.shouldBeInstanceOf<IdentifiedReferenceValue>()
            generalized.id shouldBe 0
        }

        "Then particular.generalize(identified) should be identified" {
            val generalized = particular.generalize(identified)
            generalized.shouldBeInstanceOf<IdentifiedReferenceValue>()
            generalized.id shouldBe 0
        }
    }

    "Given two particular values" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
        val stringBuilderClazz = ClassPoolBuilder.libraryClassPool.getClass("java/lang/StringBuilder")
        val stringBuilder = StringBuilder()
        val particular1 = ParticularReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            valueFactory,
            0,
            stringBuilder
        )
        val particular2 = ParticularReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            valueFactory,
            0,
            stringBuilder
        )

        "Then particular1.generalize(particular2) should be particular" {
            val generalized = particular1.generalize(particular2)
            generalized.shouldBeInstanceOf<ParticularReferenceValue>()
            generalized.id shouldBe 0
        }

        "Then particular2.generalize(particular1) should be particular" {
            val generalized = particular2.generalize(particular1)
            generalized.shouldBeInstanceOf<ParticularReferenceValue>()
            generalized.id shouldBe 0
        }
    }

    "Given an identified and a typed value" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
        val stringBuilderClazz = ClassPoolBuilder.libraryClassPool.getClass("java/lang/StringBuilder")
        val identified = IdentifiedReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            false,
            false,
            valueFactory,
            0
        )
        val typed = TypedReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            false,
            false
        )

        "Then identified.generalize(typed) should be typed" {
            val generalized = identified.generalize(typed)
            generalized.shouldBeInstanceOf<TypedReferenceValue>()
        }

        "Then typed.generalize(identified) should be typed" {
            val generalized = typed.generalize(identified)
            generalized.shouldBeInstanceOf<TypedReferenceValue>()
        }
    }

    "Given an identified and a unknown reference" - {
        val valueFactory = ParticularValueFactory(ParticularReferenceValueFactory())
        val stringBuilderClazz = ClassPoolBuilder.libraryClassPool.getClass("java/lang/StringBuilder")
        val identified = IdentifiedReferenceValue(
            "Ljava/lang/StringBuilder;",
            stringBuilderClazz,
            false,
            false,
            valueFactory,
            0
        )
        val unknown = UnknownReferenceValue()

        "Then identified.generalize(typed) should be unknown" {
            val generalized = identified.generalize(unknown)
            generalized.shouldBeInstanceOf<UnknownReferenceValue>()
        }

        "Then typed.generalize(identified) should be unknown" {
            val generalized = unknown.generalize(identified)
            generalized.shouldBeInstanceOf<UnknownReferenceValue>()
        }
    }
})

fun checkExpectedValueParticularReferenceValue(invocationsWithStack: HashMap<Int, MethodWithStack>, instructionOffset: Int, stackPositionFromTop: Int, expected: String?): ParticularReferenceValue {
    val value = invocationsWithStack[instructionOffset]!!.stack[stackPositionFromTop]
    value.shouldBeInstanceOf<ParticularReferenceValue>()
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
