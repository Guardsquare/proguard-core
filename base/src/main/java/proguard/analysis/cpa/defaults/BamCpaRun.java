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

package proguard.analysis.cpa.defaults;

import java.util.Set;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCacheImpl;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.bam.NoOpRebuildOperator;
import proguard.analysis.cpa.bam.RebuildOperator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.classfile.Signature;

/**
 * This abstract wrapper class constructs a {@link CpaWithBamOperators} based on the intraprocedural {@link ConfigurableProgramAnalysis}, runs it, and returns
 * the {@link proguard.analysis.cpa.interfaces.ReachedSet}.
 *
 * @author Dmitry Ivanov
 */
public abstract class BamCpaRun<CpaT extends ConfigurableProgramAnalysis,
                                AbstractStateT extends AbstractState,
                                CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
                                CfaEdgeT extends CfaEdge<CfaNodeT>,
                                SignatureT extends Signature>
    extends CpaRun<BamCpa<CfaNodeT, CfaEdgeT, SignatureT>, AbstractStateT>
{

    private final   int     maxCallStackDepth;
    protected final boolean reduceHeap;

    /**
     * Create a BAM CPA run.
     *
     * @param abortOperator     an abort operator
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally
     *                          0 means intraprocedural analysis
     *                          < 0 means no maximum depth
     */
    protected BamCpaRun(AbortOperator abortOperator, int maxCallStackDepth)
    {
        this(abortOperator, maxCallStackDepth, true);
    }

    /**
     * Create a BAM CPA run.
     *
     * @param abortOperator     an abort operator
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally
     *                          0 means intraprocedural analysis
     *                          < 0 means no maximum depth
     * @param reduceHeap        whether reduction/expansion of the heap state is performed at call/return sites
     */
    protected BamCpaRun(AbortOperator abortOperator, int maxCallStackDepth, boolean reduceHeap)
    {
        this.abortOperator = abortOperator;
        this.maxCallStackDepth = maxCallStackDepth;
        this.reduceHeap = reduceHeap;
    }

    // implementations for CpaRun


    @Override
    public BamCpa<CfaNodeT, CfaEdgeT, SignatureT> getCpa()
    {
        return cpa == null
               ? cpa = new BamCpa<>(new CpaWithBamOperators<>(createIntraproceduralCPA(),
                                                              createReduceOperator(),
                                                              createExpandOperator(),
                                                              createRebuildOperator()),
                                    getCfa(),
                                    getMainSignature(),
                                    createCache(),
                                    getMaxCallStackDepth(),
                                    abortOperator)
               : super.getCpa();
    }

    /**
     * Returns the intraprocedural CPA.
     */
    public abstract CpaT createIntraproceduralCPA();

    /**
     * Returns the reduce operator.
     */
    public abstract ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT> createReduceOperator();

    /**
     * Returns the expand operator.
     */
    public abstract ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT> createExpandOperator();

    /**
     * Returns the rebuild operator.
     */
    public RebuildOperator createRebuildOperator()
    {
        return new NoOpRebuildOperator();
    }

    /**
     * Returns a fresh BAM cache.
     */
    public BamCache<SignatureT> createCache()
    {
        return new BamCacheImpl<>();
    }

    /**
     * Returns the CFA.
     */
    public abstract Cfa<CfaNodeT, CfaEdgeT, SignatureT> getCfa();

    /**
     * Returns the signature of the main procedure.
     */
    public abstract SignatureT getMainSignature();

    /**
     * Returns the maximal call stack depth. If negative the maximum call stack depth is unlimited.
     */
    public int getMaxCallStackDepth()
    {
        return maxCallStackDepth;
    }

    public Set<SignatureT> getAnalyzedMethods()
    {
        return getCpa().getCache().getAllMethods();
    }

    /**
     * A builder for {@link BamCpaRun}. It assumes either the best performing parameters or the most basic one, if there is no absolute benefit.
     *
     * @author Dmitry Ivanov
     */
    public static abstract class Builder
    {

        protected int           maxCallStackDepth = -1;
        protected AbortOperator abortOperator     = NeverAbortOperator.INSTANCE;
        protected boolean       reduceHeap        = true;

        /**
         * Returns the {@link BamCpaRun} for given parameters.
         */
        public abstract BamCpaRun<?, ?, ?, ?, ?> build();

        /**
         * Sets the call stack limit for the interprocedural analysis.
         */
        public Builder setMaxCallStackDepth(int maxCallStackDepth)
        {
            this.maxCallStackDepth = maxCallStackDepth;
            return this;
        }

        /**
         * Sets the abort operator for premature CPA algorithm termination.
         */
        public Builder setAbortOperator(AbortOperator abortOperator)
        {
            this.abortOperator = abortOperator;
            return this;
        }

        /**
         * Sets whether the heap should be reduced before method calls.
         */
        public Builder setReduceHeap(boolean reduceHeap)
        {
            this.reduceHeap = reduceHeap;
            return this;
        }
    }
}
