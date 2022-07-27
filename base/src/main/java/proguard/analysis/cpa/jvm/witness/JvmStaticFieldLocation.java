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

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * The {@link JvmStaticFieldLocation} is a memory location corresponding to a public static field.
 *
 * @author Dmitry Ivanov
 */
public class JvmStaticFieldLocation
    extends JvmMemoryLocation
{

    public final String fqn;

    /**
     * Create a static field location.
     *
     * @param fqn a fully qualified name
     */
    public JvmStaticFieldLocation(String fqn)
    {
        this.fqn = fqn;
    }

    // implementations for MemoryLocation

    @Override
    public <T extends LatticeAbstractState> T extractValueOrDefault(JvmAbstractState abstractState, T defaultValue)
    {
        return (T) abstractState.getStaticOrDefault(fqn, defaultValue);
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmStaticFieldLocation))
        {
            return false;
        }
        JvmStaticFieldLocation other = (JvmStaticFieldLocation) obj;
        return fqn.equals(other.fqn);
    }

    @Override
    public int hashCode()
    {
        return fqn.hashCode();
    }

    @Override
    public String toString()
    {
        return "JvmStaticFieldLocation(" + fqn + ")";
    }
}
