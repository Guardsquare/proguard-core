## Taint analysis with CPA

Taint analysis aims for detecting a data flow between taint sources and sinks.
Configurable program analysis (CPA) is a formalism suitable for integrating multiple
data flow analyses in one tool. Taints can be traced in few simple steps.

### Modeling the control flow

A control flow automaton (CFA) is a graph with nodes being bytecode offsets and
edges being instructions or calls connecting them. You can create a CFA from
the program class pool:

    :::java
    // Create the control flow automaton (CFA).
    JvmCfa cfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);

### Defining taint sources

Every taint analysis data flow starts from a taint source. Any Java method can be a taint source.
You have several options of how a taint source can behave. A source may:

- taint the calling instance,
- return the taint,
- taint its actual parameters of nonprimitive types,
- taint static fields.

For creating a taint you need its fully qualified name and the expected tainting pattern.
Let us create a simple taint source returning a tainted string:

    :::java
    // Create a taint source.
    TaintSource source = new TaintSource("LMain;source()Ljava/lang/String;", // the fully qualified name of a source method
                                         false,                              // whether the source taints the calling instance
                                         true,                               // whether the source taints its return
                                         Collections.emptySet(),             // a set of tainted arguments
                                         Collections.emptySet());            // a set of tainted global variables

### Defining taint sinks

Taint sinks are the counterpart of taint sources sensitive to a taint. A taint sink may be sensitive to

- the calling instance,
- actual parameters,
- static fields.

Given the fully qualified name and the sensitivity model you can straightforwardly create a taint sink
like the one sensitive to its only argument:

    :::java
    // Create a taint sink.
    JvmTaintSink sink = new JvmTaintSink("LMain;sink(Ljava/lang/String;)V", // the fully qualified name of a sink method
                                         false,                             // whether the sink is sensitive to the calling instance
                                         Collections.singleton(1),          // a set of sensitive arguments
                                         Collections.emptySet());           // a set of sensitive global variables

**Note:** The argument enumeration for both taint sources and taint sinks starts from one
and does not depend on whether the method is static. The calling distance is handled by
a separate boolean constructor parameter.

### Setting up a CPA run

CPA runs encapsulate the initialization of CPA components and allow configuring the analysis.
The CPA run needs to know in which method the analysis needs to start and how deep the call stack
for the interprocedural analysis should be. All calls overflowing the stack, as well as all
library methods, are approximated intraprocedurally as propagating the taint from their
calling instance and arguments into the return value. You can create a CPA run for analyzing
`Main.main(String args)` with an unlimited call stack as follows:

    :::java
    // Create the CPA run.
    JvmTaintMemoryLocationBamCpaRun cpaRun = new JvmTaintMemoryLocationBamCpaRun(cfa,                                          // a CFA
                                                                                 Collections.singleton(source),                // a set of taint sources
                                                                                 new MethodSignature("Main",
                                                                                                     "main",
                                                                                                     "([Ljava/lang/String)V"), // the signature of the main method
                                                                                 -1,                                           // the maximum depth of the call stack analyzed interprocedurally.
                                                                                                                               // 0 means intra-procedural analysis.
                                                                                                                               // < 0 means unlimited depth.
                                                                                 TaintAbstractState.bottom,                    // a cut-off threshold
                                                                                 Collections.singleton(sink));                 // a collection of taint sinks

### Running the analysis and obtaining witness traces

The analysis execution can be done in a single line together with generating witness traces:

    :::java
    // Run the analysis and get witness traces.
    Set<List<JvmMemoryLocation>> traces = cpaRun.extractLinearTraces();

### Interpreting the analysis result

The result of the analysis is a set of witness traces, if there is a data flow detected.
A witness trace is a list of memory locations at specific program locations. For instance,
the class below

    :::java
    // Run the analysis and get witness traces.
    public class Main
    {
        public static void main()
        {
            sink(callee());
        }
                    
        public static String callee()
        {
            return source();
        }
    }

would generate a witness trace consisting of two top stack locations, one after the taint source in
`callee()` and another before the call to `sink(String s)`:

    :::java
    [JvmStackLocation(0)@LMain;main()V:3, JvmStackLocation(0)@LMain;callee()Ljava/lang/String;:3]

Note that the traces returned by the CPA run go from the taint sink to the taint source.
There are four types of memory locations:

- stack locations identified by their offsets from the operand stack top,
- local variable locations identified by their indices in the local variable array,
- static field locations identified by their fully qualified names,
- heap locations identified by their abstract references.

Complete example: AnalyzeTaints.java
