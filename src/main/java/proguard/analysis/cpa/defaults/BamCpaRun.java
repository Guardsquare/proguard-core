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

import proguard.classfile.Signature;
import proguard.analysis.cpa.bam.BamCache;
import proguard.analysis.cpa.bam.BamCacheImpl;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.bam.NoOpRebuildOperator;
import proguard.analysis.cpa.bam.RebuildOperator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;

/**
 * This abstract wrapper class constructs a {@link CpaWithBamOperators} based on the intraprocedural {@link ConfigurableProgramAnalysis}, runs it, and returns the {@link ReachedSet}.
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

    protected final int maxCallStackDepth;

    /**
     * Create a BAM CPA run.
     *
     * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally.
     *                          0 means intra-procedural analysis.
     *                          < 0 means no maximum depth.
     */
    protected BamCpaRun(int maxCallStackDepth)
    {
        this.maxCallStackDepth = maxCallStackDepth;
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
                                    maxCallStackDepth)
               : cpa;
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
     * Returns the max call stack depth of the BAM algorithm. If negative the maximum call stack depth is unlimited.
     */
    public int getMaxCallStackDepth()
    {
        return maxCallStackDepth;
    }
}
