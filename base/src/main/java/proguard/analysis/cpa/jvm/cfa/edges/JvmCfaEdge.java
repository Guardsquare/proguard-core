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

import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.classfile.MethodSignature;

/**
 * Default implementation of {@link CfaEdge} for JVM instructions.
 *
 * @author Carlo Alberto Pozzoli
 */
public abstract class JvmCfaEdge
    implements CfaEdge<JvmCfaNode>
{

    private JvmCfaNode source;
    private JvmCfaNode target;

    /**
     * Create a disconnected JVM CFA edge.
     */
    public JvmCfaEdge()
    {
    }

    /**
     * Create a JVM CFA edge. Also sets it as the entering and leaving edge of the source and target nodes.
     *
     * @param source the source node of the edge
     * @param target the target node of the edge
     */
    public JvmCfaEdge(JvmCfaNode source, JvmCfaNode target)
    {
        setSource(source);
        setTarget(target);
    }

    // Implementations for CfaEdge

    @Override
    public JvmCfaNode getSource()
    {
        return source;
    }

    @Override
    public JvmCfaNode getTarget()
    {
        return target;
    }

    /**
     * Sets a node as the predecessor of the edge and adds the edge as leaving edge of the node.
     */
    public void setSource(JvmCfaNode source)
    {
        this.source = source;
        source.addLeavingEdge(this);
    }

    /**
     * Sets a node as the successor of the edge and adds the edge as entering edge of the node.
     */
    public void setTarget(JvmCfaNode target)
    {
        this.target = target;
        target.addEnteringEdge(this);
    }

    /**
     * Returns the signature of the target method. This is the current method or, for call edges,
     * the target method of the call.
     */
    public MethodSignature targetSignature()
    {
        return target.getSignature();
    }
}
