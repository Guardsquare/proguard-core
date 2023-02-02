package proguard.analysis.cpa;

import org.junit.jupiter.api.Test;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.DepthFirstWaitlist;
import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StaticPrecisionAdjustment;
import proguard.analysis.cpa.defaults.StopAlwaysOperator;
import proguard.analysis.cpa.defaults.StopContainedOperator;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.value.JvmValueAbstractState;
import proguard.analysis.cpa.jvm.domain.value.JvmValueTransferRelation;
import proguard.analysis.cpa.jvm.domain.value.ValueAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.Signature;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;
import proguard.testutils.ClassPoolBuilder;
import proguard.testutils.FileSource;
import proguard.testutils.TestSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
import static proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState.FAKE_FIELD;
import static proguard.testutils.JavaUtilKt.getCurrentJavaHome;

/**
 * @author james
 */
public class CpaValueTest {

    @Test
    public void testSimpleString()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleString");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals(value.value(), "Hello", "The value should be correctly tracked");
    }

    @Test
    public void testSimpleString2()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleString2");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals(value.value(), "Hello", "The value should be correctly tracked");
    }

    @Test
    public void testJoinTwoDifferentValues()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("JoinTestDifferentValues");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        Value valueInVarSlot1 = lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertInstanceOf(TypedReferenceValue.class, valueInVarSlot1);
        assertEquals("Ljava/lang/String;", valueInVarSlot1.internalType());
    }

    @Test
    public void testJoinTwoSameValues()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("JoinTestSameValues");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals(value.value(), "1", "The value should be correctly tracked");
    }

    @Test
    public void testSimpleStringBuilder()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringBuilder");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        IdentifiedReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals( "Hello World", lastAbstractState.getFieldOrDefault(value.id, FAKE_FIELD, UNKNOWN).getValue().referenceValue().value(),"The value should be correctly tracked");
    }

    @Test
    public void testSimpleStringBuffer()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringBuffer");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        IdentifiedReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals( "Hello World", lastAbstractState.getFieldOrDefault(value.id, FAKE_FIELD, UNKNOWN).getValue().referenceValue().value(),"The value should be correctly tracked");
    }

    @Test
    public void testSimpleStringConcat()
    {
        // In this example a known integer constant value is used
        // as an operand to String.substring.

        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringConcat");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        assertEquals(
            "Hello World",
            // It'll be on the heap, the final value is not store in a local -> it's a parameter to a method
            lastAbstractState.getFieldOrDefault(3, FAKE_FIELD, UNKNOWN).getValue().referenceValue().value(),
            "The value should be correctly tracked"
        );
    }

    @Test
    public void testStringSubString()
    {
        // In this example, a second StringBuilder is introduced by the compiler
        // for the string concatenation. This StringBuilder is not stored in a local
        // variable.
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringSubString");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        assertEquals(
            "Hello World",
            // It'll be on the heap, the final value is not store in a local -> it's a parameter to a method
            lastAbstractState.getFieldOrDefault(0, FAKE_FIELD, UNKNOWN).getValue().referenceValue().value(),
            "The value should be correctly tracked"
        );
    }

    @Test
    public void testStringBuilderBranch()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderBranch");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        Value value = lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertInstanceOf(TypedReferenceValue.class, value, "The value should be a TypedReferenceValue");
        assertEquals("Ljava/io/Serializable;", ((TypedReferenceValue)value).getType(), "The type should be Serializable (common interface of String and StringBuilder)");
    }

    //@Test
    public void testStringBuilderLoop()
    {
        // TODO: gets stuck in a loop!
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderLoop");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        IdentifiedReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        Object heapValue = lastAbstractState.getFieldOrDefault(value.id, FAKE_FIELD, UNKNOWN).getValue();
        assertInstanceOf(TypedReferenceValue.class, heapValue, "The value should be a TypedReferenceValue");
        // TODO: sometimes Serializable, sometimes StringBuilder
//        assertEquals("Ljava/io/Serializable;", ((TypedReferenceValue)heapValue).getType(), "The type should be Serializable (common class of String and StringBuilder)");
    }

    private static JvmValueAbstractState getLastAbstractState(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet)
    {
        return reachedSet
                .asCollection()
                .stream()
                .max(Comparator.comparingInt(it -> it.getProgramLocation().getOffset()))
                .get();
    }

    private static ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> runCpa(String className)
    {
        ClassPool programClassPool = getProgramClassPool(className);
        ValueFactory valueFactory = new ParticularValueFactory(new ParticularValueFactory.ReferenceValueFactory());
        JvmHeapAbstractState<ValueAbstractState> heap = new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), Integer.class, UNKNOWN);

        JvmCfa cfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool, ClassPoolBuilder.Companion.getLibraryClassPool());

        try {
            Files.write(new File("graph.dot").toPath(), CfaUtil.toDot(cfa).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        AbstractDomain abstractDomain = new DelegateAbstractDomain<ValueAbstractState>();

        JvmValueTransferRelation transferRelation = new JvmValueTransferRelation(valueFactory);

        MergeOperator mergeJoinOperator = new MergeJoinOperator(abstractDomain);
        StopOperator stopAlwaysOperator = new StopAlwaysOperator();
        StopOperator stopContainedOperator = new StopContainedOperator();
        StopSepOperator stopSepOperator = new StopSepOperator(abstractDomain);

        PrecisionAdjustment precisionAdjustment = new StaticPrecisionAdjustment();

        ProgramClass clazz = (ProgramClass) programClassPool.getClass(className);
        MethodSignature mainSignature = (MethodSignature) Signature.of(clazz, clazz.findMethod("main", null));
        JvmFrameAbstractState<ValueAbstractState> frame = new JvmFrameAbstractState<>();
        HashMapAbstractState<String, ValueAbstractState> staticFields = new HashMapAbstractState<>();
        JvmValueAbstractState initialStates =
            new JvmValueAbstractState(
                valueFactory,
                cfa.getFunctionEntryNode(mainSignature),
                frame,
                heap,
                staticFields
            );

        Waitlist waitlist = new DepthFirstWaitlist();
        waitlist.add(initialStates);
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature>
                reachedSet = new ProgramLocationDependentReachedSet<>();
        new CpaAlgorithm(
                new SimpleCpa(
                        abstractDomain,
                        transferRelation,
                        mergeJoinOperator,
                        stopContainedOperator,
                        precisionAdjustment
                )
        ).run(reachedSet, waitlist);
        return reachedSet;
    }

    private static ClassPool getProgramClassPool(String className)
    {
        return ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/CpaValueTest/" + className + ".java"))},
                        Arrays.asList("-source", "1.8", "-target", "1.8"),
                        Collections.emptyList(),
                        getCurrentJavaHome(),
                        true)
                .getProgramClassPool();
    }
}
