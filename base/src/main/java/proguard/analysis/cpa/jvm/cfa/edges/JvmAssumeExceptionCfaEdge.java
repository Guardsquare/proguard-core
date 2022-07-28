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

package proguard.analysis.cpa.jvm.cfa.edges;

import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * An edge representing an assumption on a JVM exception of a specific type that can be either caught or not caught.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmAssumeExceptionCfaEdge
    extends JvmCfaEdge
{

    private final boolean isCaught;
    // the catch type constant of the corresponding exception info
    private final int     catchType;

    /**
     * Create a disconnected JVM CFA exception assumption edge.
     *
     * @param isCaught  an assumption on the exception being caught or not
     * @param catchType an integer representing the exception type in the edge's method exception table
     */
    public JvmAssumeExceptionCfaEdge(boolean isCaught, int catchType)
    {
        this.isCaught = isCaught;
        this.catchType = catchType;
    }

    /**
     * Create a JVM CFA exception assumption edge. Also sets it as the entering and leaving edge of the source and target nodes.
     *
     * @param source    the source node of the edge
     * @param target    the target node of the edge
     * @param isCaught  an assumption on the exception being caught or not
     * @param catchType an integer representing the exception type in the edge's method exception table
     */
    public JvmAssumeExceptionCfaEdge(JvmCfaNode source, JvmCfaNode target, boolean isCaught, int catchType)
    {
        super(source, target);
        this.isCaught = isCaught;
        this.catchType = catchType;
    }

    /**
     * Returns the integer representing the exception type in the exception table of the method the node belongs to.
     */
    public int getCatchType()
    {
        return catchType;
    }

    /**
     * Returns the assumption on the exception being caught made for the edge.
     */
    public boolean isCaught()
    {
        return isCaught;
    }
}
