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

import java.util.Map;
import proguard.analysis.cpa.jvm.util.TriPredicate;

/**
 * This {@link LimitedHashMapAbstractState} represents a limited map to {@link LatticeAbstractState}s with the semilattice operators lifted to the map.
 *
 * @author Dmitry Ivanov
 */
public class LimitedHashMapAbstractState<KeyT, AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends LimitedHashMap<KeyT, AbstractSpaceT>
    implements MapAbstractState<KeyT, AbstractSpaceT>
{

    /**
     * Create an empty limited hash map abstract state.
     *
     * @param limitReached    whether the size limit of the map is reached and no further mappings can be added
     */
    public LimitedHashMapAbstractState(TriPredicate<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT> limitReached)
    {
        super(limitReached);
    }

    /**
     * Create an empty limited hash map abstract state with reserved initial capacity.
     *
     * @param initialCapacity the initial capacity of the hash table
     * @param limitReached    whether the size limit of the map is reached and no further mappings can be added
     *
     */
    public LimitedHashMapAbstractState(int initialCapacity, TriPredicate<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT> limitReached)
    {
        super(initialCapacity, limitReached);
    }

    /**
     * Create a hash map abstract state from another map.
     *
     * @param m map which elements are used for initialization
     * @param limitReached    whether the size limit of the map is reached and no further mappings can be added
     */
    public LimitedHashMapAbstractState(Map<? extends KeyT, ? extends AbstractSpaceT> m, TriPredicate<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT> limitReached) {
        super(m, limitReached);
    }

    // implementations for AbstractState

    @Override
    public LimitedHashMapAbstractState<KeyT, AbstractSpaceT> copy()
    {
        return new LimitedHashMapAbstractState<>(this, limitReached);
    }

}
