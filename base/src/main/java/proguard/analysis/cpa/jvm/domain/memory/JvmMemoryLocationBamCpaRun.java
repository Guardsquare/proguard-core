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

package proguard.analysis.cpa.jvm.domain.memory;

import proguard.analysis.cpa.defaults.BamCpaRun;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SequentialCpaRun;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.classfile.MethodSignature;

/**
 * This abstract analyzer runs the {@link JvmMemoryLocationCpa} and returns the {@link ReachedSet}.
 * The reached set contains a flattened forest of memory location traces. Their roots are the endpoints. For each root a tree of source
 * {@link JvmMemoryLocation}s is constructed. The threshold defines the minimal value above which the {@link AbstractState} should be
 * in order to be included into the continuation of the trace.
 *
 * @param <AbstractStateT> The type of the values of the traced analysis.
 *
 * @author Dmitry Ivanov
 */
public abstract class JvmMemoryLocationBamCpaRun<CpaT extends ConfigurableProgramAnalysis, AbstractStateT extends LatticeAbstractState<AbstractStateT>>
    extends SequentialCpaRun<JvmMemoryLocationCpa<AbstractStateT>,
                             JvmMemoryLocationAbstractState<?>,
                             BamCpaRun<CpaT, ? extends ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>, JvmCfaNode, JvmCfaEdge, MethodSignature>>
    implements TraceExtractor
{

    protected final AbstractStateT threshold;

    /**
     * Create a CPA run.
     *
     * @param inputCpaRun   an unwrapped traced CPA BAM run
     * @param threshold     a cut-off threshold
     */
    public JvmMemoryLocationBamCpaRun(BamCpaRun<CpaT, JvmAbstractState<AbstractStateT>, JvmCfaNode, JvmCfaEdge, MethodSignature> inputCpaRun, AbstractStateT threshold)
    {
        this(inputCpaRun, threshold, NeverAbortOperator.INSTANCE);
    }

    /**
     * Create a CPA run.
     *
     * @param inputCpaRun   a traced CPA BAM run wrapped with an ARG computation
     * @param threshold     a cut-off threshold
     * @param abortOperator an abort operator
     */
    public JvmMemoryLocationBamCpaRun(BamCpaRun<CpaT, JvmAbstractState<AbstractStateT>, JvmCfaNode, JvmCfaEdge, MethodSignature> inputCpaRun,
                                      AbstractStateT threshold,
                                      AbortOperator abortOperator)
    {
        super(inputCpaRun);
        this.threshold = threshold;
        cpa = new JvmMemoryLocationCpa<>(threshold, inputCpaRun.getCpa());
        this.abortOperator = abortOperator;
    }

    // implementations for CpaRun

    @Override
    public ReachedSet createReachedSet()
    {
        return new ProgramLocationDependentReachedSet<>();
    }

    @Override
    protected Waitlist createWaitlist()
    {
        return new BreadthFirstWaitlist();
    }

    // implementations for TraceExtractor

    @Override
    public ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmMemoryLocationAbstractState<?>, MethodSignature> getOutputReachedSet()
    {
        return (ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmMemoryLocationAbstractState<?>, MethodSignature>) super.getOutputReachedSet();
    }
}
