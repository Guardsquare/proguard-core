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
     * Returns a field {@code descriptor} from a reference {@code object}.
     * If there is no abstract state representing the field, returns the {@code defaultValue}
     */
    StateT getField(StateT object, String descriptor, StateT defaultValue);

    /**
     * Sets a {@code value} to a field {@code descriptor} of a referenced {@code object} and returns the {@code value}.
     */
    void setField(StateT object, String descriptor, StateT value);

    /**
     * Returns an {@code array} element at the specified {@code index} or the {@code defaultValue}, if the element is unset.
     */
    StateT getArrayElementOrDefault(StateT array, StateT index, StateT defaultValue);

    /**
     * Sets the {@code array} element {@code value} at the specified {@code index}.
     */
    void setArrayElement(StateT array, StateT index, StateT value);
}
