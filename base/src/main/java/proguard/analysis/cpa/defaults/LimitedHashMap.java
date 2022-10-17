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
import proguard.analysis.cpa.jvm.util.TriPredicate;

/**
 * This {@link LimitedHashMap} is a {@link HashMap} which limits its content based on the tripredicate {@code limitReached}.
 *
 * @author Dmitry Ivanov
 */
public class LimitedHashMap<K, V>
    extends HashMap<K, V>
{

    protected TriPredicate<LimitedHashMap<K, V>, K, V> limitReached;

    /**
     * Create an empty limited hash map.
     *
     * @param limitReached    whether the size limit of the map is reached and no further mappings can be added
     */
    public LimitedHashMap(TriPredicate<LimitedHashMap<K, V>, K, V> limitReached)
    {
        this.limitReached = limitReached;
    }

    /**
     * Create an empty limited map with reserved initial capacity.
     *
     * @param initialCapacity the initial capacity of the hash table
     * @param limitReached    whether the size limit of the map is reached and no further mappings can be added
     */
    public LimitedHashMap(int initialCapacity, TriPredicate<LimitedHashMap<K, V>, K, V> limitReached)
    {
        super(initialCapacity);
        this.limitReached = limitReached;
    }

    /**
     * Create a limited map from another map and a tripredicate.
     *
     * @param m            map which elements are used for initialization
     * @param limitReached whether the size limit of the map is reached and no further mappings can be added
     */
    public LimitedHashMap(Map<? extends K, ? extends V> m, TriPredicate<LimitedHashMap<K, V>, K, V> limitReached) {
        super(m);
        this.limitReached = limitReached;
    }

    // implementations for HashMap

    @Override
    public V put(K key, V value)
    {
        return limitReached.test(this, key, value) ? get(key) : super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        m.entrySet().forEach((e -> put(e.getKey(), e.getValue())));
    }
}
