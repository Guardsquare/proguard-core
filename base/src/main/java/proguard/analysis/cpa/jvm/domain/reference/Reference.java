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

package proguard.analysis.cpa.jvm.domain.reference;

import java.util.Objects;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;

/**
 * A reference points at an object or an array at the heap. It is identified by the program point when it was created and the memory location where it appears first.
 *
 * @author Dmitry Ivanov
 */
public class Reference
{

    /**
     * The program point at which the reference was created.
     */
    public final JvmCfaNode        creationTime;
    /**
     * The memory location where the reference was encountered for the first time.
     */
    public final JvmMemoryLocation creationSite;

    /**
     * Create a reference.
     *
     * @param creationTime the program point at which the reference was created
     * @param creationSite the memory location where the reference was encountered for the first time
     */
    public Reference(JvmCfaNode creationTime, JvmMemoryLocation creationSite)
    {
        this.creationTime = creationTime;
        this.creationSite = creationSite;
    }

    // implementations for Object

    @Override
    public int hashCode()
    {
        return Objects.hash(creationTime, creationSite);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof Reference))
        {
            return false;
        }
        Reference other = (Reference) obj;
        return creationTime.equals(other.creationTime) && creationSite.equals(other.creationSite);
    }

    @Override
    public String toString()
    {
        return "Reference(" + creationSite + "@" + (creationTime instanceof JvmUnknownCfaNode
                                                    ? "unknown"
                                                    : creationTime.getSignature() + ":" + creationTime.getOffset() + ")");
    }
}
