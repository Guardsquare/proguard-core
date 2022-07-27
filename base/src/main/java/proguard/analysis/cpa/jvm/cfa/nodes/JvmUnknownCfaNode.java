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

package proguard.analysis.cpa.jvm.cfa.nodes;

/**
 * A unique node for an entire CFA representing an unknown code location.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmUnknownCfaNode
    extends JvmCfaNode
{

    public static final JvmUnknownCfaNode INSTANCE = new JvmUnknownCfaNode();

    /**
     * Create the unknown node.
     */
    private JvmUnknownCfaNode()
    {
        super(null, -1, null);
    }

    // Implementations for CfaNode

    @Override
    public boolean isReturnExitNode()
    {
        return false;
    }

    @Override
    public boolean isUnknownNode()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "JvmUnknownCfaNode{}";
    }
}
