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

package proguard.analysis.cpa.state;

import proguard.analysis.cpa.defaults.HashMapAbstractState;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;

/**
 * This interface contains a method creating a fresh instance of {@link HashMapAbstractState}.
 *
 * @author Dmitry Ivanov
 */
public class HashMapAbstractStateFactory<KeyT, StateT extends LatticeAbstractState<StateT>>
    implements MapAbstractStateFactory<KeyT, StateT>
{

    private static final HashMapAbstractStateFactory<?, ?> INSTANCE = new HashMapAbstractStateFactory<>();

    private HashMapAbstractStateFactory()
    {
    }

    public static <K, V extends LatticeAbstractState<V>> HashMapAbstractStateFactory<K, V> getInstance()
    {
        return (HashMapAbstractStateFactory<K, V>) INSTANCE;
    }

    // implementations for MapAbstractStateFactory

    @Override
    public MapAbstractState<KeyT, StateT> createMapAbstractState()
    {
        return new HashMapAbstractState<>();
    }
}
