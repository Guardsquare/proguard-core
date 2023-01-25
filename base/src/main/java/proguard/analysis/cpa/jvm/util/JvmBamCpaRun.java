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

package proguard.analysis.cpa.jvm.util;

import java.util.Arrays;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.BamCpaRun;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.JvmCompositeHeapReduceOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceReduceOperator;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.classfile.MethodSignature;

/**
 * A JVM instance of {@link BamCpaRun} uses a reached set optimized for program location-dependent analysis.
 *
 * @author Dmitry Ivanov
 */
public abstract class JvmBamCpaRun<CpaT extends ConfigurableProgramAnalysis, AbstractStateT extends LatticeAbstractState<AbstractStateT>, OuterAbstractStateT extends AbstractState>
    extends BamCpaRun<CpaT, OuterAbstractStateT, JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    protected    JvmCfa    cfa;
    public final HeapModel heapModel;

    /**
     * Create a JVM BAM CPA run.
     *
     * @param cfa               a CFA
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally
     *                          0 means intraprocedural analysis
     *                          < 0 means no maximum depth
     */
    protected JvmBamCpaRun(JvmCfa cfa, int maxCallStackDepth)
    {
        this(cfa, maxCallStackDepth, HeapModel.FORGETFUL, NeverAbortOperator.INSTANCE, true);
    }

    /**
     * Create a JVM BAM CPA run.
     *
     * @param cfa               a CFA
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally
     *                          0 means intraprocedural analysis
     *                          < 0 means no maximum depth
     * @param abortOperator     an abort operator
     * @param reduceHeap        whether reduction/expansion of the heap state is performed at call/return sites
     */
    protected JvmBamCpaRun(JvmCfa cfa, int maxCallStackDepth, HeapModel heapModel, AbortOperator abortOperator, boolean reduceHeap)
    {
        super(abortOperator, maxCallStackDepth, reduceHeap);
        this.cfa = cfa;
        this.heapModel = heapModel;
    }

    // implementations for BamCpaRun

    @Override
    public JvmCfa getCfa()
    {
        return cfa;
    }

    @Override
    public ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createReduceOperator()
    {
        JvmDefaultReduceOperator<AbstractStateT> jvmReduceOperator = new JvmDefaultReduceOperator<>(reduceHeap);

        switch (heapModel)
        {
            case FORGETFUL:
                return jvmReduceOperator;
            case TREE:
            case TAINT_TREE:
                return new JvmCompositeHeapReduceOperator(Arrays.asList(new JvmReferenceReduceOperator(reduceHeap),
                                                                        jvmReduceOperator));
            default:
                throw new IllegalArgumentException("Heap model " + heapModel.name() + " is not supported by " + getClass().getName());
        }
    }

    // implementations for CpaRun

    @Override
    protected ReachedSet createReachedSet()
    {
        return new ProgramLocationDependentReachedSet<>();
    }

    /**
     * A builder for {@link JvmBamCpaRun}. It assumes either the best performing parameters or the most basic one, if there is no absolute benefit.
     *
     * @author Dmitry Ivanov
     */
    public static abstract class Builder extends BamCpaRun.Builder
    {

        protected JvmCfa    cfa;
        protected HeapModel heapModel = HeapModel.FORGETFUL;

        // implementations for BamCpaRun.Builder

        /**
         * Returns the {@link JvmBamCpaRun} for given parameters.
         */
        @Override
        public abstract JvmBamCpaRun<?, ?, ?> build();

        @Override
        public Builder setMaxCallStackDepth(int maxCallStackDepth)
        {
            return (Builder) super.setMaxCallStackDepth(maxCallStackDepth);
        }

        @Override
        public Builder setAbortOperator(AbortOperator abortOperator)
        {
            return (Builder) super.setAbortOperator(abortOperator);
        }

        @Override
        public Builder setReduceHeap(boolean reduceHeap)
        {
            return (Builder) super.setReduceHeap(reduceHeap);
        }

        /**
         * Sets the control flow automaton.
         */
        public Builder setCfa(JvmCfa cfa)
        {
            this.cfa = cfa;
            return this;
        }

        /**
         * Sets the heap model.
         */
        public Builder setHeapModel(HeapModel heapModel)
        {
            this.heapModel = heapModel;
            return this;
        }
    }
}
