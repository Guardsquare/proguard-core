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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;

/**
 * This {@link ArgAbstractState} is extended for the case when the wrapped abstract state is program location dependent.
 *
 * @author Dmitry Ivanov
 */
public class ArgProgramLocationDependentAbstractState<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    extends ArgAbstractState
    implements ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>
{

    /**
     * Create a program location dependent ARG node wrapping the given {@code wrappedAbstractState}
     * and pointing at the {@code parents}.
     *
     * @param wrappedAbstractState an abstract state to be wrapped
     * @param parents              its ARG parents
     */
    public <AbstractStateT extends AbstractState
                                   & ProgramLocationDependent<CfaNodeT,
                                                              CfaEdgeT,
                                                              SignatureT>> ArgProgramLocationDependentAbstractState(AbstractStateT wrappedAbstractState,
                                                                                                                    List<? extends ArgProgramLocationDependentAbstractState<CfaNodeT,
                                                                                                                                                                            CfaEdgeT,
                                                                                                                                                                            SignatureT>> parents)
    {
        super(wrappedAbstractState, parents);
    }

    /**
     * Create a parentless program location dependent {@link ArgAbstractState} wrapping the given {@code wrappedAbstractState}.
     *
     * @param wrappedAbstractState an abstract state to be wrapped
     */
    public <AbstractStateT extends AbstractState & ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>> ArgProgramLocationDependentAbstractState(AbstractStateT wrappedAbstractState)
    {
        super(wrappedAbstractState, Collections.emptyList());
    }

    // implementations for ProgramLocationDependent

    @Override
    public CfaNodeT getProgramLocation()
    {
        return ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) wrappedAbstractState).getProgramLocation();
    }

    @Override
    public void setProgramLocation(CfaNodeT programLocation)
    {
        ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) wrappedAbstractState).setProgramLocation(programLocation);
    }

    // implementations for ArgAbstractState

    @Override
    public List<ArgProgramLocationDependentAbstractState<CfaNodeT, CfaEdgeT, SignatureT>> getParents()
    {
        return (List<ArgProgramLocationDependentAbstractState<CfaNodeT, CfaEdgeT, SignatureT>>) super.getParents();
    }

    // implementations for AbstractState

    @Override
    public ArgProgramLocationDependentAbstractState<CfaNodeT, CfaEdgeT, SignatureT> copy()
    {
        return new ArgProgramLocationDependentAbstractState<>((AbstractState & ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) getWrappedState().copy(),
                                                                                            new ArrayList<>(getParents()));
    }
}
