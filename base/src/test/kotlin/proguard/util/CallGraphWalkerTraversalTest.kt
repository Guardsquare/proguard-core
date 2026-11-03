import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.analysis.CallResolver
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.analysis.datastructure.callgraph.Node
import proguard.classfile.ClassPool
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.util.CallGraphWalker
import java.util.ArrayDeque

class CallGraphWalkerTraversalTest : BehaviorSpec({

    Given("a linear call graph") {
        // Chain: testMethod -> succ1 -> succ2 -> succ3
        val chainPool = buildCallGraph(
            """
            public class A
            {
                public static void testMethod() { succ1(); }
                public static void succ1() { succ2(); }
                public static void succ2() { succ3(); }
                public static void succ3() {}
            }
            """,
        )
        val chainGraph = chainPool.second
        val chainStart = MethodSignature("A", "testMethod", "()V")
        val succ1 = MethodSignature("A", "succ1", "()V")
        val succ2 = MethodSignature("A", "succ2", "()V")
        val succ3 = MethodSignature("A", "succ3", "()V")

        Then("maxDepth=2 reports only the first two levels and marks the third truncated") {
            val reported = mutableListOf<MethodSignature>()
            val root = CallGraphWalker.successorPathsAccept(
                chainGraph,
                chainStart,
                { n -> reported.add(n.signature); true },
                2,
            )

            // Depth 0 (testMethod) and depth 1 (succ1) are reported; succ2 is at depth 2,
            // which equals maxDepth, so it is marked truncated and not passed to the handler.
            reported shouldContainExactly listOf(chainStart, succ1)

            // succ2 exists in the reconstructed tree and is marked truncated.
            val succ2Node = findNode(root, succ2)
            succ2Node shouldNotBe null
            succ2Node!!.isTruncated shouldBe true

            // succ3 was never created, because succ2 was never expanded.
            findNode(root, succ3) shouldBe null
        }

        Then("maxDepth large enough reports the whole chain") {
            val reported = mutableListOf<MethodSignature>()
            CallGraphWalker.successorPathsAccept(
                chainGraph,
                chainStart,
                { n -> reported.add(n.signature); true },
                100,
            )

            reported shouldContainExactly listOf(chainStart, succ1, succ2, succ3)
        }
    }

    Given("a diamond-shaped call graph") {
        // Diamond: root -> a -> c, root -> b -> c (c reachable via 2 distinct paths)
        val diamondPool = buildCallGraph(
            """
            public class A
            {
                public static void root() { a(); b(); }
                public static void a() { c(); }
                public static void b() { c(); }
                public static void c() {}
            }
            """,
        )
        val diamondGraph = diamondPool.second
        val diamondStart = MethodSignature("A", "root", "()V")
        val a = MethodSignature("A", "a", "()V")
        val b = MethodSignature("A", "b", "()V")
        val c = MethodSignature("A", "c", "()V")

        Then("per-path visited set: a method reachable via N paths is reported N times") {
            val reported = mutableListOf<MethodSignature>()
            CallGraphWalker.successorPathsAccept(
                diamondGraph,
                diamondStart,
                { n -> reported.add(n.signature); true },
                100,
            )

            // root, a, b are each reachable via a single path; c is reachable via 2 paths
            // (root->a->c and root->b->c), so it must be reported once per path.
            reported.count { it == c } shouldBe 2
            reported.count { it == diamondStart } shouldBe 1
            reported.count { it == a } shouldBe 1
            reported.count { it == b } shouldBe 1
        }
    }

    Given("a cyclic call graph") {
        // Cycle: start -> cycA -> start (cycle), cycA -> cycB
        val cyclePool = buildCallGraph(
            """
            public class A
            {
                public static void start() { cycA(); }
                public static void cycA() { start(); cycB(); }
                public static void cycB() {}
            }
            """,
        )
        val cycleGraph = cyclePool.second
        val cycleStart = MethodSignature("A", "start", "()V")
        val cycA = MethodSignature("A", "cycA", "()V")
        val cycB = MethodSignature("A", "cycB", "()V")

        Then("per-path visited set breaks cycles without skipping siblings") {
            val reported = mutableListOf<MethodSignature>()
            CallGraphWalker.successorPathsAccept(
                cycleGraph,
                cycleStart,
                { n -> reported.add(n.signature); true },
                100,
            )

            // start is on the path start->cycA, so the edge cycA->start is skipped (cycle broken).
            // cycB is not on the path, so it is still explored.
            reported.count { it == cycleStart } shouldBe 1
            reported.count { it == cycA } shouldBe 1
            reported.count { it == cycB } shouldBe 1
        }
    }

    Given("a directly recursive call graph") {
        // Recursion: start -> rec -> rec (self-loop)
        val recursionPool = buildCallGraph(
            """
            public class A
            {
                public static void start() { rec(); }
                public static void rec() { rec(); }
            }
            """,
        )
        val recursionGraph = recursionPool.second
        val recursionStart = MethodSignature("A", "start", "()V")
        val rec = MethodSignature("A", "rec", "()V")

        Then("direct recursion is reported once and the self-edge is not expanded") {
            val reported = mutableListOf<MethodSignature>()
            val root = CallGraphWalker.successorPathsAccept(
                recursionGraph,
                recursionStart,
                { n -> reported.add(n.signature); true },
                100,
            )

            // rec is reported exactly once: the self-edge rec->rec is skipped because
            // rec is already on the current path, so the traversal terminates.
            reported shouldContainExactly listOf(recursionStart, rec)

            // The recursive node has no children in the reconstructed tree and is not truncated.
            val recNode = findNode(root, rec)
            recNode shouldNotBe null
            recNode!!.successors shouldBe emptySet()
            recNode.isTruncated shouldBe false
        }
    }

    Given("a call graph with nested loops") {
        // Nested loops: a -> b,c; b -> c,a; c -> b,a
        val nestedLoopPool = buildCallGraph(
            """
            public class A
            {
                public static void a() { b(); c(); }
                public static void b() { c(); a(); }
                public static void c() { b(); a(); }
            }
            """,
        )
        val nestedLoopGraph = nestedLoopPool.second
        val a = MethodSignature("A", "a", "()V")
        val b = MethodSignature("A", "b", "()V")
        val c = MethodSignature("A", "c", "()V")

        Then("mutual recursion is reported once per distinct path") {
            val reported = mutableListOf<MethodSignature>()
            val root = CallGraphWalker.successorPathsAccept(
                nestedLoopGraph,
                a,
                { n -> reported.add(n.signature); true },
                100,
            )

            // The distinct acyclic paths from a are: [a], [a,b], [a,c], [a,b,c], [a,c,b].
            // a appears only on its own path; b and c are each reachable via 2 paths.
            reported.count { it == a } shouldBe 1
            reported.count { it == b } shouldBe 2
            reported.count { it == c } shouldBe 2

            // The tree is finite: a has children b and c, each of which has a single
            // child (the other one); those deepest nodes have no children, because
            // every further edge leads back to a method already on the path.
            root.successors.size shouldBe 2
            root.successors.forEach { node ->
                node.successors.size shouldBe 1
                val deepest = node.successors.first()
                deepest.successors shouldBe emptySet()
                deepest.isTruncated shouldBe false
            }
        }
    }
})

private fun buildCallGraph(source: String): Pair<ClassPool, CallGraph> {
    val classPool = ClassPoolBuilder.fromSource(
        JavaSource("A.java", source.trimIndent()),
        javacArguments = listOf("-source", "1.8", "-target", "1.8"),
    ).programClassPool
    val callGraph = CallGraph()
    val resolver = CallResolver.Builder(classPool, ClassPool(), callGraph)
        .setEvaluateAllCode(true)
        .build()
    classPool.classesAccept(resolver)
    return classPool to callGraph
}

/** BFS the reconstructed node tree (following successors) for a node with the given signature. */
private fun findNode(root: Node, signature: MethodSignature): Node? {
    val seen = HashSet<Node>()
    val queue = ArrayDeque<Node>()
    queue.add(root)
    while (queue.isNotEmpty()) {
        val n = queue.poll()
        if (!seen.add(n)) continue
        if (n.signature == signature) return n
        queue.addAll(n.successors)
    }
    return null
}
