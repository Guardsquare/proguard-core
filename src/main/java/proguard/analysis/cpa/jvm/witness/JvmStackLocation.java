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
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.domain.arg.ArgProgramLocationDependentAbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * The {@link JvmStackLocation} is a memory location at the operand stack at a certain program point.
 * Indexing starts from the top of the stack.
 *
 * @author Dmitry Ivanov
 */
public class JvmStackLocation
    extends JvmMemoryLocation
{

    public final int index;

    /**
     * Create a stack location unspecific to a program location.
     *
     * @param index a stack element index from the top
     */
    public JvmStackLocation(int index)
    {
        this(null, index);
    }

    /**
     * Create a stack location at a specific program location.
     *
     * @param argNode an ARG node
     * @param index   a stack element index from the top
     */
    public JvmStackLocation(ArgProgramLocationDependentAbstractState<JvmCfaNode, JvmCfaEdge, MethodSignature> argNode, int index)
    {
        this.argNode = argNode;
        this.index = index;
    }

    // implementations for JvmMemoryLocation

    @Override
    public JvmStackLocation copy()
    {
        return new JvmStackLocation(argNode, index);
    }

    // implementations for MemoryLocation

    @Override
    public <T extends LatticeAbstractState> T extractValueOrDefault(JvmAbstractState abstractState, T defaultValue)
    {
        return (T) abstractState.peekOrDefault(index, defaultValue);
    }

    /**
     * Returns the stack index from the top.
     */
    public int getIndex()
    {
        return index;
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmStackLocation))
        {
            return false;
        }
        JvmStackLocation other = (JvmStackLocation) obj;
        return super.equals(obj) && index == other.index;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(argNode, index);
    }

    @Override
    public String toString()
    {
        return "JvmStackLocation(" + index + ")" + (argNode == null ? "" : "@" + argNode.getProgramLocation().getSignature().getFqn() + ":" + argNode.getProgramLocation().getOffset());
    }
}
