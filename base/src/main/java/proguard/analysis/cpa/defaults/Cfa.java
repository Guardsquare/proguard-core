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

package proguard.analysis.cpa.defaults;

import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.Signature;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Cfa} is a control flow automaton with nodes {@code <CfaNodeT>} and edges {@code <CfaEdgeT>}. It can be used for different programming languages with functions identified by {@code
 * <SignatureT>}.
 *
 * @author Carlo Alberto Pozzoli
 */
public abstract class Cfa<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
{

    protected final Map<SignatureT, Map<Integer, CfaNodeT>> functionNodes = new HashMap<>();

    /**
     * Returns true if there are no nodes in the CFA, false otherwise.
     */
    public boolean isEmpty()
    {
        return functionNodes.isEmpty();
    }

    /**
     * Returns a stream of all the nodes present in the graph.
     *
     * Note: a {@link Stream} is provided to avoid creating new collections
     *       unnecessarily.
     */
    public Stream<CfaNodeT> getAllNodes()
    {
        return functionNodes
                .values()
                .stream()
                .flatMap(it -> it.values().stream());
    }

    /**
     * Returns a collection of the entry nodes (with offset 0) of all the functions present in the graph, returns an empty collection if the graph is empty.
     */
    public Collection<CfaNodeT> getFunctionEntryNodes()
    {
        return functionNodes.keySet()
                            .stream()
                            .map(x -> functionNodes.get(x).getOrDefault(0, null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
    }

    /**
     * Returns the entry node of a specific function (with offset 0), returns null if the function or its entry node are not in the graph.
     *
     * @param signature The signature of the function.
     */
    public CfaNodeT getFunctionEntryNode(SignatureT signature)
    {
        return functionNodes.getOrDefault(signature, Collections.emptyMap()).getOrDefault(0, null);
    }

    /**
     * Returns all the nodes of a specific function, returns an empty collection if the function is not in the graph or if it has no nodes.
     *
     * @param signature The signature of the function.
     */
    public Collection<CfaNodeT> getFunctionNodes(SignatureT signature)
    {
        return functionNodes.getOrDefault(signature, Collections.emptyMap()).values();
    }

    /**
     * Returns the node of a function at a specific code offset, returns null if the function or the specific node are not in the graph.
     *
     * @param signature The signature of the function.
     * @param offset    The offset of the code location represented by the node.
     */
    public CfaNodeT getFunctionNode(SignatureT signature, int offset)
    {
        return functionNodes.getOrDefault(signature, Collections.emptyMap()).getOrDefault(offset, null);
    }

    /**
     * Returns the node of a function at a specific code offset, returns null if the function or the specific node are not in the graph.
     *
     * @param clazz  The {@link Clazz} in which the function is declared.
     * @param method The {@link Method} in which the function is declared.
     * @param offset The offset of the code location represented by the node.
     */
    public CfaNodeT getFunctionNode(Clazz clazz, Method method, int offset)
    {
        return functionNodes.getOrDefault((SignatureT) Signature.of(clazz, method), Collections.emptyMap()).getOrDefault(offset, null);
    }

    /**
     * Add an entry node to the graph for a specified function (with offset 0).
     *
     * @param signature The signature of the function,.
     * @param node      The entry node to add.
     */
    public void addFunctionEntryNode(SignatureT signature, CfaNodeT node)
    {
        addFunctionNode(signature, node, 0);
    }

    /**
     * Add a node to the graph for a specified function.
     *
     * @param signature The signature of the function.
     * @param node      The node to add.
     */
    public void addFunctionNode(SignatureT signature, CfaNodeT node, int offset)
    {
        functionNodes.computeIfAbsent(signature, x -> new HashMap<>()).put(offset, node);
    }
}
