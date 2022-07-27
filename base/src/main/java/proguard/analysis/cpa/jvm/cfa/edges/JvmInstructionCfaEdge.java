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
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * A {@link JvmCfaEdge} the operation of which is defined by an instruction.
 *
 * <p>An instruction is identified by a code attribute and an offset and generated on the fly.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmInstructionCfaEdge
    extends JvmCfaEdge
{

    private final CodeAttribute methodCode;
    private final int           offset;

    /**
     * Create a disconnected JVM CFA instruction edge.
     *
     * @param methodCode the code attribute of the method the edge belongs to
     * @param offset     the offset of the instruction represented by the edge
     */
    public JvmInstructionCfaEdge(CodeAttribute methodCode, int offset)
    {
        this.methodCode = methodCode;
        this.offset = offset;
    }

    /**
     * Create a JVM CFA instruction edge. Also sets it as the entering and leaving edge of the source and target nodes.
     *
     * @param source     the source node of the edge
     * @param target     the target node of the edge
     * @param methodCode the code attribute of the method the edge belongs to
     * @param offset     the offset of the instruction represented by the edge
     */
    public JvmInstructionCfaEdge(JvmCfaNode source, JvmCfaNode target, CodeAttribute methodCode, int offset)
    {
        super(source, target);
        this.methodCode = methodCode;
        this.offset = offset;
    }

    /**
     * Generates and returns the JVM instruction represented by the edge.
     */
    public Instruction getInstruction()
    {
        return InstructionFactory.create(methodCode.code, offset);
    }

    /**
     * Returns the code attribute of the method the node belongs to.
     */
    public CodeAttribute getMethodCode()
    {
        return methodCode;
    }
}
