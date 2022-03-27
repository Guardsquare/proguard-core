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

package proguard.analysis.cpa.interfaces;

/**
 * An {@link AbstractState} contains information about the program state.
 *
 * @author Dmitry Ivanov
 */
public interface AbstractState
{

    /**
     * Returns the {@link Precision} used by the {@link PrecisionAdjustment}.
     */
    default Precision getPrecision()
    {
        return null;
    }

    /**
     * Creates a copy of itself.
     */
    AbstractState copy();

    /**
     * Returns an abstract state for a given {@param name} if the state is composite, returns {@code self} otherwise.
     */
    default AbstractState getStateByName(String name)
    {
        return this;
    }

    // overrides for Object

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
