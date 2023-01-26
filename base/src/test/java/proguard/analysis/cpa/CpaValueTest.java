package proguard.analysis.cpa;

import org.junit.jupiter.api.Test;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.Cfa;
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
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.value.JvmValueAbstractState;
import proguard.analysis.cpa.jvm.domain.value.JvmValueTransferRelation;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.Signature;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.UnknownReferenceValue;
import proguard.testutils.ClassPoolBuilder;
import proguard.testutils.FileSource;
import proguard.testutils.TestSource;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static proguard.testutils.JavaUtilKt.getCurrentJavaHome;

/**
 * @author james
 */
public class CpaValueTest {

    @Test
    public void testSimpleString()
    {
        ClassPool programClassPool = getProgramClassPool("A.java");

        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<JvmValueAbstractState>, MethodSignature> reachedSet = runCpa(programClassPool, "A");

        JvmAbstractState<JvmValueAbstractState> lastAbstractState = reachedSet
                .asCollection()
                .stream()
                .max(Comparator.comparingInt(it -> it.getProgramLocation().getOffset()))
                .get();

        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals(value.value(), "Hello", "The value should be correctly tracked");
    }

    @Test
    public void testJoinTwoDifferentValues()
    {
        ClassPool programClassPool = getProgramClassPool("JoinTestDifferentValues.java");

        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<JvmValueAbstractState>, MethodSignature> reachedSet = runCpa(programClassPool, "JoinTestDifferentValues");

        JvmAbstractState<JvmValueAbstractState> lastAbstractState = reachedSet
                .asCollection()
                .stream()
                .max(Comparator.comparingInt(it -> it.getProgramLocation().getOffset()))
                .get();

        assertEquals(lastAbstractState.getFrame().getLocalVariables().get(1), JvmValueAbstractState.top);
    }

    @Test
    public void testJoinTwoSameValues()
    {
        ClassPool programClassPool = getProgramClassPool("JoinTestSameValues.java");

        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<JvmValueAbstractState>, MethodSignature> reachedSet = runCpa(programClassPool, "JoinTestSameValues");

        JvmAbstractState<JvmValueAbstractState> lastAbstractState = reachedSet
                .asCollection()
                .stream()
                .max(Comparator.comparingInt(it -> it.getProgramLocation().getOffset()))
                .get();

        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getFrame().getLocalVariables().get(1).getValue();
        assertEquals(value.value(), "1", "The value should be correctly tracked");
    }


    private static ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<JvmValueAbstractState>, MethodSignature> runCpa(ClassPool programClassPool, String className)
    {
        Cfa<JvmCfaNode, JvmCfaEdge, MethodSignature> cfa = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);

        AbstractDomain abstractDomain = new DelegateAbstractDomain<JvmValueAbstractState>();

        JvmValueTransferRelation transferRelation = new JvmValueTransferRelation();

        MergeOperator mergeJoinOperator = new MergeJoinOperator(abstractDomain);
        StopOperator stopAlwaysOperator = new StopAlwaysOperator();
        StopOperator stopContainedOperator = new StopContainedOperator();
        StopSepOperator stopSepOperator = new StopSepOperator(abstractDomain);

        PrecisionAdjustment precisionAdjustment = new StaticPrecisionAdjustment();

        ProgramClass clazz = (ProgramClass) programClassPool.getClass(className);
        MethodSignature mainSignature = (MethodSignature) Signature.of(clazz, clazz.findMethod("main", null));
        UnknownReferenceValue unknownReferenceValue = new UnknownReferenceValue();
        JvmAbstractState<JvmValueAbstractState> initialStates =
                new JvmAbstractState<>(
                        cfa.getFunctionEntryNode(mainSignature),
                        new JvmFrameAbstractState<>(),
                        new JvmShallowHeapAbstractState<>(new HashMapAbstractState<>(), Integer.class, new JvmValueAbstractState(unknownReferenceValue)),
                        new HashMapAbstractState<>()
                );

        Waitlist waitlist = new DepthFirstWaitlist();
        waitlist.add(initialStates);
        ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<JvmValueAbstractState>, MethodSignature>
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

    private static ClassPool getProgramClassPool(String fileName) {
        return ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/CpaValueTest/" + fileName))},
                        Arrays.asList("-source", "1.8", "-target", "1.8"),
                        Collections.emptyList(),
                        getCurrentJavaHome(),
                        true)
                .getProgramClassPool();
    }
}
