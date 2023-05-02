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
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.value.JvmCfaReferenceValueFactory;
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
import proguard.classfile.visitor.ClassPrinter;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.value.DoubleValue;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ParticularDoubleValue;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.TopValue;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.UnknownReferenceValue;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static proguard.analysis.cpa.jvm.domain.value.ValueAbstractState.UNKNOWN;
import static proguard.testutils.JavaUtilKt.getCurrentJavaHome;

/**
 * Tests for CPA value analysis.
 */
public class CpaValueTest {
    private static final boolean DEBUG = false;

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
    public void testSimpleDouble()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleDouble");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);

        ParticularDoubleValue value = (ParticularDoubleValue) lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertEquals(1.0, value.doubleValue().value(), "1.0 should be in local variable slot 1");

        TopValue topValue = (TopValue) lastAbstractState.getVariableOrDefault(2, UNKNOWN).getValue();
        assertEquals(topValue, new TopValue(), "The top value should be in local variable slot 2");

        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stackTopValue = printlnCall.getFrame().getOperandStack().peek(1).getValue();
        assertInstanceOf(TopValue.class, stackTopValue);

        DoubleValue doubleValue = printlnCall.getFrame().getOperandStack().peek(0).getValue().doubleValue();
        assertEquals(1.0, doubleValue.value());
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
        assertInstanceOf(JvmCfaNode.class, value.id);
        // In this case, the ID would be 5 or 11 since two strings are created at different locations.
    }

    @Test
    public void testSimpleStringBuilder()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringBuilder");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getVariableOrDefault(1, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stackTop).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stringBuilder).id);
        assertEquals("JvmCfaNode{LSimpleStringBuilder;main([Ljava/lang/String;)V:28}", ((IdentifiedReferenceValue)stackTop).id.toString());
        assertEquals("JvmCfaNode{LSimpleStringBuilder;main([Ljava/lang/String;)V:0}", ((IdentifiedReferenceValue)stringBuilder).id.toString());
    }

    @Test
    public void testSimpleStringBuffer()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleStringBuffer");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getVariableOrDefault(1, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stackTop).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stringBuilder).id);
        assertEquals("JvmCfaNode{LSimpleStringBuffer;main([Ljava/lang/String;)V:28}", ((IdentifiedReferenceValue)stackTop).id.toString());
        assertEquals("JvmCfaNode{LSimpleStringBuffer;main([Ljava/lang/String;)V:0}", ((IdentifiedReferenceValue)stringBuilder).id.toString());
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
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stackTop).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stringBuilder).id);
        assertEquals("JvmCfaNode{LSimpleStringConcat;main([Ljava/lang/String;)V:39}", ((IdentifiedReferenceValue)stackTop).id.toString());
        assertEquals("JvmCfaNode{LSimpleStringConcat;main([Ljava/lang/String;)V:0}", ((IdentifiedReferenceValue)stringBuilder).id.toString());
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
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stackTop).id);
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)stringBuilder).id);
        assertEquals("JvmCfaNode{LStringSubString;main([Ljava/lang/String;)V:18}", ((IdentifiedReferenceValue)stackTop).id.toString());
        assertEquals("JvmCfaNode{LStringSubString;main([Ljava/lang/String;)V:0}", ((IdentifiedReferenceValue)stringBuilder).id.toString());
    }

    @Test
    public void testStringBuilderBranch()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderBranch");
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        assertInstanceOf(TypedReferenceValue.class, stackTop,"The value should be correctly tracked");
        assertEquals("Ljava/lang/String;", stackTop.internalType());
        assertFalse(stackTop instanceof ParticularReferenceValue);

        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        Value value = lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertInstanceOf(IdentifiedReferenceValue.class, value, "The value should be a Identified reference 0");
        assertEquals("Ljava/lang/StringBuilder;", value.internalType(), "The type should be StringBuilder");
        assertInstanceOf(JvmCfaNode.class, ((IdentifiedReferenceValue)value).id);
        assertEquals("JvmCfaNode{LStringBuilderBranch;main([Ljava/lang/String;)V:0}", ((IdentifiedReferenceValue)value).id.toString());
    }

    @Test
    public void testSimpleSwitch()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("SimpleSwitch");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        Value value = lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertInstanceOf(TypedReferenceValue.class, value);
        assertEquals("Ljava/lang/String;", value.internalType());
        assertFalse(value.isParticular());
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek()
                .getValue()
                .referenceValue();
        assertInstanceOf(TypedReferenceValue.class, stackTop);
        assertEquals("Ljava/lang/String;", stackTop.internalType());
        assertFalse(stackTop.isParticular());
    }

    @Test
    public void testStringBuilderLoopNested()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderLoopNested");

        // Parameter to System.out.println is a typed String but
        // the actual value is unknown.
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek()
                .getValue()
                .referenceValue();
        assertInstanceOf(TypedReferenceValue.class, stackTop,"The value should be correctly tracked");
        assertEquals("Ljava/lang/String;", stackTop.internalType());
        assertTrue(stackTop instanceof TypedReferenceValue);
        assertFalse(stackTop instanceof ParticularReferenceValue);
    }


    @Test
    public void testStringBuilderLoop()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("StringBuilderLoop");

        // Parameter to System.out.println is a typed String but the actual value is unknown.
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek()
                .getValue()
                .referenceValue();
        assertInstanceOf(TypedReferenceValue.class, stackTop,"The value should be correctly tracked");
        assertEquals("Ljava/lang/String;", stackTop.internalType());
        assertTrue(stackTop instanceof TypedReferenceValue);
        assertFalse(stackTop instanceof ParticularReferenceValue);
    }

    @Test
    public void testUnsupportedClass()
    {
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet = runCpa("UnsupportedClass");
        JvmValueAbstractState lastAbstractState = getLastAbstractState(reachedSet);
        assertInstanceOf(UnknownReferenceValue.class, lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue());
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        assertInstanceOf(UnknownReferenceValue.class, stackTop);
        assertTrue(((JvmShallowHeapAbstractState)lastAbstractState.getHeap()).referenceToObject.isEmpty());
    }

    private static JvmValueAbstractState getLastAbstractState(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet)
    {
        return reachedSet
                .asCollection()
                .stream()
                .max(Comparator.comparingInt(it -> it.getProgramLocation().getOffset()))
                .get();
    }

    private static JvmValueAbstractState getPrintlnCall(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet)
    {
        return getMethodCall(reachedSet, "println");
    }

    private static JvmValueAbstractState getMethodCall(ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> reachedSet, String methodName)
    {
        return reachedSet.asCollection()
                .stream()
                .filter(it -> it
                        .getProgramLocation()
                        .getLeavingInterproceduralEdges()
                        .stream()
                        .anyMatch(x -> x.getCall().getTarget().method.equals(methodName))
                ).findFirst().get();
    }

    private static ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmValueAbstractState, MethodSignature> runCpa(String className)
    {
        ClassPool programClassPool = getProgramClassPool(className);
        JvmHeapAbstractState<ValueAbstractState> heap = new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), JvmCfaNode.class, UNKNOWN);

        JvmCfa cfa = CfaUtil.createInterproceduralCfa(programClassPool, ClassPoolBuilder.Companion.getLibraryClassPool());

        if (DEBUG)
        {
            try {
                Files.write(new File("graph.dot").toPath(), CfaUtil.toDot(cfa).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        AbstractDomain abstractDomain = new DelegateAbstractDomain<ValueAbstractState>();
        ValueFactory valueFactory = new ParticularValueFactory(new JvmCfaReferenceValueFactory(cfa));
        ExecutingInvocationUnit executingInvocationUnit = new ExecutingInvocationUnit.Builder().setEnableSameInstanceIdApproximation(true).build(valueFactory);
        JvmValueTransferRelation transferRelation = new JvmValueTransferRelation(valueFactory, executingInvocationUnit);
        MergeOperator mergeJoinOperator = new MergeJoinOperator(abstractDomain);
        StopOperator stopOperator = new StopJoinOperator(abstractDomain);
        PrecisionAdjustment precisionAdjustment = new StaticPrecisionAdjustment();

        ProgramClass clazz = (ProgramClass) programClassPool.getClass(className);
        MethodSignature mainSignature = (MethodSignature) Signature.of(clazz, clazz.findMethod("main", null));
        JvmFrameAbstractState<ValueAbstractState> frame = new JvmFrameAbstractState<>();
        HashMapAbstractState<String, ValueAbstractState> staticFields = new HashMapAbstractState<>();
        JvmValueAbstractState initialStates =
            new JvmValueAbstractState(
                valueFactory,
                    executingInvocationUnit,
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
                        stopOperator,
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
