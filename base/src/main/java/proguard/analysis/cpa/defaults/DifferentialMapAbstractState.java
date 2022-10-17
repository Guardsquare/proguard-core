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

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This {@link DifferentialMapAbstractState} represents a map to {@link LatticeAbstractState}s with the semilattice operators lifted to the map.
 *
 * @author Dmitry Ivanov
 */
public class DifferentialMapAbstractState<KeyT, AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends DifferentialMap<KeyT, AbstractSpaceT>
    implements MapAbstractState<KeyT, AbstractSpaceT>
{

    public DifferentialMapAbstractState()
    {
    }

    public DifferentialMapAbstractState(Map<KeyT, AbstractSpaceT> m) {
        super(m);
    }

    public DifferentialMapAbstractState(Predicate<DifferentialMap<KeyT, AbstractSpaceT>> shouldCollapse) {
        super(Collections.emptyMap(), shouldCollapse);
    }

    // implementations for AbstractState

    @Override
    public DifferentialMapAbstractState<KeyT, AbstractSpaceT> copy()
    {
        return new DifferentialMapAbstractState<>(this);
    }

}
