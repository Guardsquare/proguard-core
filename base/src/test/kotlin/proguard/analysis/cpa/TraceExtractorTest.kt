package proguard.analysis.cpa

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmMethodTaintSource
import proguard.analysis.cpa.jvm.domain.taint.JvmReturnTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmStaticFieldTaintSource
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.cpa.util.TaintAnalyzer
import proguard.classfile.FieldSignature
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class TraceExtractorTest : BehaviorSpec({

    val taintSourceReturn1 = JvmMethodTaintSource(
        MethodSignature("A", "source1", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSourceReturn2 = JvmMethodTaintSource(
        MethodSignature("A", "source2", "()Ljava/lang/String;"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSourceStatic = JvmMethodTaintSource(
        MethodSignature("A", "source", "()V"),
        false,
        false,
        setOf(),
        setOf("A.staticSourceField:Ljava/lang/String;"),
    )
    val taintSourceFieldRead = JvmStaticFieldTaintSource(FieldSignature("A", "staticSourceField", "Ljava/lang/String;"))
    val taintSourceReturnDouble = JvmMethodTaintSource(
        MethodSignature("A", "source", "()D"),
        false,
        true,
        setOf(),
        setOf(),
    )
    val taintSinkArgument = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(Ljava/lang/String;)V"),
        false,
        setOf(1),
        setOf(),
    )
    val taintSinkArgumentLong = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(J)V"),
        false,
        setOf(1),
        setOf(),
    )
    val taintSinkArgumentDouble = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(D)V"),
        false,
        setOf(1),
        setOf(),
    )
    val taintSinkArgumentMultiple = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "(Ljava/lang/String;Ljava/lang/String;)V"),
        false,
        setOf(2),
        setOf(),
    )
    val taintSinkStatic = JvmInvokeTaintSink(
        MethodSignature("A", "sink", "()V"),
        false,
        setOf(),
        setOf("A.staticSourceField:Ljava/lang/String;"),
    )
    val taintSinkReturn = JvmReturnTaintSink(MethodSignature("A", "sink", "(Ljava/lang/String;)Ljava/lang/String;"))

    Given("A main method passing a callee's source method result to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            sink(callee());
                        }
                    
                        public static String callee()
                        {
                            return source1();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:3",
                        "JvmStackLocation(0)@LA;callee()Ljava/lang/String;:3",
                    ),
                )
            }
        }
    }

    Given("A callee passing a nested callee's source method result to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            callee1();
                        }
                    
                        public static void callee1()
                        {
                            sink(callee2());
                        }
                    
                        public static String callee2()
                        {
                            return source1();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;callee1()V:3",
                        "JvmStackLocation(0)@LA;callee2()Ljava/lang/String;:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing two source method results through a callee to a sink, two returning sources, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            callee(source1());
                            callee(source2());
                        }
                    
                        public static void callee(String s)
                        {
                            sink(s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public static String source2()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(
                interproceduralCfa,
                setOf(taintSourceReturn1, taintSourceReturn2),
                setOf(taintSinkArgument),
            )
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
                [3] invokestatic #3 = Methodref(A.callee(Ljava/lang/String;)V)
                [6] invokestatic #4 = Methodref(A.source2()Ljava/lang/String;)
                [9] invokestatic #3 = Methodref(A.callee(Ljava/lang/String;)V)
                [12] return

             Bytecode of callee:
                 [0] aload_0 v0
                 [1] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
                 [4] return
            */

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1",
                        "JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                    listOf(
                        "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)V:1",
                        "JvmLocalVariableLocation(0)@LA;callee(Ljava/lang/String;)V:0",
                        "JvmStackLocation(0)@LA;main()V:9",
                    ),
                )
            }
        }
    }

    Given("A main method calling a sink reading a static field tainted in a callee, a method source tainting the field, and a static field sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String staticSourceField;
                    
                        public void main() {
                            callee();
                            sink();
                        }
                    
                        public static void callee()
                        {
                            source();
                        }
                    
                        public static void sink()
                        {
                            return;
                        }
                    
                        public static void source()
                        {
                            return;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceStatic), setOf(taintSinkStatic))
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:3",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;callee()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a static field written with a source method result in a callee to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String staticSourceField;
                    
                        public void main() {
                            callee();
                            sink(staticSourceField);
                        }
                    
                        public static void callee()
                        {
                            staticSourceField = source1();
                        }
                    
                        public static void sink(String s)
                        {
                            return;
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:6",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:3",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;callee()V:6",
                        "JvmStackLocation(0)@LA;callee()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a static field read stored in a local across an intermediate call to a sink, a static field source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String staticSourceField;

                        public void main() {
                            String t = staticSourceField;
                            foo();
                            sink(t);
                        }

                        public static void foo()
                        {
                        }

                        public static void sink(String s)
                        {
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceFieldRead), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] getstatic #7 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [3] astore_1 v1
                [4] invokestatic #13 = Methodref(A.foo()V)
                [7] aload_1 v1
                [8] invokestatic #16 = Methodref(A.sink(Ljava/lang/String;)V)
                [11] return
             */

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:8",
                        "JvmLocalVariableLocation(1)@LA;main()V:7",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a static field written with a source method result in a callee to a sink, a static field source, and an argument sink") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                class A {

                    public static String staticSourceField;

                    public void main() {
                        callee();
                        sink(staticSourceField);
                    }

                    public static void callee()
                    {
                        staticSourceField = source1();
                    }

                    public static void sink(String s)
                    {
                    }

                    public static String source1()
                    {
                        return null;
                    }
                }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        )

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool)
            val mainSignature =
                interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }
                    .findFirst().get().signature
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceFieldRead), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] invokestatic #7 = Methodref(A.callee()V)
                [3] getstatic #12 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [6] invokestatic #16 = Methodref(A.sink(Ljava/lang/String;)V)
                [9] return

            Bytecode of callee:
                [0] invokestatic #20 = Methodref(A.source1()Ljava/lang/String;)
                [3] putstatic #12 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [6] return
             */

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:6",
                    ),
                )
            }
        }

        When("TaintAnalyzer analyzes taints with an additional returning source and reconstructs traces") {
            val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool)
            val mainSignature =
                interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }
                    .findFirst().get().signature
            val taintAnalyzer = TaintAnalyzer.Builder(
                interproceduralCfa,
                setOf(taintSourceFieldRead, taintSourceReturn1),
                setOf(taintSinkArgument),
            )
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] invokestatic #7 = Methodref(A.callee()V)
                [3] getstatic #12 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [6] invokestatic #16 = Methodref(A.sink(Ljava/lang/String;)V)
                [9] return

            Bytecode of callee:
                [0] invokestatic #20 = Methodref(A.source1()Ljava/lang/String;)
                [3] putstatic #12 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [6] return
             */

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:6",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:3",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;callee()V:6",
                        "JvmStackLocation(0)@LA;callee()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method calling a library method between a source method call and a sink call, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            System.out.println();
                            sink(s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:11",
                        "JvmLocalVariableLocation(1)@LA;main()V:10",
                        "JvmLocalVariableLocation(1)@LA;main()V:7",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method calling an intermediate method between a source method call and a sink call, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            foo();
                            sink(s);
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                        
                        public static void foo()
                        {
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:8",
                        "JvmLocalVariableLocation(1)@LA;main()V:7",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method assigning null to an unused local between a source method call and a sink call, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s1 = source1(); // out offset: 3
                            String s2 = null;
                            sink(s1); // in offset: 7
                    
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:7",
                        "JvmLocalVariableLocation(1)@LA;main()V:6",
                        "JvmLocalVariableLocation(1)@LA;main()V:5",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a source method result length to a sink, a returning source, and a long argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(s.length());
                        }
                    
                        public static void sink(long l)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgumentLong))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:9",
                        "JvmStackLocation(0)@LA;main()V:8",
                        "JvmStackLocation(0)@LA;main()V:5",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a source method result as the second argument of a two-argument sink, a returning source, and a two-argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(null, s);
                        }
                    
                        public static void sink(String s1, String s2)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgumentMultiple))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:6",
                        "JvmLocalVariableLocation(1)@LA;main()V:5",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a ternary between two callees calling a source method to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main(boolean b) {
                            sink(b ? getSource1() : getSource2());
                        }
                    
                        public static String getSource1()
                        {
                            return source1();
                        }
                    
                        public static String getSource2()
                        {
                            return source1();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] iload_1 v1
                [1] ifeq +9 (target=10)
                [4] invokestatic #2 = Methodref(A.getSource1()Ljava/lang/String;)
                [7] goto +6 (target=13)
                [10] invokestatic #3 = Methodref(A.getSource2()Ljava/lang/String;)
                [13] invokestatic #4 = Methodref(A.sink(Ljava/lang/String;)V)
                [16] return
             */

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:13",
                        "JvmStackLocation(0)@LA;getSource2()Ljava/lang/String;:3",
                    ),
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:13",
                        "JvmStackLocation(0)@LA;main(Z)V:7",
                        "JvmStackLocation(0)@LA;getSource1()Ljava/lang/String;:3",
                    ),
                )
            }
        }
    }

    // with the current version of JvmMemoryLocationTransferRelation exception paths are not supported yet
    Given("A main method assigning null to a local in a try block with a throwing callee before passing it to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            try
                            {
                                callee();
                                s = null;
                            }
                            catch (Exception e)
                            {
                    
                            }
                            sink(s);
                        }
                    
                        public static void callee()
                        {
                            throw new IllegalStateException();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed").config(enabled = false) {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:14",
                        "JvmLocalVariableLocation(1)@LA;main()V:13",
                        "JvmLocalVariableLocation(1)@LA;main()V:12",
                        "JvmLocalVariableLocation(1)@LA;main()V:4",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a callee's source method result to a sink and discarding another source result in an unused local, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            String s = source1();
                            sink(callee(source1()));
                        }
                    
                        public static String callee(String s)
                        {
                            return source1();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:10",
                        "JvmStackLocation(0)@LA;callee(Ljava/lang/String;)Ljava/lang/String;:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a ternary between two locals assigned from source method calls to a sink, two returning sources, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public void main(boolean b) {
                            String s1 = source1();
                            String s2 = source1();
                            sink(b ? s1 : s2);
                        }
                        
                        public static String source1()
                        {
                            return null;
                        }
                        
                        public static String source2()
                        {
                            return null;
                        }
                        
                        public static void sink(String s)
                        {
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(
                interproceduralCfa,
                setOf(taintSourceReturn1, taintSourceReturn2),
                setOf(taintSinkArgument),
            )
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
                [3] astore_2 v2
                [4] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
                [7] astore_3 v3
                [8] iload_1 v1
                [9] ifeq +7 (target=16)
                [12] aload_2 v2
                [13] goto +4 (target=17)
                [16] aload_3 v3
                [17] invokestatic #3 = Methodref(A.sink(Ljava/lang/String;)V)
                [20] return
            */

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:17",
                        "JvmLocalVariableLocation(3)@LA;main(Z)V:16",
                        "JvmLocalVariableLocation(3)@LA;main(Z)V:9",
                        "JvmLocalVariableLocation(3)@LA;main(Z)V:8",
                        "JvmStackLocation(0)@LA;main(Z)V:7",
                    ),
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:17",
                        "JvmStackLocation(0)@LA;main(Z)V:13",
                        "JvmLocalVariableLocation(2)@LA;main(Z)V:12",
                        "JvmLocalVariableLocation(2)@LA;main(Z)V:9",
                        "JvmLocalVariableLocation(2)@LA;main(Z)V:8",
                        "JvmLocalVariableLocation(2)@LA;main(Z)V:7",
                        "JvmLocalVariableLocation(2)@LA;main(Z)V:4",
                        "JvmStackLocation(0)@LA;main(Z)V:3",
                    ),
                )
            }
        }
    }

    Given("A main method conditionally writing a static field with a source method result or through a callee and passing it to a sink, two returning sources, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {
                    
                        public static String staticSourceField;

                        public void main(boolean b) {
                            
                            if (b)
                            {
                                staticSourceField = source1();
                            }
                            else 
                            {
                                callee();
                            }
                            
                            sink(staticSourceField);
                        }
                    
                        public static void callee()
                        {
                            staticSourceField = source2();
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    
                        public static String source2()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(
                interproceduralCfa,
                setOf(taintSourceReturn1, taintSourceReturn2),
                setOf(taintSinkArgument),
            )
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] iload_1 v1
                [1] ifeq +12 (target=13)
                [4] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
                [7] putstatic #3 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [10] goto +6 (target=16)
                [13] invokestatic #4 = Methodref(A.callee()V)
                [16] getstatic #3 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [19] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
                [22] return

            Bytecode of callee:
                [0] invokestatic #6 = Methodref(A.source2()Ljava/lang/String;)
                [3] putstatic #3 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [6] return
            */

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:19",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main(Z)V:16",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main(Z)V:10",
                        "JvmStackLocation(0)@LA;main(Z)V:7",
                    ),
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:19",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main(Z)V:16",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;callee()V:6",
                        "JvmStackLocation(0)@LA;callee()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing the result of a recursive callee taking two source method results to a sink, two returning sources, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main(boolean b) 
                        {
                            sink(callee(b, source1(), source2()));
                        }
                        
                        public String callee(boolean b, String s1, String s2) 
                        {             
                            return b ? callee(!b, s1, s2) : s1;
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                        
                        public static String source2()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(
                interproceduralCfa,
                setOf(taintSourceReturn1, taintSourceReturn2),
                setOf(taintSinkArgument),
            )
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed").config(enabled = false) {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] aload_0 v0
                [1] iload_1 v1
                [2] invokestatic #2 = Methodref(A.source1()Ljava/lang/String;)
                [5] invokestatic #3 = Methodref(A.source2()Ljava/lang/String;)
                [8] invokevirtual #4 = Methodref(A.callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;)
                [11] invokestatic #5 = Methodref(A.sink(Ljava/lang/String;)V)
                [14] return

            Bytecode of callee:
                [0] iload_1 v1
                [1] ifeq +21 (target=22)
                [4] aload_0 v0
                [5] iload_1 v1
                [6] ifne +7 (target=13)
                [9] iconst_1
                [10] goto +4 (target=14)
                [13] iconst_0
                [14] aload_2 v2
                [15] aload_3 v3
                [16] invokevirtual #4 = Methodref(A.callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;)
                [19] goto +4 (target=23)
                [22] aload_2 v2
                [23] areturn
             */

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main(Z)V:11",
                        "JvmStackLocation(0)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:23",
                        "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:22",
                        "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:11",
                        "JvmLocalVariableLocation(2)@LA;callee(ZLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;:0",
                        "JvmStackLocation(1)@LA;main(Z)V:8, JvmStackLocation(0)@LA;main(Z)V:5",
                    ),
                )
            }
        }
    }

    Given("A main method writing an unrelated static field between a callee writing a source method result to the field and passing the field to a sink, a returning source, and an argument sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public static String staticSourceField;
                        public static String s1;
                    
                        public void main() {
                            callee();
                            s1 = "42";
                            sink(staticSourceField);
                        }
                    
                        public static void callee()
                        {
                            staticSourceField = source1();
                        }
                    
                        public static void sink(String s)
                        {
                            return;
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkArgument))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                /*
            Bytecode of main:
                [0] invokestatic #2 = Methodref(A.callee()V)
                [3] ldc #3 = String("42")
                [5] putstatic #4 = Fieldref(A.s1 Ljava/lang/String;)
                [8] getstatic #7 = Fieldref(A.staticSourceField Ljava/lang/String;)
                [11] invokestatic #8 = Methodref(A.sink(Ljava/lang/String;)V)
                [14] return
             */

                traces.map { it.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;main()V:11",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:8",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:5",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;main()V:3",
                        "JvmStaticFieldLocation(A.staticSourceField:Ljava/lang/String;)@LA;callee()V:6",
                        "JvmStackLocation(0)@LA;callee()V:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a callee's source method result to a sink returning its argument, a returning source, and a return sink") {
        val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPool(
            ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    class A {

                        public void main() {
                            sink(callee());
                        }
                    
                        public static String callee()
                        {
                            return source1();
                        }
                    
                        public static String sink(String s)
                        {
                            return s;
                        }
                    
                        public static String source1()
                        {
                            return null;
                        }
                    }
                    """.trimIndent(),
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8"),
            ).programClassPool,
        )
        /*
        Bytecode of main:
            [0] invokestatic #2 = Methodref(A.callee()Ljava/lang/String;)
            [3] invokestatic #3 = Methodref(A.sink(Ljava/lang/String;)Ljava/lang/String;)
            [6] pop
            [7] return

        Bytecode of callee:
            [0] invokestatic #4 = Methodref(A.source1()Ljava/lang/String;)
            [3] areturn

        Bytecode of sink:
            [0] aload_0 v0
            [1] areturn
         */
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer = TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturn1), setOf(taintSinkReturn))
                .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;sink(Ljava/lang/String;)Ljava/lang/String;:1",
                        "JvmLocalVariableLocation(0)@LA;sink(Ljava/lang/String;)Ljava/lang/String;:0",
                        "JvmStackLocation(0)@LA;main()V:3",
                        "JvmStackLocation(0)@LA;callee()Ljava/lang/String;:3",
                    ),
                )
            }
        }
    }

    Given("A main method passing a source method result through a callee to a sink, a returning double source, and a double argument sink") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                        class A {
                        
                            public void main() {
                                foo(source());
                            }
                        
                            public static double source()
                            {
                                return 0.0;
                            }
                            
                            public static void foo(double d)
                            {
                                sink(d);
                            }
                        
                            public static void sink(double d)
                            {
                                return;
                            }
                        }
                """.trimIndent(),
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8"),
        )
        val interproceduralCfa = CfaUtil.createInterproceduralCfa(programClassPool)
        /*
        Bytecode of main:
             [0] invokestatic #7 = Methodref(A.source()D)
             [3] invokestatic #13 = Methodref(A.foo(D)V)
             [6] return
        Bytecode of source:
             [0] dconst_0
             [1] dreturn
        Bytecode of foo:
            [0] dload_0 v0
            [1] invokestatic #17 = Methodref(A.sink(D)V)
            [4] return
        Bytecode of sink:
            [0] return
         */
        val mainSignature =
            interproceduralCfa!!.functionEntryNodes.stream().filter { it.signature.fqn.contains("main") }.findFirst()
                .get().signature

        When("TaintAnalyzer analyzes taints and reconstructs traces") {
            val taintAnalyzer =
                TaintAnalyzer.Builder(interproceduralCfa, setOf(taintSourceReturnDouble), setOf(taintSinkArgumentDouble))
                    .build()
            val result = taintAnalyzer.analyze(mainSignature).traceReconstructionResult
            interproceduralCfa.clear()

            Then("The expected traces are reconstructed") {
                val traces = result.extractLinearTraces()

                traces.map { trace -> trace.map { it.toString() } }.toSet() shouldBe setOf(
                    listOf(
                        "JvmStackLocation(0)@LA;foo(D)V:1",
                        "JvmLocalVariableLocation(0)@LA;foo(D)V:0",
                        "JvmStackLocation(0)@LA;main()V:3",
                    ),
                )
            }
        }
    }
})
