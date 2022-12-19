package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.CallResolver
import proguard.analysis.cpa.jvm.domain.memory.BamLocationDependentJvmMemoryLocation
import proguard.analysis.cpa.jvm.domain.taint.JvmInvokeTaintSink
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSource
import proguard.analysis.cpa.jvm.util.CfaUtil
import proguard.analysis.datastructure.callgraph.Call
import proguard.analysis.datastructure.callgraph.CallGraph
import proguard.classfile.ClassPool
import proguard.classfile.MethodSignature
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class JvmTaintSinkTest : FreeSpec({

    fun getSinkCall(trace: List<BamLocationDependentJvmMemoryLocation<*>>): Call {
        return trace
            .map { it.programLocation }
            .minBy { it.offset }
            .knownMethodCallEdges
            .first()
            .call
    }

    val classPool = ClassPoolBuilder.fromSource(
        JavaSource(
            "A.java",
            """
                    class A
                    {
    
                        public void main()
                        {
                            sink(source());
                            sink(source());
                        }
                    
                        public static void sink(String s)
                        {
                        }
                    
                        public static String source()
                        {
                            return null;
                        }
                    
                    }
            """.trimIndent()
        ),
        javacArguments = listOf("-source", "1.8", "-target", "1.8")
    ).programClassPool
    val callGraph = CallGraph()
    val resolver = CallResolver
        .Builder(
            classPool,
            ClassPool(),
            callGraph
        )
        .setEvaluateAllCode(true)
        .build()
    classPool.classesAccept(resolver)
    MethodSignature.clearCache()
    val interproceduralCfa = CfaUtil.createInterproceduralCfaFromClassPoolAndCallGraph(classPool, callGraph)

    val taintSourceReturnSignature = MethodSignature("A", "source", "()Ljava/lang/String;")
    val taintSourceReturn = JvmTaintSource(
        taintSourceReturnSignature,
        false,
        true,
        setOf(),
        setOf()
    )

    val taintSinkArgumentLastSignature = MethodSignature("A", "sink", "(Ljava/lang/String;)V")
    val taintSinkArgumentLast = JvmInvokeTaintSink(
        taintSinkArgumentLastSignature,
        { c -> callGraph.incoming[c.target]!!.maxOf { it.caller.offset } == c.caller.offset },
        false,
        setOf(1),
        setOf()
    )

    val mainSignature = MethodSignature("A", "main", "()V")

    "Call filter selects the correct call" {

        val taintMemoryLocationCpaRun = JvmTaintMemoryLocationBamCpaRun
            .Builder()
            .setCfa(interproceduralCfa)
            .setMainSignature(mainSignature)
            .setTaintSources(setOf(taintSourceReturn))
            .setTaintSinks(setOf(taintSinkArgumentLast))
            .build()

        val traces = taintMemoryLocationCpaRun.extractLinearTraces()
        traces.size shouldBe 1
        getSinkCall(traces.first()) shouldBe callGraph.incoming[taintSinkArgumentLastSignature]!!.maxBy { it.caller.offset }
    }
})
