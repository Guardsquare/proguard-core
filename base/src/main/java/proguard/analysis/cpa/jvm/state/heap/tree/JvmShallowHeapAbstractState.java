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

package proguard.analysis.cpa.jvm.state.heap.tree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A shallow heap models objects as atomic abstract states thus having only one level of depth. Object fields are not modeled. References of wrong types are ignored.
 *
 * @author Dmitry Ivanov
 */
public class JvmShallowHeapAbstractState<ReferenceT, StateT extends LatticeAbstractState<StateT>>
    implements JvmHeapAbstractState<StateT>
{
    private static final Logger logger = LogManager.getLogger(JvmShallowHeapAbstractState.class);

    public    final MapAbstractState<ReferenceT, StateT> referenceToObject;
    protected final Class<ReferenceT>                    referenceClass;
    protected final StateT                               defaultValue;

    /**
     * Create a shallow heap abstract state returning the specified value for all queries from an existing reference to abstract state map.
     *
     * @param defaultValue      the value to be returned by memory accesses
     * @param referenceClass    the class of the reference used for addressing
     * @param referenceToObject the value to be returned by memory accesses
     */
    public JvmShallowHeapAbstractState(MapAbstractState<ReferenceT, StateT> referenceToObject,
                                       Class<ReferenceT>                    referenceClass,
                                       StateT                               defaultValue)
    {
        this.referenceToObject = referenceToObject;
        this.referenceClass    = referenceClass;
        this.defaultValue      = defaultValue;
    }

    // implementations for JvmHeapAbstractState

    /**
     * Discards all the references not in {@param referencesToKeep}.
     */
    @Override
    public void reduce(Set<Object> referencesToKeep)
    {
        int     originalSize = referenceToObject.size();
        boolean anyRemoved   = referenceToObject.keySet().retainAll(referencesToKeep);
        logger.trace("Reduced heap: retain {}, original heap size {}; new heap size {}; anyRemoved = {}.", referencesToKeep.size(), originalSize, referenceToObject.size(), anyRemoved);
    }

    /**
     * Expands the state with all the entries from another heap state with reference not already known by the state.
     */
    @Override
    public void expand(JvmHeapAbstractState<StateT> otherState)
    {
        if (!(otherState instanceof JvmShallowHeapAbstractState))
        {
            throw new IllegalArgumentException("The other state should be a JvmShallowHeapAbstractState");
        }

        ((JvmShallowHeapAbstractState<ReferenceT, StateT>) otherState).referenceToObject.forEach(referenceToObject::putIfAbsent);
    }

    @Override
    public <T> StateT getFieldOrDefault(T object, String fqn, StateT defaultValue)
    {
        return referenceClass.isInstance(object) && fqn.length() == 0
               ? referenceToObject.getOrDefault(referenceClass.cast(object), defaultValue)
               : defaultValue;
    }

    @Override
    public <T> void setField(T object, String fqn, StateT value)
    {
        if (!(referenceClass.isInstance(object)) || fqn.length() > 0)
        {
            return;
        }
        referenceToObject.put(referenceClass.cast(object), value);
    }

    @Override
    public <T> StateT getArrayElementOrDefault(T array, StateT index, StateT defaultValue)
    {
        return referenceClass.isInstance(array)
               ? referenceToObject.getOrDefault(referenceClass.cast(array), defaultValue)
               :defaultValue;
    }

    @Override
    public <T> void setArrayElement(T array, StateT index, StateT value)
    {
        if (!(referenceClass.isInstance(array)))
        {
            return;
        }
        referenceToObject.put(referenceClass.cast(array), value);
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
    public JvmShallowHeapAbstractState<ReferenceT, StateT> join(JvmHeapAbstractState<StateT> abstractState)
    {
        JvmShallowHeapAbstractState<ReferenceT, StateT> other = (JvmShallowHeapAbstractState<ReferenceT, StateT>) abstractState;
        MapAbstractState<ReferenceT, StateT> newReferenceToState = referenceToObject.join(other.referenceToObject);
        if (referenceToObject == newReferenceToState)
        {
            return this;
        }
        if (other.referenceToObject == newReferenceToState)
        {
            return other;
        }
        return new JvmShallowHeapAbstractState<>(newReferenceToState, referenceClass, defaultValue);
    }

    @Override
    public boolean isLessOrEqual(JvmHeapAbstractState<StateT> abstractState)
    {
        return abstractState instanceof JvmShallowHeapAbstractState
               && referenceToObject.isLessOrEqual(((JvmShallowHeapAbstractState<ReferenceT, StateT>) abstractState).referenceToObject);
    }



    // implementations for AbstractState

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof JvmShallowHeapAbstractState))
        {
            return false;
        }
        JvmShallowHeapAbstractState<?, ?> that = (JvmShallowHeapAbstractState<?, ?>) o;
        return Objects.equals(referenceToObject, that.referenceToObject);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(referenceToObject);
    }

    @Override
    public JvmShallowHeapAbstractState<ReferenceT, StateT> copy()
    {
        return new JvmShallowHeapAbstractState<>(referenceToObject.copy(), referenceClass, defaultValue);
    }
}
