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

import java.util.function.Predicate;
import proguard.analysis.cpa.defaults.DifferentialMap;
import proguard.analysis.cpa.defaults.DifferentialMapAbstractState;
import proguard.analysis.cpa.defaults.LatticeAbstractState;

/**
 * This interface contains a method creating a fresh instance of {@link DifferentialMapAbstractState}.
 *
 * @author Dmitry Ivanov
 */
public class DifferentialMapAbstractStateFactory<KeyT, StateT extends LatticeAbstractState<StateT>>
    implements MapAbstractStateFactory<KeyT, StateT>
{

    private final Predicate<DifferentialMap<KeyT, StateT>> shouldCollapse;

    /**
     * Create a differential map abstract state factory.
     *
     * @param shouldCollapse a collapse criterion
     */
    public DifferentialMapAbstractStateFactory(Predicate<DifferentialMap<KeyT, StateT>> shouldCollapse)
    {
        this.shouldCollapse = shouldCollapse;
    }

    // implementations for MapAbstractStateFactory

    @Override
    public DifferentialMapAbstractState<KeyT, StateT> createMapAbstractState()
    {
        return new DifferentialMapAbstractState<>(shouldCollapse);
    }
}
