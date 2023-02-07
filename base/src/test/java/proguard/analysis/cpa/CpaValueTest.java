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
import proguard.evaluation.value.ReferenceValue;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
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
        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertEquals(value.value(), "Hello World", "Hello World should be in local variable slot 1");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"Hello World should be on the top of the stack");
    }

    @Test
    public void testSimpleString2()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleString2");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertEquals(value.value(), "Hello World", "Hello World should be in local variable slot 1");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"Hello World should be on the top of the stack");

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
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getFieldOrDefault(0, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
    }

    @Test
    public void testSimpleStringBuffer()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringBuffer");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getFieldOrDefault(0, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
    }


    @Test
    public void testSimpleStringConcat()
    {
        // In this example a known integer constant value is used
        // as an operand to String.substring.
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringConcat");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getVariableOrDefault(1, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
    }

    @Test
    public void testStringSubString()
    {
        // In this example, a second StringBuilder is introduced by the compiler
        // for the string concatenation. The second StringBuilder is not stored in a local
        // variable.
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringSubString");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getVariableOrDefault(1, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
    }

    @Test
    public void testStringBuilderBranch()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderBranch");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        assertInstanceOf(TypedReferenceValue.class, stackTop,"The value should be correctly tracked");
        assertEquals("Ljava/lang/String;", stackTop.internalType());
        assertNotEquals(true, stackTop instanceof ParticularReferenceValue);

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        Value value = lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertInstanceOf(IdentifiedReferenceValue.class, value, "The value should be a Identified reference 0");
        assertEquals(0, ((IdentifiedReferenceValue)value).id);
        assertNotEquals(true, stackTop instanceof ParticularReferenceValue);
        assertEquals("Ljava/lang/StringBuilder;", value.internalType(), "The type should be StringBuilder");
    }

    //@Test
    public void testStringBuilderLoop()
    {
        // TODO: gets stuck in a loop!
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderLoop");

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        IdentifiedReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        Object heapValue = lastAbstractState.getFieldOrDefault(value.id, UNKNOWN).getValue();
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

    private static JvmValueAbstractState getPrintlnCall(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet) {
        return reachedSet.asCollection()
                .stream()
                .filter(it -> it
                        .getProgramLocation()
                        .getLeavingInterproceduralEdges()
                        .stream()
                        .anyMatch(x -> x.getCall().getTarget().method.equals("println"))
                ).findFirst().get();
    }

    private static ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> runCpa(String className)
    {
        ClassPool programClassPool = getProgramClassPool(className);
        ValueFactory valueFactory = new ParticularValueFactory(new ParticularValueFactory.ReferenceValueFactory());
        JvmHeapAbstractState<ValueAbstractState> heap = new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), Integer.class, UNKNOWN);

        JvmCfa cfa = CfaUtil.createInterproceduralCfa(programClassPool, ClassPoolBuilder.Companion.getLibraryClassPool());

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
