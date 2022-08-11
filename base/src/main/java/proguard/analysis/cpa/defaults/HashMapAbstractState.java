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

import java.util.HashMap;
import java.util.Map;

/**
 * This {@link HashMapAbstractState} represents a map to {@link LatticeAbstractState}s with the semilattice operators lifted to the map.
 *
 * @author Dmitry Ivanov
 */
public class HashMapAbstractState<KeyT, AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends HashMap<KeyT, AbstractSpaceT>
    implements MapAbstractState<KeyT, AbstractSpaceT>
{

    /**
     * Create an empty hash map abstract state.
     */
    public HashMapAbstractState()
    {
    }

    /**
     * Create an empty hash map abstract state with reserved initial capacity.
     *
     * @param initialCapacity the initial capacity of the hash table
     */
    public HashMapAbstractState(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Create a hash map abstract state from another map.
     *
     * @param m map which elements are used for initialization
     */
    public HashMapAbstractState(Map<? extends KeyT, ? extends AbstractSpaceT> m) {
        super(m);
    }

    // implementations for AbstractState

    @Override
    public HashMapAbstractState<KeyT, AbstractSpaceT> copy()
    {
        return new HashMapAbstractState<>(this);
    }

}
