/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.analysis.cpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE;
import static proguard.testutils.JavaUtilKt.getCurrentJavaHome;

import io.kotest.core.spec.style.AnnotationSpec.After;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCacheImpl;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.bam.NoOpRebuildOperator;
import proguard.analysis.cpa.bam.RebuildOperator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.StaticPrecisionAdjustment;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentTransferRelation;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultExpandOperator;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState;
import proguard.analysis.cpa.jvm.util.CfaUtil;
import proguard.classfile.ClassPool;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.evaluation.value.BasicValueFactory;
import proguard.evaluation.value.ParticularDoubleValue;
import proguard.evaluation.value.ParticularIntegerValue;
import proguard.evaluation.value.UnknownValue;
import proguard.testutils.ClassPoolBuilder;
import proguard.testutils.FileSource;
import proguard.testutils.TestSource;
import proguard.testutils.cpa.ExpressionAbstractState;
import proguard.testutils.cpa.ExpressionTransferRelation;
import proguard.testutils.cpa.InstructionExpression;
import proguard.testutils.cpa.MethodExpression;
import proguard.testutils.cpa.ValueExpression;

/**
 * Test for {@link BamCpa}.
 */
public class BamCpaAlgorithmTest
{

    JvmCfa cfa;

    @After
    public void clearCfa()
    {
        if (cfa != null)
        {
            cfa.clear();
        }
    }

