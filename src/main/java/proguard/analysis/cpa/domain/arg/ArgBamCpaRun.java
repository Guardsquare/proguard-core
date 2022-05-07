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

package proguard.analysis.cpa.domain.arg;

import java.util.Collection;
import java.util.stream.Collectors;
import proguard.classfile.Signature;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.bam.RebuildOperator;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.BamCpaRun;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;

/**
 * This {@link BamCpaRun} wraps the given {@link BamCpaRun} with an ARG generator.
 *
 * @author Dmitry Ivanov
 */
public class ArgBamCpaRun<CpaT extends ConfigurableProgramAnalysis,
                          AbstractStateT extends AbstractState,
                          CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
                          CfaEdgeT extends CfaEdge<CfaNodeT>,
                          SignatureT extends Signature>
    extends BamCpaRun<ArgCpa, ArgAbstractState, CfaNodeT, CfaEdgeT, SignatureT>
{

    protected final BamCpaRun<CpaT, AbstractStateT, CfaNodeT, CfaEdgeT, SignatureT> wrappedBamCpaRun;
    protected final ArgAbstractStateFactory                                         argAbstractStateFactory;

    /**
     * Create an ARG BAM CPA run.
     *
     * @param wrappedBamCpaRun        a BAM CPA run to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgBamCpaRun(BamCpaRun<CpaT, AbstractStateT, CfaNodeT, CfaEdgeT, SignatureT> wrappedBamCpaRun, ArgAbstractStateFactory argAbstractStateFactory)
    {
        super(wrappedBamCpaRun.getAbortOperator(), wrappedBamCpaRun.getMaxCallStackDepth());
        this.wrappedBamCpaRun = wrappedBamCpaRun;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for BamCpaRun

    @Override
    public ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT> createReduceOperator()
    {
        return new ArgReduceOperator<>(wrappedBamCpaRun.createReduceOperator(), argAbstractStateFactory);
    }

    @Override
    public ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT> createExpandOperator()
    {
        return new ArgExpandOperator<>(wrappedBamCpaRun.createExpandOperator(), argAbstractStateFactory);
    }

    @Override
    public RebuildOperator createRebuildOperator()
    {
        return new ArgRebuildOperator(wrappedBamCpaRun.createRebuildOperator(), argAbstractStateFactory);
    }

    @Override
    public Cfa<CfaNodeT, CfaEdgeT, SignatureT> getCfa()
    {
        return wrappedBamCpaRun.getCfa();
    }

    @Override
    public SignatureT getMainSignature()
    {
        return wrappedBamCpaRun.getMainSignature();
    }

    @Override
    public ArgCpa createIntraproceduralCPA()
    {
        ConfigurableProgramAnalysis wrappedCpa = wrappedBamCpaRun.createIntraproceduralCPA();
        return new ArgCpa(new ArgAbstractDomain(wrappedCpa.getAbstractDomain(), argAbstractStateFactory),
                          new ArgProgramLocationDependentTransferRelation<>(wrappedCpa.getTransferRelation(), argAbstractStateFactory),
                          new ArgMergeOperator(wrappedCpa.getMergeOperator(), argAbstractStateFactory),
                          new ArgStopOperator(wrappedCpa.getStopOperator()),
                          new ArgPrecisionAdjustment(wrappedCpa.getPrecisionAdjustment(), argAbstractStateFactory));
    }

    // implementations for CpaRun

    @Override
    public Collection<ArgAbstractState> getInitialStates()
    {
        return wrappedBamCpaRun.getInitialStates().stream().map(argAbstractStateFactory::createArgAbstractState).collect(Collectors.toList());
    }

    @Override
    protected ReachedSet createReachedSet()
    {
        return new ProgramLocationDependentReachedSet<>();
    }
}
