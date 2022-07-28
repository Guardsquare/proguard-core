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

package proguard.analysis.cpa.jvm.witness;

import java.util.Objects;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * The {@link JvmLocalVariableLocation} is a memory location at the local variable array.
 *
 * @author Dmitry Ivanov
 */
public class JvmLocalVariableLocation
    extends JvmMemoryLocation
{

    public final int index;

    /**
     * Create a local variable location.
     *
     * @param index a position at the local variable array
     */
    public JvmLocalVariableLocation(int index)
    {
        this.index = index;
    }

    // implementations for MemoryLocation

    @Override
    public <T extends LatticeAbstractState> T extractValueOrDefault(JvmAbstractState abstractState, T defaultValue)
    {
        return (T) abstractState.getVariableOrDefault(index, defaultValue);
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmLocalVariableLocation))
        {
            return false;
        }
        JvmLocalVariableLocation other = (JvmLocalVariableLocation) obj;
        return index == other.index;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(index);
    }

    @Override
    public String toString()
    {
        return "JvmLocalVariableLocation(" + index + ")";
    }
}
