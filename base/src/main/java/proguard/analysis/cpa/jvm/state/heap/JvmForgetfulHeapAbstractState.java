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

package proguard.analysis.cpa.jvm.state.heap;

import java.util.List;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * This is a forgetful stub heap implementation. It does not change and always returns the default value.
 *
 * @author Dmitry Ivanov
 */
public class JvmForgetfulHeapAbstractState<StateT extends LatticeAbstractState<StateT>>
    implements JvmHeapAbstractState<StateT>
{

    private final StateT defaultValue;

    /**
     * Create a forgetful heap abstract state returning the specified value for all queries.
     *
     * @param defaultValue the value to be returned by memory accesses
     */
    public JvmForgetfulHeapAbstractState(StateT defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    // implementations for JvmHeapAbstractState

    @Override
    public <T> StateT getFieldOrDefault(T object, String fqn, StateT defaultValue)
    {
        return defaultValue;
    }

    @Override
    public <T> void setField(T object, String fqn, StateT value)
    {
    }

    @Override
    public <T> StateT getArrayElementOrDefault(T array, StateT index, StateT defaultValue)
    {
        return this.defaultValue;
    }

    @Override
    public <T> void setArrayElement(T array, StateT index, StateT value)
    {
    }

    @Override
    public StateT newObject(String className, JvmCfaNode creationCite)
    {
        return defaultValue;
    }

    @Override
    public StateT newArray(String type, List<StateT> dimensions, JvmCfaNode creationCite)
    {
        return defaultValue;
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmForgetfulHeapAbstractState<StateT> join(JvmHeapAbstractState<StateT> abstractState)
    {
        return this;
    }

    @Override
    public boolean isLessOrEqual(JvmHeapAbstractState<StateT> abstractState)
    {
        return abstractState instanceof JvmForgetfulHeapAbstractState;
    }

    // implementations for AbstractState

    @Override
    public boolean equals(Object o)
    {
        return o instanceof JvmForgetfulHeapAbstractState;
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public JvmForgetfulHeapAbstractState<StateT> copy()
    {
        return this;
    }
}
