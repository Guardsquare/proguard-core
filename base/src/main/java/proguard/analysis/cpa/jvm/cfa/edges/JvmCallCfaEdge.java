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

import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.cpa.interfaces.CallEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.classfile.MethodSignature;

/**
 * A {@link JvmCfaEdge} representing a call to another method, linking to the first node of the called method.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmCallCfaEdge
    extends JvmCfaEdge
    implements CallEdge
{

    private Call call;

    /**
     * Create a disconnected JVM CFA call edge.
     *
     * @param call the call to a method
     */
    public JvmCallCfaEdge(Call call)
    {
        this.call = call;
    }

    /**
     * Create a JVM CFA call edge. Also sets it as the entering and leaving edge of the source and target nodes.
     *
     * @param source the source node of the edge
     * @param target the target node of the edge
     * @param call   the call to a method
     */
    public JvmCallCfaEdge(JvmCfaNode source, JvmCfaNode target, Call call)
    {
        super(source, target);
        this.call = call;
    }

    @Override
    public MethodSignature targetSignature()
    {
        return call.getTarget();
    }

    // Implementations for CallEdge

    @Override
    public Call getCall()
    {
        return call;
    }
}
