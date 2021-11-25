/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.analysis.datastructure.callgraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.ClassPool;
import proguard.classfile.MethodSignature;

/**
 * Represents a node in a sub-callgraph, e.g. only the incoming
 * or the outgoing callgraph for a specific method.
 * See {@link CallGraph#reconstructCallGraph(ClassPool, MethodSignature)}
 * for more details.
 *
 * @author Samuel Hopstock
 */
public class Node
{

    public final MethodSignature   signature;
    public final Set<Node>         predecessors          = new HashSet<>();
    /**
     * The {@link CodeLocation}s containing the calls in this node's predecessors
     * that lead here. If the call graph is traversed strictly in successor
     * direction, there is exactly one incoming call per node, except for the root,
     * which has none.
     */
    public final Set<CodeLocation> incomingCallLocations = new HashSet<>();
    /**
     * The {@link CodeLocation}s containing the calls in this node that lead to its
     * successors. If the call graph is traversed strictly in predecessor
     * direction, there is exactly one outgoing call per node, except
     * for the root, which has none.
     */
    public final Set<CodeLocation> outgoingCallLocations = new HashSet<>();
    public final Set<Node>         successors            = new HashSet<>();
    public       EntryPoint        matchingEntrypoint    = null;
    public       boolean           isTruncated           = false;

    public Node(MethodSignature signature)
    {
        this.signature = signature;
    }

    /**
     * Checks if this node or any successors corresponds to a specific {@link MethodSignature}.
     *
     * @param signature The {@link MethodSignature} to look for
     * @return true if this node or any of its transitive successors represents the target location
     */
    public boolean successorsContain(MethodSignature signature)
    {
        if (this.signature.equals(signature))
        {
            return true;
        }
        return successors.stream().anyMatch(s -> s.successorsContain(signature));
    }

    /**
     * Checks if this node or any predecessors corresponds to a specific {@link MethodSignature}.
     *
     * @param signature The {@link MethodSignature} to look for
     * @return true if this node or any of its transitive predecessors represents the target location
     */
    public boolean predecessorsContain(MethodSignature signature)
    {
        if (this.signature.equals(signature))
        {
            return true;
        }
        return successors.stream().anyMatch(s -> s.predecessorsContain(signature));
    }

    /**
     * Calculate the distance between this node and its furthest successor.
     *
     * @return The distance (number of hops in the graph)
     */
    public int getSuccessorDepth()
    {
        if (successors.isEmpty())
        {
            return 0;
        }
        return successors.stream().mapToInt(s -> s.getSuccessorDepth() + 1).max().getAsInt();
    }

    /**
     * Calculate the distance between this node and its furthest predecessor.
     *
     * @return The distance (number of hops in the graph)
     */
    public int getPredecessorDepth()
    {
        if (predecessors.isEmpty())
        {
            return 0;
        }
        return predecessors.stream().mapToInt(s -> s.getSuccessorDepth() + 1).max().getAsInt();
    }

    /**
     * Get all predecessors of this node.
     */
    public Set<Node> getAllPredecessors()
    {
        Set<Node> predecessors = new HashSet<>();
        List<Node> worklist = new ArrayList<>();
        worklist.add(this);
        while (!worklist.isEmpty())
        {
            Node curr = worklist.remove(0);
            predecessors.add(curr);
            worklist.addAll(curr.predecessors);
        }

        return predecessors;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Node that = (Node) o;
        return Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(signature);
    }
}
