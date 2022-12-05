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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import proguard.analysis.cpa.util.TriFunction;

/**
 * This {@link LimitedHashMap} is a {@link HashMap} which limits its content based on the function {@code removeElement}.
 * {@code removeElement} determines whether the map limit is reached. If it returns an empty value, the map behaves as usual.
 * Otherwise, the returned key is removed from the map.
 *
 * @author Dmitry Ivanov
 */
public class LimitedHashMap<K, V>
    extends HashMap<K, V>
{

    protected TriFunction<LimitedHashMap<K, V>, K, V, Optional<K>> removeElement;

    /**
     * Create an empty limited hash map.
     *
     * @param removeElement determines whether the map limit is reached
     *                      if it returns an empty value, the map behaves as usual
     *                      otherwise, the returned key is removed from the map
     */
    public LimitedHashMap(TriFunction<LimitedHashMap<K, V>, K, V, Optional<K>> removeElement)
    {
        this.removeElement = removeElement;
    }

    /**
     * Create an empty limited map with reserved initial capacity.
     *
     * @param initialCapacity the initial capacity of the hash table
     * @param removeElement   determines whether the map limit is reached
     *                        if it returns an empty value, the map behaves as usual
     *                        otherwise, the returned key is removed from the map
     */
    public LimitedHashMap(int initialCapacity, TriFunction<LimitedHashMap<K, V>, K, V, Optional<K>> removeElement)
    {
        super(initialCapacity);
        this.removeElement = removeElement;
    }

    /**
     * Create a limited map from another map and a tripredicate.
     *
     * @param m             map which elements are used for initialization
     * @param removeElement determines whether the map limit is reached
     *                      if it returns an empty value, the map behaves as usual
     *                      otherwise, the returned key is removed from the map
     */
    public LimitedHashMap(Map<? extends K, ? extends V> m, TriFunction<LimitedHashMap<K, V>, K, V, Optional<K>> removeElement) {
        super(m);
        this.removeElement = removeElement;
    }

    // implementations for HashMap

    @Override
    public V put(K key, V value)
    {
        V oldValue = super.put(key, value);
        removeElement.apply(this, key, value).ifPresent(this::remove);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        m.entrySet().forEach((e -> put(e.getKey(), e.getValue())));
    }

    @Override
    public V putIfAbsent(K key, V value)
    {
        V oldValue = super.putIfAbsent(key, value);
        if (oldValue == null)
        {
            Optional.ofNullable(get(key))
                    .flatMap(v -> removeElement.apply(this, key, v))
                    .ifPresent(this::remove);
        }
        return oldValue;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
    {
        V oldValue = get(key);
        V newValue = super.computeIfAbsent(key, mappingFunction);
        if (oldValue == null)
        {
            Optional.ofNullable(newValue)
                    .flatMap(v -> removeElement.apply(this, key, v))
                    .ifPresent(this::remove);
        }
        return newValue;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
    {
        V newValue = super.compute(key, remappingFunction);
        Optional.ofNullable(newValue)
                .flatMap(v -> removeElement.apply(this, key, v))
                .ifPresent(this::remove);
        return newValue;
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
    {
        V newValue = super.merge(key, value, remappingFunction);
        Optional.ofNullable(newValue)
                .flatMap(v -> removeElement.apply(this, key, v))
                .ifPresent(this::remove);
        return newValue;
    }
}
