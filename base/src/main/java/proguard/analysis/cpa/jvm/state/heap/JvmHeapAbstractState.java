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
import java.util.Set;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.classfile.Clazz;

/**
 * The {@link JvmHeapAbstractState} provides the interfaces for heap operations over objects and arrays.
 *
 * @author Dmitry Ivanov
 */
public interface JvmHeapAbstractState<StateT extends LatticeAbstractState<StateT>>
    extends LatticeAbstractState<JvmHeapAbstractState<StateT>>
{

    // overrides for LatticeAbstractState

    @Override
    JvmHeapAbstractState<StateT> copy();

    /**
     * Creates a new array of a given class with the given dimension sizes at a specific program point and returns a reference to it.
     */
    StateT newArray(String type, List<StateT> dimensions, JvmCfaNode creationCite);

    /**
     * Creates a new object of a given class at a specific program point and returns a reference to it.
     */
    StateT newObject(String className, JvmCfaNode creationCite);

    /**
     * Creates a new object of a given {@link Clazz} at a specific program point and returns a reference to it.
     */
    default StateT newObject(Clazz clazz, JvmCfaNode creationCite)
    {
        return newObject(clazz.getName(), creationCite);
    }

    /**
     * Returns a field {@code fqn} from a reference {@code object}.
     * If there is no abstract state representing the field, returns the {@code defaultValue}
     */
    <T> StateT getFieldOrDefault(T object, String fqn, StateT defaultValue);

    /**
     * Sets a {@code value} to a field {@code fqn} of a referenced {@code object}.
     */
    <T> void setField(T object, String fqn, StateT value);

    /**
     * Returns an {@code array} element at the specified {@code index} or the {@code defaultValue}, if the element is unset.
     */
    <T> StateT getArrayElementOrDefault(T array, StateT index, StateT defaultValue);

    /**
     * Sets the {@code array} element {@code value} at the specified {@code index}.
     */
    <T> void setArrayElement(T array, StateT index, StateT value);

    /**
     * Discards unused parts of the heap. Does nothing in the default implementation.
     *
     * This can be overridden to model discarding heap portions at call sites.
     *
     * @param references information on the references to keep or discard, based on the implementation. Unused in the default implementation
     */
    default void reduce(Set<Object> references)
    {
    }

    /**
     * Expands the heap with references present in another state. Does nothing in the default implementation.
     *
     * This can be overridden to model recovering information discarded at call sites when analyzing a return site.
     *
     * @param otherState a heap state from which expanding the heap (e.g. the state calling a method to recover information discarded from it)
     */
    default void expand(JvmHeapAbstractState<StateT> otherState)
    {
    }
}
