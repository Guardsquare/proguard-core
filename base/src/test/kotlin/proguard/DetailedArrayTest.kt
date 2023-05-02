package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.DetailedArrayReferenceValue
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ValueFactory
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.PartialEvaluatorUtil

class DetailedArrayTest : FreeSpec({
    val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())
    val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory)
    val partialEvaluator = PartialEvaluator(
        valueFactory,
        invocationUnit,
        false
    )

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
            """
            )

            val (classPool, _) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "()V",
                classPool,
                partialEvaluator
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
            """
            )

            val (classPool, _) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "(Z)V",
                classPool,
                partialEvaluator
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
            """
            )

            val (classPool, _) = ClassPoolBuilder.fromSource(code, javacArguments = listOf("-g", "-source", "1.8", "-target", "1.8"))

            val (instructions, variableTable) = PartialEvaluatorUtil.evaluate(
                "Test",
                "arrayTest",
                "(Z)V",
                classPool,
                partialEvaluator
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
    }
})
