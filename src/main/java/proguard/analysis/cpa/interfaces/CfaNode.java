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

package proguard.analysis.cpa.interfaces;

import java.util.List;
import proguard.classfile.Signature;

/**
 * A node for {@link proguard.analysis.cpa.defaults.Cfa} parametrized by its edges {@code CfaEdgeT}. A CFA node corresponds to a program point before the instruction at a specific offset.
 *
 * @author Dmitry Ivanov
 */
public interface CfaNode<CfaEdgeT extends CfaEdge, SignatureT extends Signature>
{

    int RETURN_EXIT_NODE_OFFSET    = -1;
    int EXCEPTION_EXIT_NODE_OFFSET = -2;

    /**
     * Returns a list of leaving edges.
     */
    List<CfaEdgeT> getLeavingEdges();

    /**
     * Returns a list of entering edges.
     */
    List<CfaEdgeT> getEnteringEdges();

    /**
     * Checks whether the node is a function entry.
     */
    boolean isEntryNode();

    /**
     * Checks whether the node is a function exit.
     */
    boolean isExitNode();

    /**
     * Returns the function signature it belongs to.
     */
    SignatureT getSignature();

    /**
     * Returns the instruction offset.
     */
    int getOffset();

    /**
     * Returns true if the node is the return location of the function (offset == {@link CfaNode#RETURN_EXIT_NODE_OFFSET}).
     */
    default boolean isReturnExitNode()
    {
        return getOffset() == RETURN_EXIT_NODE_OFFSET;
    }

    /**
     * Returns true if the node is the return location of the function (offset == {@link CfaNode#EXCEPTION_EXIT_NODE_OFFSET}).
     */
    default boolean isExceptionExitNode()
    {
        return getOffset() == EXCEPTION_EXIT_NODE_OFFSET;
    }

    /**
     * Returns true if the location of the node is unknown.
     */
    default boolean isUnknownNode()
    {
        return false;
    }
}