    @Test
    public void interproceduralResultsTest()
    {
        ClassPool programClassPool = ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/BamCpaAlgorithmTest/interprocedural.java"))},
                                                                           Arrays.asList("-source", "1.8", "-target", "1.8"),
                                                                           Collections.emptyList(),
                                                                           getCurrentJavaHome(),
                                                                           true)
                                                               .getProgramClassPool();
        cfa                        = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);

        AbstractDomain abstractDomain      = new DelegateAbstractDomain<ExpressionAbstractState>();
        ProgramLocationDependentTransferRelation<JvmCfaNode, JvmCfaEdge, MethodSignature> transferRelation    = new ExpressionTransferRelation();
        MergeOperator mergeOperator       = new MergeJoinOperator(abstractDomain);
        StopOperator stopOperator        = new StopSepOperator(abstractDomain);
        PrecisionAdjustment precisionAdjustment = new StaticPrecisionAdjustment();
        ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> reduceOperator      = new JvmDefaultReduceOperator<>();
        ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> expandOperator      = new JvmDefaultExpandOperator<>(cfa);
        RebuildOperator rebuildOperator     = new NoOpRebuildOperator();
        CpaWithBamOperators<JvmCfaNode, JvmCfaEdge, MethodSignature> wrappedCpa = new CpaWithBamOperators<>(abstractDomain, transferRelation, mergeOperator, stopOperator, precisionAdjustment,
                                                                                                            reduceOperator, expandOperator,
                                                                                                            rebuildOperator);

        ProgramClass    clazzA        = (ProgramClass) programClassPool.getClass("A");
        Method          mainMethod    = Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("main")).findFirst().get();
        MethodSignature mainSignature = MethodSignature.computeIfAbsent(clazzA, mainMethod);

        BamCache<MethodSignature> cache = new BamCacheImpl<>();

        BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa = new BamCpa<>(wrappedCpa, cfa, mainSignature, cache);

        JvmCfaNode node = cfa.getFunctionEntryNode(mainSignature);

        JvmAbstractState<ExpressionAbstractState> emptyState = new JvmAbstractState<>(
            node,
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        ReachedSet reached = new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<ExpressionAbstractState>, MethodSignature>();
        reached.add(emptyState);
        Waitlist waitlist = new BreadthFirstWaitlist();
        waitlist.add(emptyState);

        new CpaAlgorithm(bamCpa).run(reached, waitlist);

        assertEquals(cfa.getFunctionNodes(mainSignature).size(), reached.asCollection().size());

        // test result foo function call:
        // tests call of static method
        // tests usage of static field in another method
        List<AbstractState> returnStates8 = reached.asCollection().stream().filter(s -> ((ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>) s).getProgramLocation().getOffset() == 8)
                                                   .collect(Collectors.toList());
        assertEquals(1, returnStates8.size());
        AbstractState returnState8 = returnStates8.get(0);

        HashMapAbstractState<String, ExpressionAbstractState> staticFields = new HashMapAbstractState();
        staticFields.put("A.a:I", new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(4)))));

        JvmAbstractState<ExpressionAbstractState> expected8 = new JvmAbstractState<>(
            cfa.getFunctionNode(mainSignature, 8),
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            staticFields
        );

        ExpressionAbstractState addResult8 = new ExpressionAbstractState(
            Collections.singleton(
                new InstructionExpression(
                    new SimpleInstruction(Instruction.OP_IADD),
                    Arrays.asList(
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(4)))),
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(2))))
                    )
                )
            )
        );

        ExpressionAbstractState mulResult8 = new ExpressionAbstractState(
            Collections.singleton(
                new InstructionExpression(
                    new SimpleInstruction(Instruction.OP_IMUL),
                    Arrays.asList(
                        addResult8,
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(42))))
                    )
                )
            )
        );

        expected8.push(mulResult8);
        assertEquals(returnState8, expected8);

        // test result fee function call:
        // tests call to instance method
        // tests double argument
        // tests rebuild with non empty stack
        List<AbstractState> returnStates26 = reached.asCollection().stream().filter(s -> ((ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>) s).getProgramLocation().getOffset() == 26)
                                                    .collect(Collectors.toList());
        assertEquals(1, returnStates26.size());
        AbstractState returnState26 = returnStates26.get(0);

        JvmAbstractState<ExpressionAbstractState> expected26 = new JvmAbstractState<>(
            cfa.getFunctionNode(mainSignature, 26),
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            staticFields
        );

        expected26.setVariable(0, mulResult8, null);

        ExpressionAbstractState i2dResult26 = new ExpressionAbstractState(
            Collections.singleton(
                new InstructionExpression(
                    new SimpleInstruction(Instruction.OP_I2D),
                    Collections.singletonList(
                        mulResult8
                    )
                )
            )
        );

        ExpressionAbstractState addResult26 = new ExpressionAbstractState(
            Collections.singleton(
                new InstructionExpression(
                    new SimpleInstruction(Instruction.OP_DADD),
                    Arrays.asList(
                        i2dResult26,
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE))),
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularDoubleValue(2.0)))),
                        new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))
                    )
                )
            )
        );

        expected26.push(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE))));
        expected26.push(new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularDoubleValue(42.0)))));
        expected26.push(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE))));
        expected26.push(addResult26);
        assertEquals(expected26, returnState26);
    }

    @Test
    public void limitedRecursionTest()
    {
        ClassPool programClassPool = ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/BamCpaAlgorithmTest/recursive.java"))},
                                                                           Arrays.asList("-source", "1.8", "-target", "1.8"),
                                                                           Collections.emptyList(),
                                                                           getCurrentJavaHome(),
                                                                           true)
                                                               .getProgramClassPool();
        cfa                        = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);

        AbstractDomain                                                                    abstractDomain      = new DelegateAbstractDomain<ExpressionAbstractState>();
        ProgramLocationDependentTransferRelation<JvmCfaNode, JvmCfaEdge, MethodSignature> transferRelation    = new ExpressionTransferRelation();
        MergeOperator                                                                     mergeOperator       = new MergeJoinOperator(abstractDomain);
        StopOperator                                                                      stopOperator        = new StopSepOperator(abstractDomain);
        PrecisionAdjustment                                                               precisionAdjustment = new StaticPrecisionAdjustment();
        ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>                           reduceOperator      = new JvmDefaultReduceOperator<>();
        ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>                           expandOperator      = new JvmDefaultExpandOperator<>(cfa);
        RebuildOperator                                                                   rebuildOperator     = new NoOpRebuildOperator();
        CpaWithBamOperators<JvmCfaNode, JvmCfaEdge, MethodSignature> wrappedCpa = new CpaWithBamOperators<>(abstractDomain, transferRelation, mergeOperator, stopOperator, precisionAdjustment,
                                                                                                            reduceOperator, expandOperator,
                                                                                                            rebuildOperator);

        ProgramClass    clazzA        = (ProgramClass) programClassPool.getClass("A");
        Method          mainMethod    = Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("main")).findFirst().get();
        MethodSignature mainSignature = MethodSignature.computeIfAbsent(clazzA, mainMethod);

        BamCache<MethodSignature> cache = new BamCacheImpl<>();

        BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa = new BamCpa<>(wrappedCpa, cfa, mainSignature, cache, 3, NeverAbortOperator.INSTANCE);

        JvmCfaNode node = cfa.getFunctionEntryNode(mainSignature);

        JvmAbstractState<ExpressionAbstractState> emptyState = new JvmAbstractState<ExpressionAbstractState>(
            node,
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        ReachedSet reached = new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<ExpressionAbstractState>, MethodSignature>();
        reached.add(emptyState);
        Waitlist waitlist = new BreadthFirstWaitlist();
        waitlist.add(emptyState);

        new CpaAlgorithm(bamCpa).run(reached, waitlist);

        assertEquals(cfa.getFunctionNodes(mainSignature).size(), reached.asCollection().size());

        // test result of recursive calls with a max call stack limited to 3 (main function and 2 calls to sum function
        List<AbstractState> returnStates = reached.asCollection().stream().filter(s -> ((ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>) s).getProgramLocation().getOffset() == 5)
                                                  .collect(Collectors.toList());
        assertEquals(1, returnStates.size());
        AbstractState returnState = returnStates.get(0);

        HashMapAbstractState<String, ExpressionAbstractState> staticFields = new HashMapAbstractState();
        staticFields.put("A.a", new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(4)))));

        JvmAbstractState<ExpressionAbstractState> expected = new JvmAbstractState<>(
            cfa.getFunctionNode(mainSignature, 5),
            new JvmFrameAbstractState<ExpressionAbstractState>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        // result of the first iteration
        ValueExpression baseCase0 = new ValueExpression(new ParticularIntegerValue(2));

        // result of the second iteration
        InstructionExpression baseCase1 = new InstructionExpression(
            new SimpleInstruction(Instruction.OP_IADD),
            Arrays.asList(
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(2)))),
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(1))))
            )
        );

        InstructionExpression a1 = new InstructionExpression(
            new SimpleInstruction(Instruction.OP_ISUB),
            Arrays.asList(
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(4)))),
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(1))))
            )
        );

        InstructionExpression b2 = new InstructionExpression(
            new SimpleInstruction(Instruction.OP_IADD),
            Arrays.asList(
                new ExpressionAbstractState(Collections.singleton(baseCase1)),
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(1))))
            )
        );

        InstructionExpression a2 = new InstructionExpression(
            new SimpleInstruction(Instruction.OP_ISUB),
            Arrays.asList(
                new ExpressionAbstractState(Collections.singleton(a1)),
                new ExpressionAbstractState(Collections.singleton(new ValueExpression(new ParticularIntegerValue(1))))
            )
        );

        // result of the intra-procedural transfer relation when max stack size is reached
        MethodExpression call1 = new MethodExpression(
            MethodSignature.computeIfAbsent(clazzA, Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("sum")).findFirst().get()).getFqn(),
            Arrays.asList(
                new ExpressionAbstractState(Collections.singleton(a2)),
                new ExpressionAbstractState(Collections.singleton(b2))
            )
        );

        // the merge join operator for ExpressionAbstractState adds the three possible results of the function call to the set
        ExpressionAbstractState expectedCallResult = new ExpressionAbstractState(
            Stream.of(
                baseCase0,
                baseCase1,
                call1
            ).collect(Collectors.toSet())
        );

        expected.push(expectedCallResult);
        assertEquals(expected, returnState);
    }

    @Test
    public void uncaughtExceptionTest()
    {
        ClassPool                                                    programClassPool    = ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/BamCpaAlgorithmTest/uncaughtException.java"))},
                                                                                                                                 Arrays.asList("-source", "1.8", "-target", "1.8"),
                                                                                                                                 Collections.emptyList(),
                                                                                                                                 getCurrentJavaHome(),
                                                                                                                                 true)
                                                                                                                     .getProgramClassPool();
        cfa                                                                              = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);
        AbstractDomain                                               abstractDomain      = new DelegateAbstractDomain<ExpressionAbstractState>();
        ProgramLocationDependentTransferRelation<JvmCfaNode,
                                                 JvmCfaEdge,
                                                 MethodSignature>    transferRelation    = new ExpressionTransferRelation();
        MergeOperator                                                mergeOperator       = new MergeJoinOperator(abstractDomain);
        StopOperator                                                 stopOperator        = new StopSepOperator(abstractDomain);
        PrecisionAdjustment                                          precisionAdjustment = new StaticPrecisionAdjustment();
        ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>      reduceOperator      = new JvmDefaultReduceOperator<>();
        ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>      expandOperator      = new JvmDefaultExpandOperator<>(cfa);
        RebuildOperator                                              rebuildOperator     = new NoOpRebuildOperator();
        CpaWithBamOperators<JvmCfaNode, JvmCfaEdge, MethodSignature> wrappedCpa          = new CpaWithBamOperators<>(abstractDomain,
                                                                                                                     transferRelation,
                                                                                                                     mergeOperator,
                                                                                                                     stopOperator,
                                                                                                                     precisionAdjustment,
                                                                                                                     reduceOperator,
                                                                                                                     expandOperator,
                                                                                                                     rebuildOperator);

        ProgramClass    clazzA        = (ProgramClass) programClassPool.getClass("A");
        Method          mainMethod    = Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("main")).findFirst().get();
        MethodSignature mainSignature = MethodSignature.computeIfAbsent(clazzA, mainMethod);

        BamCache<MethodSignature> cache = new BamCacheImpl<>();

        BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa = new BamCpa<>(wrappedCpa, cfa, mainSignature, cache);

        JvmCfaNode node = cfa.getFunctionEntryNode(mainSignature);

        JvmAbstractState<ExpressionAbstractState> emptyState = new JvmAbstractState<ExpressionAbstractState>(
            node,
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        ReachedSet reached = new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<ExpressionAbstractState>, MethodSignature>();
        reached.add(emptyState);
        Waitlist waitlist = new BreadthFirstWaitlist();
        waitlist.add(emptyState);

        new CpaAlgorithm(bamCpa).run(reached, waitlist);

        assertEquals(2, reached.asCollection().size());
        // the return location is not reached, only the function call (offset 0) and the exception exit (offset -2)
        assertEquals(Stream.of(0, -2).collect(Collectors.toSet()), reached.asCollection().stream().map(r -> ((JvmAbstractState<ExpressionAbstractState>) r).getProgramLocation().getOffset()).collect(Collectors.toSet()));
    }

    @Test
    public void caughtExceptionTest()
    {
        ClassPool                                                                         programClassPool    = ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/BamCpaAlgorithmTest/caughtException.java"))},
                                                                                                                                                      Arrays.asList("-source", "1.8", "-target", "1.8"),
                                                                                                                                                      Collections.emptyList(),
                                                                                                                                                      getCurrentJavaHome(),
                                                                                                                                                      true)
                                                                                                                                          .getProgramClassPool();
        cfa                                                                                                   = CfaUtil.createInterproceduralCfaFromClassPool(programClassPool);
        AbstractDomain                                                                    abstractDomain      = new DelegateAbstractDomain<ExpressionAbstractState>();
        ProgramLocationDependentTransferRelation<JvmCfaNode, JvmCfaEdge, MethodSignature> transferRelation    = new ExpressionTransferRelation();
        MergeOperator                                                                     mergeOperator       = new MergeJoinOperator(abstractDomain);
        StopOperator                                                                      stopOperator        = new StopSepOperator(abstractDomain);
        PrecisionAdjustment                                                               precisionAdjustment = new StaticPrecisionAdjustment();
        ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>                           reduceOperator      = new JvmDefaultReduceOperator<>();
        ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>                           expandOperator      = new JvmDefaultExpandOperator<>(cfa);
        RebuildOperator                                                                   rebuildOperator     = new NoOpRebuildOperator();
        CpaWithBamOperators<JvmCfaNode, JvmCfaEdge, MethodSignature> wrappedCpa = new CpaWithBamOperators<>(abstractDomain, transferRelation, mergeOperator, stopOperator, precisionAdjustment,
                                                                                                            reduceOperator, expandOperator,
                                                                                                            rebuildOperator);

        ProgramClass    clazzA        = (ProgramClass) programClassPool.getClass("A");
        Method          mainMethod    = Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("main")).findFirst().get();
        MethodSignature mainSignature = MethodSignature.computeIfAbsent(clazzA, mainMethod);

        BamCache<MethodSignature> cache = new BamCacheImpl<>();

        BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa = new BamCpa<>(wrappedCpa, cfa, mainSignature, cache);

        JvmCfaNode node = cfa.getFunctionEntryNode(mainSignature);

        JvmAbstractState<ExpressionAbstractState> emptyState = new JvmAbstractState<ExpressionAbstractState>(
            node,
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        ReachedSet reached = new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<ExpressionAbstractState>, MethodSignature>();
        reached.add(emptyState);
        Waitlist waitlist = new BreadthFirstWaitlist();
        waitlist.add(emptyState);

        new CpaAlgorithm(bamCpa).run(reached, waitlist);

        // all the nodes of the cfa are reached (included the catch node) and the exception output node
        assertEquals(1, cfa.getFunctionCatchNodes(mainSignature).size());
        assertEquals(cfa.getFunctionNodes(mainSignature).size() + cfa.getFunctionCatchNodes(mainSignature).size(), reached.asCollection().size());
        assertTrue(reached.asCollection().stream().map(r -> ((JvmAbstractState<ExpressionAbstractState>) r).getProgramLocation()).anyMatch(JvmCfaNode::isExceptionExitNode));
    }

    @Test
    public void successorNoCallEdge()
    {
        // Regression test: tests that a successor state is produced if the inter-procedural call edge is missing from the CFA

        ClassPool programClassPool = ClassPoolBuilder.Companion.fromSource(new TestSource[]{new FileSource(new File("src/test/resources/BamCpaAlgorithmTest/interprocedural.java"))},
                                                                           Arrays.asList("-source", "1.8", "-target", "1.8"),
                                                                           Collections.emptyList(),
                                                                           getCurrentJavaHome(),
                                                                           true)
                                                               .getProgramClassPool();
        // create just intra-procedural CFA, all inter-procedural call edges are missing
        cfa                        = CfaUtil.createIntraproceduralCfaFromClassPool(programClassPool);

        AbstractDomain abstractDomain      = new DelegateAbstractDomain<ExpressionAbstractState>();
        ProgramLocationDependentTransferRelation<JvmCfaNode, JvmCfaEdge, MethodSignature> transferRelation    = new ExpressionTransferRelation();
        MergeOperator mergeOperator       = new MergeJoinOperator(abstractDomain);
        StopOperator stopOperator        = new StopSepOperator(abstractDomain);
        PrecisionAdjustment precisionAdjustment = new StaticPrecisionAdjustment();
        ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> reduceOperator      = new JvmDefaultReduceOperator<>();
        ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> expandOperator      = new JvmDefaultExpandOperator<>(cfa);
        RebuildOperator rebuildOperator     = new NoOpRebuildOperator();
        CpaWithBamOperators<JvmCfaNode, JvmCfaEdge, MethodSignature> wrappedCpa = new CpaWithBamOperators<>(abstractDomain, transferRelation, mergeOperator, stopOperator, precisionAdjustment,
                                                                                                            reduceOperator, expandOperator,
                                                                                                            rebuildOperator);

        ProgramClass    clazzA        = (ProgramClass) programClassPool.getClass("A");
        Method          mainMethod    = Arrays.stream(clazzA.methods).filter(m -> m.getName(clazzA).equals("main")).findFirst().get();
        MethodSignature mainSignature = MethodSignature.computeIfAbsent(clazzA, mainMethod);

        BamCache<MethodSignature> cache = new BamCacheImpl<>();

        BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa = new BamCpa<>(wrappedCpa, cfa, mainSignature, cache);

        JvmCfaNode node = cfa.getFunctionEntryNode(mainSignature);

        JvmAbstractState<ExpressionAbstractState> emptyState = new JvmAbstractState<>(
            node,
            new JvmFrameAbstractState<>(),
            new JvmForgetfulHeapAbstractState<>(new ExpressionAbstractState(Collections.singleton(new ValueExpression(UNKNOWN_VALUE)))),
            new HashMapAbstractState<>()
        );

        ReachedSet reached = new ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmAbstractState<ExpressionAbstractState>, MethodSignature>();
        reached.add(emptyState);
        Waitlist waitlist = new BreadthFirstWaitlist();
        waitlist.add(emptyState);

        new CpaAlgorithm(bamCpa).run(reached, waitlist);

        assertEquals(cfa.getFunctionNodes(mainSignature).size(), reached.asCollection().size());
    }
}
