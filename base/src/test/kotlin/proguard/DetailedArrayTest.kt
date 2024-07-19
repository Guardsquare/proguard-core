package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValue
import proguard.evaluation.value.DetailedArrayReferenceValue
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.IdentifiedArrayReferenceValue
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.UnknownIntegerValue
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil
import proguard.util.PartialEvaluatorHelper

class DetailedArrayTest : FreeSpec({
    val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(ParticularReferenceValueFactory()), ParticularReferenceValueFactory())

    "Array values evaluated correctly" - {

        "Regression test: future update does not change previous value" {

            val code = JavaSource(
                "Test.java",
                """
            public class Test {

                public void arrayTest() {
                    int[] array = new int[5];
                    array[0] = 4;
                    array[1] = 42;
                    array[1] = 2;
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            /*
                [0] iconst_5
                [1] newarray 10
                [3] astore_1 v1
                [4] aload_1 v1
                [5] iconst_0
                [6] iconst_4
                [7] iastore
                [8] aload_1 v1
                [9] iconst_1
                [10] bipush 42
                [12] iastore
                [13] aload_1 v1
                [14] iconst_1
                [15] iconst_2
                [16] iastore
                [17] return
             */

            val (instruction, _) = instructions[13]
            val s = partialEvaluator.getVariablesBefore(instruction)
                .getValue(variableTable["array"]!!) as DetailedArrayReferenceValue
            s.type shouldBe "[I"
            s.integerArrayLoad(valueFactory.createIntegerValue(1), valueFactory).integerValue().value() shouldBe 42
        }

        "Regression test: array from String with future update does not change previous value" {

            val code = JavaSource(
                "Test.java",
                """
                    public class Test {
        
                        public void arrayTest() {
                            byte[] array = "00000".getBytes();
                            array[0] = 4;
                            array[1] = 42;
                            array[1] = 102;
                        }
                    }
                    """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )
            /*
                [8]  =>  (11, iconst_1)
                [9]  =>  (12, bipush 42)
                [10]  =>  (14, bastore)
                [11]  =>  (15, aload_1 v1)
             */
            val (instruction, _) = instructions[10]
            val s = partialEvaluator.getVariablesAfter(instruction)
                .getValue(variableTable["array"]!!) as DetailedArrayReferenceValue
            s.type shouldBe "[B"
            s.integerArrayLoad(valueFactory.createIntegerValue(1), valueFactory).integerValue()
                .value() shouldBe 42

            val (instructionEnd, _) = instructions.last()
            val sEnd = partialEvaluator.getVariablesAfter(instructionEnd)
                .getValue(variableTable["array"]!!) as DetailedArrayReferenceValue

            sEnd.type shouldBe "[B"
            sEnd.integerArrayLoad(valueFactory.createIntegerValue(0), valueFactory).integerValue().value() shouldBe 4
            sEnd.integerArrayLoad(valueFactory.createIntegerValue(1), valueFactory).integerValue().value() shouldBe 102
        }

        "String array with no assignment in try block" {
            val code = JavaSource(
                "Test.java",
                """
            public class Test {

                public void arrayTest(boolean b) {
                    
                    String[] array = new String[5];
                    try 
                    {
                        if (b)
                        {
                            throw new java.lang.IllegalStateException("woops");
                        }    
                    }
                    catch (java.lang.Exception e)
                    {
                        System.out.println("idc");
                    }
                
                    array = new String[5];
                    array[0] = "4";
                    array[1] = "42";
                    array[1] = "2";
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "(Z)V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["array"]!!)
            s.shouldBeInstanceOf<DetailedArrayReferenceValue>()
            s.type shouldBe "[Ljava/lang/String;"
            s.referenceArrayLoad(valueFactory.createIntegerValue(1), valueFactory).referenceValue().value() shouldBe "2"
        }

        "Regression test: array assignment inside try block".config(enabled = false) {
            val code = JavaSource(
                "Test.java",
                """
            public class Test {

                public void arrayTest(boolean b) {
                    
                    String[] array = new String[5];
                    try 
                    {
                        array = new String[5];
                    
                        if (b)
                        {
                            throw new java.lang.IllegalStateException("woops");
                        }  
                          
                          
                    }
                    catch (java.lang.Exception e)
                    {
                        System.out.println("idc");
                    }
                
                    array = new String[5];
                    array[0] = "4";
                    array[1] = "42";
                    array[1] = "2";
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "(Z)V",
                programClassPool,
                partialEvaluator,
            )

            /*
            Code:
                [0] iconst_5
                [1] anewarray #2 = Class(java/lang/String)
                [4] astore_2 v2
                [5] iconst_5
                [6] anewarray #2 = Class(java/lang/String)
                [9] astore_2 v2
                [10] iload_1 v1
                [11] ifeq +13 (target=24)
                [14] new #3 = Class(java/lang/IllegalStateException)
                [17] dup
                [18] ldc #4 = String("woops")
                [20] invokespecial #5 = Methodref(java/lang/IllegalStateException.<init>(Ljava/lang/String;)V)
                [23] athrow
                [24] goto +12 (target=36)
                [27] astore_3 v3
                [28] getstatic #7 = Fieldref(java/lang/System.out Ljava/io/PrintStream;)
                [31] ldc #8 = String("idc")
                [33] invokevirtual #9 = Methodref(java/io/PrintStream.println(Ljava/lang/String;)V)
                [36] iconst_5
                [37] anewarray #2 = Class(java/lang/String)
                [40] astore_2 v2
                [41] aload_2 v2
                [42] iconst_0
                [43] ldc #10 = String("4")
                [45] aastore
                [46] aload_2 v2
                [47] iconst_1
                [48] ldc #11 = String("42")
                [50] aastore
                [51] aload_2 v2
                [52] iconst_1
                [53] ldc #12 = String("2")
                [55] aastore
                [56] return

            Exception table:
                5 -> 24: 27
             */

            val (instruction, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["array"]!!)
            s.shouldBeInstanceOf<DetailedArrayReferenceValue>()
            s.type shouldBe "[Ljava/lang/String;"
            s.referenceArrayLoad(valueFactory.createIntegerValue(1), valueFactory).referenceValue().value() shouldBe "2"
        }

        "Array value unknown when there are possible paths where index is out of bound" {
            val code = JavaSource(
                "Test.java",
                """
            public class Test {
    
                public void arrayTest() {
                
                    int index = 1;
                    if (gimmeFive() == 5) {
                        index = 3;
                    }
                    
                    int[] array = new int[index];
                    array[2] = 42;
                }
                
                public int gimmeFive()
                {
                    return 5;
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(instruction)
                .getValue(variableTable["array"]!!) as ArrayReferenceValue
            s.type shouldBe "[I"
            s shouldNot beInstanceOf<DetailedArrayReferenceValue>()
        }

        "Array value unknown when index out of bounds" {
            val code = JavaSource(
                "Test.java",
                """
            public class Test {
    
                public void arrayTest() {
                    int[] array = new int[1];
                    array[2] = 42;
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(instruction)
                .getValue(variableTable["array"]!!) as ArrayReferenceValue
            s.type shouldBe "[I"
            s shouldNot beInstanceOf<DetailedArrayReferenceValue>()
        }

        "Load array out of bounds" {
            val code = JavaSource(
                "Test.java",
                """
            public class Test {
    
                public void arrayTest() {
                    int[] array = new int[1];
                    int number = array[2];
                }
            }
            """,
            )

            val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val s = partialEvaluator.getVariablesBefore(instruction)
                .getValue(variableTable["array"]!!) as ArrayReferenceValue
            s.type shouldBe "[I"
            s shouldNot beInstanceOf<DetailedArrayReferenceValue>()

            val number = partialEvaluator.getVariablesBefore(instruction).getValue(variableTable["number"]!!)
            number shouldBe UnknownIntegerValue()
        }
    }

    "String function returning primitive array" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {
                    public void functions() throws java.io.UnsupportedEncodingException
                    {
                        String str = "42";
                        
                        byte[] byteArray = str.getBytes("UTF-8");
                        System.out.println(byteArray);
                        
                        char[] charArray = str.toCharArray();
                        System.out.println(charArray);
                    }
                }
                """,
            ),
            javacArguments = listOf("-source", "8", "-target", "8"),
        )

        val invocationsWithStack = PartialEvaluatorHelper.evaluateMethod("A", "functions", "()V", programClassPool, libraryClassPool, DetailedArrayValueFactory(ParticularReferenceValueFactory()))

        "Primitive byte array evaluated correctly" {
            val value = invocationsWithStack[14]!!.stack[0]
            value.shouldBeInstanceOf<DetailedArrayReferenceValue>()
            value.type shouldBe "[B"
            value.value() shouldBe arrayOf(ParticularIntegerValue(52), ParticularIntegerValue(50))
        }

        "Primitive char array evaluated correctly" {
            val value = invocationsWithStack[26]!!.stack[0]
            value.shouldBeInstanceOf<DetailedArrayReferenceValue>()
            value.type shouldBe "[C"
            value.value() shouldBe arrayOf(ParticularIntegerValue('4'.code), ParticularIntegerValue('2'.code))
        }
    }

    "Array reference generalization" - {
        val code = JavaSource(
            "Test.java",
            """
        import java.util.Random;

        public class Test {
        
            Random r = new Random();

            public void arrayReferenceTest() {
                byte[] array = {0, 1, 2, 3, 4, 5};
                array[1] = 11;
                if (r.nextInt() > 5) {
                    array[5] = 55;
                }
                array[2] = 22;
            }
            
            public void arrayReferenceRedefinedTest() {
                byte[] array = {0, 1, 2, 3, 4, 5};
                array[1] = 11;
                if (r.nextInt() > 5) {
                    array = new byte[]{5, 4, 3, 2, 1, 0};
                }
                array[2] = 22;
            }
        }
        """,
        )

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

        val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory, libraryClassPool)
        val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, false)

        "IdentifiedArrayReference generalize maintains id" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayReferenceTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (aastoreInstruction, _) = instructions[26] // aastore
            val firstArray = partialEvaluator
                .getVariablesAfter(aastoreInstruction).getValue(variableTable["array"]!!)
            firstArray.shouldBeInstanceOf<IdentifiedArrayReferenceValue>()

            val (finalInstruction, _) = instructions.last()
            val finalArray = partialEvaluator
                .getVariablesAfter(finalInstruction).getValue(variableTable["array"]!!)
            finalArray.shouldBeInstanceOf<IdentifiedArrayReferenceValue>()

            finalArray.id shouldBe firstArray.id
            finalArray.type shouldBe "[B"
        }

        "IdentifiedArrayReference redefined in branch generalize looses id" {
            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayReferenceRedefinedTest",
                "()V",
                programClassPool,
                partialEvaluator,
            )

            val (instruction, _) = instructions.last()
            val array = partialEvaluator.getVariablesAfter(instruction).getValue(variableTable["array"]!!)
            array.shouldNotBeInstanceOf<IdentifiedArrayReferenceValue>()
            array.shouldBeInstanceOf<ArrayReferenceValue>()
            array.type shouldBe "[B"
        }
    }
})
