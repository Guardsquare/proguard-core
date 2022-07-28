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

import proguard.classfile.attribute.CodeAttribute;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * An edge representing an assumption that a case of a switch statement is taken.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmAssumeCaseCfaEdge
    extends JvmInstructionCfaEdge
{

    private final int assumedCase;

    /**
     * Create a disconnected JVM CFA assume case edge.
     *
     * @param methodCode  the code attribute of the method the edge belongs to
     * @param offset      the offset of the instruction represented by the edge
     * @param assumedCase an assumed integer value of the switch variable
     */
    public JvmAssumeCaseCfaEdge(CodeAttribute methodCode, int offset, int assumedCase)
    {
        super(methodCode, offset);
        this.assumedCase = assumedCase;
    }

    /**
     * Create a JVM CFA assume case edge. Also sets it as the entering and leaving edge of the source and target nodes.
     *
     * @param source      the source node of the edge
     * @param target      the target node of the edge
     * @param methodCode  the code attribute of the method the edge belongs to
     * @param offset      the offset of the instruction represented by the edge
     * @param assumedCase an assumed integer value of the switch variable
     */
    public JvmAssumeCaseCfaEdge(JvmCfaNode source, JvmCfaNode target, CodeAttribute methodCode, int offset, int assumedCase)
    {
        super(source, target, methodCode, offset);
        this.assumedCase = assumedCase;
    }

    /**
     * Returns the assumed integer value of the switch variable made on this edge.
     */
    public int getAssumedCase()
    {
        return assumedCase;
    }
}
