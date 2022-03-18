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

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;

/**
 * This abstract state factory creates an {@link ArgProgramLocationDependentAbstractState}
 * for a specific abstract state and its ARG parents.
 *
 * @author Dmitry Ivanov
 */
public class ArgProgramLocationDependentAbstractStateFactory<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    extends ArgAbstractStateFactory
{

    /**
     * Create an ARG node factory.
     */
    public ArgProgramLocationDependentAbstractStateFactory()
    {
        this(s -> true);
    }

    /**
     * Create an ARG node factory cutting off parents satisfying the predicate.
     *
     * @param cutOffPredicate a cut-off predicate specifying the termination of the ARG parenthood
     */
    public ArgProgramLocationDependentAbstractStateFactory(Predicate<AbstractState> cutOffPredicate)
    {
        this.cutOffPredicate = cutOffPredicate;
    }

    // implementations for ArgAbstractStateFactory

    @Override
    public ArgProgramLocationDependentAbstractState<CfaNodeT, CfaEdgeT, SignatureT> createArgAbstractState(AbstractState wrappedAbstractState, List<? extends ArgAbstractState> parents)
    {
        return new ArgProgramLocationDependentAbstractState<>((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> & AbstractState) wrappedAbstractState,
                                                              (List<? extends ArgProgramLocationDependentAbstractState<CfaNodeT,
                                                                                                                       CfaEdgeT,
                                                                                                                       SignatureT>>) parents.stream()
                                                                                                                                            .filter(p -> cutOffPredicate.test(p.getWrappedState()))
                                                                                                                                            .collect(Collectors.toList()));
    }

    @Override
    public ArgAbstractState createArgAbstractState(AbstractState wrappedAbstractState)
    {
        return new ArgProgramLocationDependentAbstractState<>((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> & AbstractState) wrappedAbstractState);
    }
}
