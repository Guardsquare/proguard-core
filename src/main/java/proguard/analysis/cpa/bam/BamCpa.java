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

package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.classfile.Signature;

/**
 * A {@link ConfigurableProgramAnalysis} for inter-procedural analysis using block abstraction memoization as described in {@see https://dl.acm.org/doi/pdf/10.1145/3368089.3409718}, which is defined
 * by a domain-dependent {@link CpaWithBamOperators} that adds three operators: reduce, expand, and rebuild. This allows an inter-procedural analysis running this CPA to be conducted by the standard
 * {@link CpaAlgorithm}.
 *
 * <p>A BAM CPA works on a domain-independent level and its abstract domain, merge operator, and stop operator are defined by the domain-dependent wrapped CPA. The main feature of a BAM CPA
 * is its transfer relation (see {@link BamTransferRelation} for details) that is able to extend the analysis of the wrapped CPA to the inter-procedural level.
 *
 * @author Carlo Alberto Pozzoli
 */
public class BamCpa<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    implements ConfigurableProgramAnalysis
{

    private final CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa;
    private final BamTransferRelation<CfaNodeT, CfaEdgeT, SignatureT> bamTransferRelation;

    /**
     * Create a BamCpa with default transfer relation.
     *
     * @param wrappedCpa   a wrapped CPA with BAM operators
     * @param cfa          a control flow automaton
     * @param mainFunction the signature of the main function of an analyzed program
     * @param cache        a cache for the block abstractions
     */
    public BamCpa(CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa, Cfa<CfaNodeT, CfaEdgeT, SignatureT> cfa, SignatureT mainFunction, BamCache<SignatureT> cache)
    {
        this(wrappedCpa, cfa, mainFunction, cache, -1, NeverAbortOperator.INSTANCE);
    }

    /**
     * Create a BamCpa with default transfer relation with a limited call depth. At the maximum call depth further function calls are just analyzed intra-procedurally.
     *
     * @param wrappedCpa        a wrapped cpa with BAM operators
     * @param cfa               a control flow automaton
     * @param mainFunction      the signature of a main function
     * @param cache             a cache for the block abstractions
     * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally.
     *                          0 means intra-procedural analysis.
     *                          < 0 means no maximum depth.
     * @param abortOperator     an abort operator used for computing block abstractions
     */
    public BamCpa(CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa,
                  Cfa<CfaNodeT, CfaEdgeT, SignatureT> cfa,
                  SignatureT mainFunction,
                  BamCache<SignatureT> cache,
                  int maxCallStackDepth,
                  AbortOperator abortOperator)
    {
        this.wrappedCpa = wrappedCpa;
        this.bamTransferRelation = new BamTransferRelation<>(wrappedCpa, cfa, mainFunction, cache, maxCallStackDepth, abortOperator);
    }

    /**
     * Create a BamCpa with a specific transfer relation. Use this constructor if a custom transfer relation is needed (e.g. with different internal waitlist/reached set implementations).
     *
     * @param transferRelation The transfer relation of the BamCpa
     */
    public BamCpa(BamTransferRelation<CfaNodeT, CfaEdgeT, SignatureT> transferRelation)
    {
        this.wrappedCpa = transferRelation.getWrappedCpa();
        this.bamTransferRelation = transferRelation;
    }

    // Implementations for ConfigurableProgramAnalysis

    /**
     * Returns the abstract domain of the wrapped CPA.
     */
    @Override
    public AbstractDomain getAbstractDomain()
    {
        return wrappedCpa.getAbstractDomain();
    }

    /**
     * Returns the BAM transfer relation, more details in {@link BamTransferRelation}.
     */
    @Override
    public BamTransferRelation<CfaNodeT, CfaEdgeT, SignatureT> getTransferRelation()
    {
        return bamTransferRelation;
    }

    /**
     * Returns the merge operator of the wrapped CPA.
     */
    @Override
    public MergeOperator getMergeOperator()
    {
        return wrappedCpa.getMergeOperator();
    }

    /**
     * Returns the stop operator of the wrapped CPA.
     */
    @Override
    public StopOperator getStopOperator()
    {
        return wrappedCpa.getStopOperator();
    }

    /**
     * Returns the precision adjustment of the wrapped CPA.
     */
    @Override
    public PrecisionAdjustment getPrecisionAdjustment()
    {
        return wrappedCpa.getPrecisionAdjustment();
    }

    /**
     * Returns the reduce operator of the wrapped CPA.
     */
    public ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT> getReduceOperator()
    {
        return wrappedCpa.getReduceOperator();
    }

    /**
     * Returns the expand operator of the wrapped CPA.
     */
    public ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT> getExpandOperator()
    {
        return wrappedCpa.getExpandOperator();
    }

    /**
     * Returns the rebuild operator of the wrapped CPA.
     */
    public RebuildOperator getRebuildOperator()
    {
        return wrappedCpa.getRebuildOperator();
    }

    /**
     * Returns the BAM cache used by the CPA.
     */
    public BamCache<SignatureT> getCache()
    {
        return bamTransferRelation.getCache();
    }

    /**
     * Returns the CFA used by the CPA.
     */
    public Cfa<CfaNodeT, CfaEdgeT, SignatureT> getCfa()
    {
        return bamTransferRelation.getCfa();
    }
}
