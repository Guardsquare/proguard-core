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

package proguard.analysis.cpa.jvm.cfa;

import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCatchCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A JVM specific implementation of {@link Cfa}.
 *
 * <p>The keys of the function maps are the {@link MethodSignature}s.
 *
 * <p>The nodes of a function are identified by the offset of the code location. Besides the normal exit node for return instructions, function can also have a special exit
 * node for uncaught exceptions.
 *
 * <p>An additional {@link JvmCatchCfaNode} is added for each handler in the method exception table.
 *
 * <p>A unique {@link JvmUnknownCfaNode} node is used for instructions the successor of which is unknown.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmCfa
    extends Cfa<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    private final Map<MethodSignature, Map<Integer, JvmCatchCfaNode>> functionCatchNodes = new HashMap<>();

    @Override
    public Stream<JvmCfaNode> getAllNodes()
    {
        return Stream.of(functionNodes.values(), functionCatchNodes.values())
                .flatMap(Collection::stream)
                .flatMap(it -> it.values().stream());
    }

    /**
     * Returns all the catch nodes of a specific method, returns an empty collection if the function is not in the graph or if it has no catch nodes.
     *
     * @param signature The signature of the method.
     */
    public Collection<JvmCatchCfaNode> getFunctionCatchNodes(MethodSignature signature)
    {
        return functionCatchNodes.getOrDefault(signature, Collections.emptyMap()).values();
    }

    /**
     * Returns the catch node of a method the handler of which begins at the specific code offset, returns null if the method or the specific catch node are not in the graph.
     *
     * @param signature The signature of the method, might be different for different domains.
     * @param offset    The offset of the catch handler represented by the node.
     */
    public JvmCatchCfaNode getFunctionCatchNode(MethodSignature signature, int offset)
    {
        return functionCatchNodes.getOrDefault(signature, Collections.emptyMap()).getOrDefault(offset, null);
    }

    /**
     * Adds a catch node to the CFA (i.e. a node indicating the beginning of an exception handler).
     */
    public void addFunctionCatchNode(MethodSignature signature, JvmCatchCfaNode node, int offset)
    {
        functionCatchNodes.computeIfAbsent(signature, x -> new HashMap<>()).put(offset, node);
    }

    /**
     * Returns true if the catch node of the specified method at the specified offset is present in the graph.
     */
    public boolean containsFunctionCatchNode(MethodSignature signature, int offset)
    {
        return functionCatchNodes.containsKey(signature) && functionCatchNodes.get(signature).containsKey(offset);
    }

    /**
     * Returns the exit node of the specified method if present, otherwise creates the exit node for the method and returns it.
     */
    public JvmCfaNode getFunctionReturnExitNode(MethodSignature signature, Clazz clazz)
    {
        // get the return exit location node
        JvmCfaNode exitNode = getFunctionNode(signature, CfaNode.RETURN_EXIT_NODE_OFFSET);

        if (exitNode == null)
        {
            exitNode = new JvmCfaNode(signature, CfaNode.RETURN_EXIT_NODE_OFFSET, clazz);
            addFunctionNode(signature, exitNode, CfaNode.RETURN_EXIT_NODE_OFFSET);
        }

        return exitNode;
    }

    /**
     * Returns the exception exit node (i.e. the exit node in case of not caught exception) of the specified method if present, otherwise creates the exit node for the method and returns it.
     */
    public JvmCfaNode getFunctionExceptionExitNode(MethodSignature signature, Clazz clazz)
    {
        // get the exit location node
        JvmCfaNode exitNode = getFunctionNode(signature, CfaNode.EXCEPTION_EXIT_NODE_OFFSET);

        if (exitNode == null)
        {
            exitNode = new JvmCfaNode(signature, CfaNode.EXCEPTION_EXIT_NODE_OFFSET, clazz);
            addFunctionNode(signature, exitNode, CfaNode.EXCEPTION_EXIT_NODE_OFFSET);
        }

        return exitNode;
    }

    /**
     * If the requested function node is present in the graph return it. If the node is not present add it to the graph and return the new node.
     */
    public JvmCfaNode addNodeIfAbsent(MethodSignature signature, int offset, Clazz clazz)
    {
        // if the location at the current offset is already in the CFA (e.g. created by a branch or goto) retrieve it, otherwise create a new node
        JvmCfaNode node = getFunctionNode(signature, offset);

        if (node == null)
        {
            node = new JvmCfaNode(signature, offset, clazz);

            // add the new node to the CFA
            if (offset == 0)
            {
                // first node of the functions' graph
                addFunctionEntryNode(signature, node);
            }
            else
            {
                addFunctionNode(signature, node, offset);
            }
        }

        return node;
    }

    /**
     * Adds a call node between two methods.
     */
    public void addInterproceduralEdge(Call call)
    {
        JvmCfaNode callNode   = addNodeIfAbsent(((MethodSignature) call.caller.signature), call.caller.offset, call.caller.clazz);
        JvmCfaNode calledNode = addNodeIfAbsent(call.getTarget(), 0, call instanceof ConcreteCall ? ((ConcreteCall) call).getTargetClass() : null);

        new JvmCallCfaEdge(callNode, calledNode, call);
    }

    /**
     * Adds a call node between two methods. This is used when the target method has no {@link proguard.classfile.attribute.CodeAttribute}, which means the target method is not present in the CFA. The
     * unknown node is set as the call target.
     */
    public void addUnknownTargetInterproceduralEdge(Call call)
    {
        JvmCfaNode callNode   = addNodeIfAbsent(((MethodSignature) call.caller.signature), call.caller.offset, call.caller.clazz);
        JvmCfaNode calledNode = JvmUnknownCfaNode.INSTANCE;

        new JvmCallCfaEdge(callNode, calledNode, call);
    }

    /**
     * Removes references to this CFA nodes from the singleton {@link JvmUnknownCfaNode} and clears its node collections making it garbage collectable.
     */
    public void clear()
    {
        List<JvmCfaEdge> unknownEnteringEdges = JvmUnknownCfaNode.INSTANCE.getEnteringEdges();
        if (!unknownEnteringEdges.isEmpty())
        {
            Set<JvmCfaEdge> edges = getAllNodes()
                .flatMap(it -> it.getLeavingEdges().stream())
                .filter(e -> e.getTarget() == JvmUnknownCfaNode.INSTANCE)
                .collect(Collectors.toSet());
            unknownEnteringEdges.removeAll(edges);
        }
        functionCatchNodes.clear();
        functionNodes.clear();
    }
}
