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

import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;

/**
 * A {@link JvmCfaNode} representing the beginning of a catch or finally block.
 *
 * <p>The finally blocks are identified by catch type 0.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmCatchCfaNode
    extends JvmCfaNode
{

    private final int catchType;

    /**
     * Create a JVM CFA catch node without edges. Since in most cases we expect to have just one element in the lists of leaving and entering edges the lists are initialized with size 1.
     *
     * @param signature the signature of the method the node belongs to
     * @param offset    a number indicating the program location offset of the node
     * @param catchType an integer representing the exception type caught by  the handler in the node's method exception table
     * @param clazz     the class of the method the node belongs to
     */
    public JvmCatchCfaNode(MethodSignature signature, int offset, int catchType, Clazz clazz)
    {
        super(signature, offset, clazz);
        this.catchType = catchType;
    }

    /**
     * Create JVM CFA catch node with the specified entering and exiting edges.
     *
     * @param leavingEdges  a list of edges leaving the node
     * @param enteringEdges a list of edges entering the node
     * @param signature     the signature of the method the node belongs to
     * @param offset        a number indicating the program location offset of the node
     * @param catchType     an integer representing the exception type caught by  the handler in the node's method exception table
     * @param clazz         the class of the method the node belongs to
     */
    public JvmCatchCfaNode(List<JvmCfaEdge> leavingEdges, List<JvmCfaEdge> enteringEdges, MethodSignature signature, int offset, int catchType, Clazz clazz)
    {
        super(leavingEdges, enteringEdges, signature, offset, clazz);
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
     * Returns true if the catch node represents the beginning of a finally block.
     */
    public boolean isFinallyNode()
    {
        return catchType == 0;
    }
}
