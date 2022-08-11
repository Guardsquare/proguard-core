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

public interface MapAbstractState<KeyT, AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends Map<KeyT, AbstractSpaceT>, LatticeAbstractState<MapAbstractState<KeyT, AbstractSpaceT>>
{

    // implementations for LatticeAbstractState

    @Override
    default MapAbstractState<KeyT, AbstractSpaceT> join(MapAbstractState<KeyT, AbstractSpaceT> abstractState)
    {
        if (this == abstractState)
        {
            return this;
        }
        MapAbstractState<KeyT, AbstractSpaceT> joinResult = abstractState.copy();
        // compute the join for the domain intersection
        forEach((keyL, valueL) -> joinResult.compute(keyL, (keyR, valueR) -> valueR == null ? valueL : valueL.join(valueR)));
        if (equals(joinResult))
        {
            return this;
        }
        if (abstractState.equals(joinResult))
        {
            return abstractState;
        }
        return joinResult;
    }

    @Override
    default boolean isLessOrEqual(MapAbstractState<KeyT, AbstractSpaceT> abstractState)
    {
        if (!abstractState.keySet().containsAll(keySet()))
        {
            return false;
        }
        return entrySet().stream()
                         .allMatch(entry -> entry.getValue()
                                                 .isLessOrEqual(abstractState.get(entry.getKey())));
    }

    // overloads for AbstractState

    @Override
    MapAbstractState<KeyT, AbstractSpaceT> copy();
}
