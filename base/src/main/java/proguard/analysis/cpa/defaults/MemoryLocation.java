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
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;

/**
 * A {@link MemoryLocation} points at a specific {@link AbstractState} in a compound {@link AbstractState}s for some {@link CfaNode}.
 *
 * @author Dmitry Ivanov
 */
public abstract class MemoryLocation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
                                     CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature,
                                     ValueT extends AbstractState, AbstractStateT extends AbstractState>
{

    /**
     * Returns an {@link AbstractState} representing value stored at the {@link MemoryLocation} in the input {@link AbstractState}.
     * If the {@link AbstractState} does not specify a {@link ValueT} at the {@link MemoryLocation}, the default value is returned.
     */
    public abstract <T extends ValueT> T extractValueOrDefault(AbstractStateT abstractState, T defaultValue);

    // implementations for Object

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
