import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.CallResolver
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.ClassPool
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.util.CallGraphWalker

class CallGraphWalkerIterationOrderTest : FunSpec({

    // Setup: Create a call graph where testMethod() has three predecessors and sucessors.
    val classPool = ClassPoolBuilder.fromSource(
        JavaSource(
            "A.java",
            """
            public class A
            {
                public static void testMethod()
                {
                    succ1();
                    succ2();
                    succ3();
                }
                
                public static void predA()
                {
                    testMethod();
                }
                
                public static void predB()
                {
                    testMethod();
                }
                                        
                public static void predC()
                {
                    testMethod();
                }
        
                public static void succ1()
                {
                }
                
                public static void succ2()
                {                   
                }
                
                public static void succ3()
                {
                }
            }
            """.trimIndent()
        ),
        javacArguments = listOf("-source", "1.8", "-target", "1.8")
    ).programClassPool
    val callGraph = CallGraph()
    val resolver = CallResolver.Builder(
        classPool,
        ClassPool(),
        callGraph
    )
        .setEvaluateAllCode(true)
        .build()
    classPool.classesAccept(resolver)
    val startSignature = MethodSignature("A", "testMethod", "()V")

    test("Successor order is deterministic") {
        val succ1 = MethodSignature("A", "succ1", "()V")
        val succ2 = MethodSignature("A", "succ2", "()V")
        val succ3 = MethodSignature("A", "succ3", "()V")

        val successors: Set<MethodSignature> = CallGraphWalker.getSuccessors(callGraph, startSignature)
        val orderedSuccessors: MutableList<MethodSignature> = ArrayList()
        successors.forEach { orderedSuccessors.add(it) }

        // Calls are created in offset-ascending order, this should have been preserved
        orderedSuccessors shouldBe listOf(startSignature, succ1, succ2, succ3)
    }

    test("Predecessor order is deterministic") {
        val pred1 = MethodSignature("A", "predA", "()V")
        val pred2 = MethodSignature("A", "predB", "()V")
        val pred3 = MethodSignature("A", "predC", "()V")

        val predecessors: Set<MethodSignature> = CallGraphWalker.getPredecessors(callGraph, startSignature)
        val orderedPredecessors: MutableList<MethodSignature> = ArrayList()
        predecessors.forEach { orderedPredecessors.add(it) }

        // Methods are visited by CallResolver in order of declaration, this should have been preserved
        orderedPredecessors shouldBe listOf(startSignature, pred1, pred2, pred3)
    }
})
