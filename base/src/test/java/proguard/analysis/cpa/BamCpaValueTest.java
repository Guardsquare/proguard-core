package proguard.analysis.cpa;

import org.junit.jupiter.api.Test;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BlockAbstraction;
import proguard.analysis.cpa.defaults.DefaultReachedSet;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.value.JvmValueAbstractState;
import proguard.analysis.cpa.jvm.domain.value.JvmValueBamCpaRun;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.Signature;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
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
 * Tests for BAM CPA value analysis.
 */
public class BamCpaValueTest {
    private static final boolean DEBUG = false;

    @Test
    public void testSimpleStringBuilder()
    {
        BamCache<MethodSignature> cache = runBamCpa("SimpleStringBuilder");

        ProgramLocationDependentReachedSet reachedSet = (ProgramLocationDependentReachedSet) cache
                .get(new MethodSignature("SimpleStringBuilder", "main", "([Ljava/lang/String;)V"))
                .stream()
                .findFirst()
                .map(BlockAbstraction::getReachedSet)
                .orElse(new DefaultReachedSet());
        JvmValueAbstractState printlnCall = getPrintlnCall(reachedSet);
        Value stringBuilder = printlnCall.getFieldOrDefault(0, UNKNOWN).getValue();
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"The value should be correctly tracked");
        assertNotEquals(((IdentifiedReferenceValue)stackTop).id, ((IdentifiedReferenceValue)stringBuilder).id);
    }


    @Test
    public void testSimpleStringInterprocedural()
    {
        BamCache<MethodSignature> cache = runBamCpa("SimpleStringInterprocedural");

        ProgramLocationDependentReachedSet fooReachedSet = (ProgramLocationDependentReachedSet) cache
                .get(new MethodSignature("SimpleStringInterprocedural", "foo", "(Ljava/lang/String;)V"))
                .stream()
                .findFirst()
                .map(BlockAbstraction::getReachedSet)
                .orElse(new DefaultReachedSet());

        JvmValueAbstractState lastAbstractState = getLastAbstractState(fooReachedSet);
        ParticularReferenceValue value = (ParticularReferenceValue) lastAbstractState.getVariableOrDefault(0, UNKNOWN).getValue();
        assertEquals("Hello World", value.value(),  "Hello World should be in local variable slot 0");

        JvmValueAbstractState printlnCall = getPrintlnCall(fooReachedSet);
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "Hello World", stackTopValue,"Hello World should be on the top of the stack");
    }

    @Test
    public void testApiKeyInterprocedural()
    {
        BamCache<MethodSignature> cache = runBamCpa("TestApiCall");

        ProgramLocationDependentReachedSet fooReachedSet = (ProgramLocationDependentReachedSet) cache
                .get(new MethodSignature("TestApiCall", "callAPI", "(Ljava/lang/String;)V"))
                .stream()
                .findFirst()
                .map(BlockAbstraction::getReachedSet)
                .orElse(new DefaultReachedSet());

        JvmValueAbstractState lastAbstractState = getLastAbstractState(fooReachedSet);
        ParticularReferenceValue local0 = (ParticularReferenceValue) lastAbstractState.getVariableOrDefault(0, UNKNOWN).getValue();
        assertEquals("yeKIPAyM", local0.value(),  "yekIPAyM should be in local variable slot 0");

        ParticularReferenceValue local1 = (ParticularReferenceValue) lastAbstractState.getVariableOrDefault(1, UNKNOWN).getValue();
        assertInstanceOf(ParticularReferenceValue.class, local1);
        assertInstanceOf(StringBuilder.class, local1.value(), "A StringBuilder should be in local variable slot 1");

        JvmValueAbstractState printlnCall = getMethodCall(fooReachedSet, "call");
        ReferenceValue stackTop = printlnCall.getFrame().getOperandStack().peek().getValue().referenceValue();
        Object stackTopValue = stackTop.value();
        assertEquals( "MyAPIKey", stackTopValue,"MyAPIKey should be on the top of the stack");
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

    private static BamCache<MethodSignature> runBamCpa(String className)
    {
        ClassPool programClassPool = getProgramClassPool(className);
        JvmCfa cfa = CfaUtil.createInterproceduralCfa(programClassPool, ClassPoolBuilder.Companion.getLibraryClassPool());

        if (DEBUG)
        {
            try {
                Files.write(new File("graph.dot").toPath(), CfaUtil.toDot(cfa).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ProgramClass clazz = (ProgramClass) programClassPool.getClass(className);
        MethodSignature mainSignature = (MethodSignature) Signature.of(clazz, clazz.findMethod("main", null));
        JvmValueBamCpaRun bamCpaRun = new JvmValueBamCpaRun.Builder(cfa, mainSignature).build();
        bamCpaRun.execute();
        return bamCpaRun.getCpa().getCache();
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
