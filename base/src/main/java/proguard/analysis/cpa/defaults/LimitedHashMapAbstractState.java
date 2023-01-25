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
import java.util.Optional;
import proguard.analysis.cpa.util.TriFunction;
import proguard.analysis.cpa.util.TriPredicate;

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
     * @param removeElement determines whether the map limit is reached
     *                      if it returns an empty value, the map behaves as usual
     *                      otherwise, the returned key is removed from the map
     */
    public LimitedHashMapAbstractState(TriFunction<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT, Optional<KeyT>> removeElement)
    {
        super(removeElement);
    }

    /**
     * Create an empty limited hash map abstract state with reserved initial capacity.
     *
     * @param initialCapacity the initial capacity of the hash table
     * @param removeElement   determines whether the map limit is reached
     *                        if it returns an empty value, the map behaves as usual
     *                        otherwise, the returned key is removed from the map
     *
     */
    public LimitedHashMapAbstractState(int initialCapacity, TriFunction<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT, Optional<KeyT>> removeElement)
    {
        super(initialCapacity, removeElement);
    }

    /**
     * Create a hash map abstract state from another map.
     *
     * @param m             map which elements are used for initialization
     * @param removeElement determines whether the map limit is reached
     *                      if it returns an empty value, the map behaves as usual
     *                      otherwise, the returned key is removed from the map
     */
    public LimitedHashMapAbstractState(Map<? extends KeyT, ? extends AbstractSpaceT> m, TriFunction<LimitedHashMap<KeyT, AbstractSpaceT>, KeyT, AbstractSpaceT, Optional<KeyT>> removeElement) {
        super(m, removeElement);
    }

    // implementations for AbstractState

    @Override
    public LimitedHashMapAbstractState<KeyT, AbstractSpaceT> copy()
    {
        return new LimitedHashMapAbstractState<>(this, removeElement);
    }

}
