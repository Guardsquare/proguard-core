package proguard.examples;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintMemoryLocationBamCpaRun;
import proguard.analysis.cpa.jvm.domain.taint.JvmTaintSink;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;

/**
 * This sample application illustrates how to perform taint analysis with the
 * ProGuard API.
 *
 * Usage:
 *     java proguard.examples.AnalyzeTaints input.jar
 */
public class AnalyzeTaints
{
    public static void main(String[] args)
    {
        String inputJarFileName  = args[0];

        try
        {
            // Read the program classes.
            ClassPool programClassPool = JarUtil.readJar(inputJarFileName, false);

            // Create the control flow automaton (CFA).
            JvmCfa cfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);

            // Create a taint source.
            TaintSource source = new TaintSource("LA;source()Ljava/lang/String;", // the fully qualified name of a source method
                                                 false,                           // whether the source taints the calling instance
                                                 true,                            // whether the source taints its return
                                                 Collections.emptySet(),          // a set of tainted arguments
                                                 Collections.emptySet());         // taintsGlobals - a set of tainted global variables

            // Create a taint sink.
            JvmTaintSink sink = new JvmTaintSink("LA;sink(Ljava/lang/String;)V", // the fully qualified name of a sink method
                                                 false,                          // whether the sink is sensitive to the calling instance
                                                 Collections.singleton(1),       // a set of sensitive arguments
                                                 Collections.emptySet());        // a set of sensitive global variables

            // Create the CPA run.
            JvmTaintMemoryLocationBamCpaRun cpaRun = new JvmTaintMemoryLocationBamCpaRun(cfa,                                         // a CFA
                                                                                         Collections.singleton(source),               // a set of taint sources
                                                                                         new MethodSignature("Main",
                                                                                                             "main",
                                                                                                             "()[Ljava/lang/String"), // the signature of the main method
                                                                                         -1,                                          // maximum depth of the call stack
                                                                                                                                      // 0 means intra-procedural analysis.
                                                                                                                                      // < 0 means unlimited call stack.
                                                                                         TaintAbstractState.bottom,                   // a cut-off threshold
                                                                                         Collections.singleton(sink));                // a collection of taint sinks

            // Run the analysis and get witness traces.
            Set<List<JvmMemoryLocation>> traces = cpaRun.extractLinearTraces();

            // Print the analysis result.
            System.out.println(traces.isEmpty()
                               ? "No data flow between the source and the sink detected."
                               : "Data flow between the source and the sink detected.\nWitness traces:");
            traces.forEach(System.out::println);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
