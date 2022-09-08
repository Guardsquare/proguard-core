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

package proguard.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.Metrics;
import proguard.analysis.Metrics.MetricType;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.CallGraph;
import proguard.analysis.datastructure.callgraph.Node;
import proguard.classfile.MethodSignature;

/**
 * Generic utilities to traverse the call graph.
 *
 * @author Samuel Hopstock
 */
public class CallGraphWalker
{

    private static final Logger log               = LogManager.getLogger(CallGraphWalker.class);
    /**
     * Call graph strands are no longer explored after a maximum distance from the original root.
     */
    public static final  int    MAX_DEPTH_DEFAULT = 100;
    /**
     * Once the call graph reaches a maximum width, no more nodes are added to the worklist of the next level.
     * E.g. suppose this limit is 5 and we have already discovered the following call graph:
     * <pre>
     *     <code>
     *         level2_0 <-- level1_0 <-- root
     *         level2_1 <------|          |
     *         level2_2 <------|          |
     *                                    |
     *         level2_3 <-- level1_1 <----|
     *         level2_4 <------|
     *     </code>
     * </pre>
     * If <code>level1_1</code> has any more known predecessors, level 2 of the call graph would have width 6,
     * which is more than the 5 allowed nodes. Thus, <code>level1_1</code> is marked as truncated and its other
     * predecessors are discarded.
     */
    public static final  int    MAX_WIDTH_DEFAULT = 100;

    /**
     * Analogous to Soot's <code>getReachableMethods()</code>: Starting from one particular method,
     * all methods that are transitively reachable are collected in a single set. The exploration
     * stops after no more reachable methods have been found, or the reachable call graph exceeds
     * {@link #MAX_DEPTH_DEFAULT} and {@link #MAX_WIDTH_DEFAULT}.
     *
     * @param callGraph The {@link CallGraph} to use as the basis for this exploration
     * @param start     The method that is to be used as the exploration root
     * @param maxDepth  See {@link #MAX_DEPTH_DEFAULT}
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return A set of all transitively reachable methods
     */
    public static Set<MethodSignature> getSuccessors(CallGraph callGraph,
                                                     MethodSignature start,
                                                     int maxDepth,
                                                     int maxWidth)
    {
        Set<MethodSignature> visited = new HashSet<>();
        explore(callGraph,
                start,
                CallGraphWalker::calculateSuccessors,
                n -> visited.add(n.signature),
                maxDepth,
                maxWidth);
        return visited;
    }

    /**
     * Like {@link #getSuccessors(CallGraph, MethodSignature, int, int)} but using default
     * values for max depth and max width.
     *
     * @param callGraph The {@link CallGraph} to use as the basis for this exploration
     * @param start     The method that is to be used as the exploration root
     * @return A set of all transitively reachable methods
     */
    public static Set<MethodSignature> getSuccessors(CallGraph callGraph, MethodSignature start)
    {
        return getSuccessors(callGraph, start, MAX_DEPTH_DEFAULT, MAX_WIDTH_DEFAULT);
    }

    /**
     * Inverse of {@link #getSuccessors(CallGraph, MethodSignature)}: Starting from one particular method,
     * all methods that can transitively reach it are collected in a single set. The exploration
     * stops after no more incoming methods have been found, or the inversely-reachable call graph exceeds
     * {@link #MAX_DEPTH_DEFAULT} and {@link #MAX_WIDTH_DEFAULT}.
     *
     * @param callGraph The {@link CallGraph} to use as the basis for this exploration
     * @param start     The method that is to be used as the exploration root
     * @param maxDepth  See {@link #MAX_DEPTH_DEFAULT}
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return A set of all methods that can transitively reach the root
     */
    public static Set<MethodSignature> getPredecessors(CallGraph callGraph,
                                                       MethodSignature start,
                                                       int maxDepth,
                                                       int maxWidth)
    {
        Set<MethodSignature> visited = new HashSet<>();
        explore(callGraph,
                start,
                CallGraphWalker::calculatePredecessors,
                n -> visited.add(n.signature),
                maxDepth,
                maxWidth);
        return visited;
    }

    /**
     * Like {@link #getPredecessors(CallGraph, MethodSignature, int, int)} but using default
     * values for max depth and max width.
     *
     * @param callGraph The {@link CallGraph} to use as the basis for this exploration
     * @param start     The method that is to be used as the exploration root
     * @return A set of all methods that can transitively reach the root
     */
    public static Set<MethodSignature> getPredecessors(CallGraph callGraph, MethodSignature start)
    {
        return getPredecessors(callGraph, start, MAX_DEPTH_DEFAULT, MAX_WIDTH_DEFAULT);
    }

    /**
     * Interactively explore the <b>outgoing</b> call graph (breadth-first) of a specific method.
     *
     * <p>Outgoing call graph edges are transitively visited, one level of the call graph at a time. E.g.
     * if we have the following graph:
     * <pre>
     *     <code>
     *         level2_0 <-- level1_0 <-- root
     *         level2_1 <------|          |
     *         level2_2 <------|          |
     *                                    |
     *         level2_3 <-- level1_1 <----|
     *         level2_4 <------|
     *     </code>
     * </pre>
     * In this case, <code>level1_0</code> and <code>level1_1</code> are visited first, then <code>level2_*</code>
     * and so on. The user of this method provides a callback that will be executed for every newly visited path.
     * This handler receives a {@link Node} that represents e.g. <code>level2_1</code> and contains references to
     * all its predecessors that have been visited in this particular path. Like this, the user can evaluate the
     * whole call chain that led to any specific method being reachable from the starting point. Graph limits
     * {@link #MAX_DEPTH_DEFAULT} and {@link #MAX_WIDTH_DEFAULT} are applicable. Any paths that are truncated due to any limit
     * being reached, are marked with {@link Node#isTruncated}
     * </p>
     * <p>If you are only interested in which methods are reachable from a start method, but do not care
     * about the individual paths that make this possible, you should use {@link #getSuccessors(CallGraph, MethodSignature)} instead.</p>
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param start     The method that is to be used as the exploration root
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @param maxDepth  See {@link #MAX_DEPTH_DEFAULT}
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return The {@link Node} representing the start method and all its successors
     */
    public static Node successorPathsAccept(CallGraph callGraph,
                                            MethodSignature start,
                                            Predicate<Node> handler,
                                            int maxDepth,
                                            int maxWidth)
    {
        return explore(callGraph,
                       start,
                       CallGraphWalker::calculateSuccessors,
                       handler,
                       maxDepth,
                       maxWidth);
    }

    /**
     * Like {@link #successorPathsAccept(CallGraph, MethodSignature, Predicate, int, int)}
     * but using default values for max depth and max width.
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param start     The method that is to be used as the exploration root
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @return The {@link Node} representing the start method and all its successors
     */
    public static Node successorPathsAccept(CallGraph callGraph, MethodSignature start, Predicate<Node> handler)
    {
        return successorPathsAccept(callGraph, start, handler, MAX_DEPTH_DEFAULT, MAX_WIDTH_DEFAULT);
    }

    /**
     * Interactively explore the <b>incoming</b> call graph (breadth-first) of a specific method.
     *
     * <p>Inverse of {@link #successorPathsAccept(CallGraph, MethodSignature, Predicate)}: Explores all methods
     * that can reach the starting point and notifies the user's handler of newly found paths.</p>
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param start     The method that is to be used as the exploration root
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @param maxDepth  See {@link #MAX_DEPTH_DEFAULT}
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return The {@link Node} representing the start method and all its predecessors
     */
    public static Node predecessorPathsAccept(CallGraph callGraph,
                                              MethodSignature start,
                                              Predicate<Node> handler,
                                              int maxDepth,
                                              int maxWidth)
    {
        return explore(callGraph,
                       start,
                       CallGraphWalker::calculatePredecessors,
                       handler,
                       maxDepth,
                       maxWidth);
    }

    /**
     * Like {@link #predecessorPathsAccept(CallGraph, MethodSignature, Predicate, int, int)}
     * but using default values for max depth and max width.
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param start     The method that is to be used as the exploration root
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @return The {@link Node} representing the start method and all its predecessors
     */
    public static Node predecessorPathsAccept(CallGraph callGraph, MethodSignature start, Predicate<Node> handler)
    {
        return predecessorPathsAccept(callGraph, start, handler, MAX_DEPTH_DEFAULT, MAX_WIDTH_DEFAULT);
    }

    /**
     * Generic call graph exploration function. The reachable methods are visited in a breadth-first way.
     * The direction of this procedure (i.e. if the call graph is explored through outgoing or incoming
     * edges) is determined by a user-provided function.
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param start     The method that is to be used as the exploration root
     * @param getNext   After all methods of the current depth have been visited, this method
     *                  is used to determine the methods of the next level. E.g. if we want to
     *                  visit the call graph in the outgoing/successor direction, this function
     *                  should yield all direct successors for nodes in the current level.
     *                  See {@link #calculateSuccessors(CallGraph, Node)} or its opposite
     *                  {@link #calculatePredecessors(CallGraph, Node)} to see example implementations.
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @param maxDepth  See {@link #MAX_DEPTH_DEFAULT}
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return The {@link Node} representing the start method and all its successors/predecessors (depending
     *     on the concrete implementation of getNext
     */
    private static Node explore(CallGraph callGraph,
                                MethodSignature start,
                                BiFunction<CallGraph, Node, Collection<Node>> getNext,
                                Predicate<Node> handler,
                                int maxDepth,
                                int maxWidth)
    {
        Node            root     = new Node(start);
        ArrayList<Node> worklist = new ArrayList<>();

        worklist.add(root);
        int currLevel = 0;
        while (!worklist.isEmpty())
        {
            if (currLevel >= maxDepth)
            {
                Metrics.increaseCount(MetricType.CALL_GRAPH_RECONSTRUCTION_MAX_DEPTH_REACHED);
                worklist.forEach(n -> n.isTruncated = true);
                break;
            }
            worklist = currentLevelAccept(callGraph, getNext, handler, worklist, maxWidth);
            currLevel++;
        }

        return root;
    }

    /**
     * Visit all the nodes of the current depth in the call graph, forward them to the provided
     * handler function and make sure that the next level is prepared.
     *
     * @param callGraph The {@link CallGraph} to use as the basis of this exploration
     * @param getNext   After all methods of the current depth have been visited, this method
     *                  is used to determine the methods of the next level. E.g. if we want to
     *                  visit the call graph in the outgoing/successor direction, this function
     *                  should yield all direct successors for nodes in the current level.
     *                  See {@link #calculateSuccessors(CallGraph, Node)} or its opposite
     *                  {@link #calculatePredecessors(CallGraph, Node)} to see example implementations.
     * @param handler   The callback function that is invoked for newly visited paths. If this returns false,
     *                  this specific path is not explored any further, without marking it as truncated.
     * @param maxWidth  See {@link #MAX_WIDTH_DEFAULT}
     * @return The nodes of the next level, making sure that the depth and width limits of the call
     *     graph won't be exceeded
     */
    private static ArrayList<Node> currentLevelAccept(CallGraph callGraph,
                                                      BiFunction<CallGraph, Node, Collection<Node>> getNext,
                                                      Predicate<Node> handler,
                                                      ArrayList<Node> worklist,
                                                      int maxWidth)
    {
        ArrayList<Node> nextLevel = new ArrayList<>();
        for (Node curr : worklist)
        {
            if (!handler.test(curr))
            {
                // The handler wants us to stop exploring this path without marking it as truncated
                continue;
            }

            for (Node next : getNext.apply(callGraph, curr))
            {
                if (nextLevel.size() >= maxWidth)
                {
                    Metrics.increaseCount(MetricType.CALL_GRAPH_RECONSTRUCTION_MAX_WIDTH_REACHED);
                    next.isTruncated = true;
                }
                else
                {
                    nextLevel.add(next);
                }
            }
        }
        return nextLevel;
    }

    /**
     * Return all direct predecessors of curr in this callgraph.
     */
    private static Set<Node> calculatePredecessors(CallGraph callGraph, Node curr)
    {
        Set<Node> predecessors = new HashSet<>();
        for (Call i : callGraph.incoming.getOrDefault(curr.signature, Collections.emptySet()))
        {
            if (!(i.caller.signature instanceof MethodSignature))
            {
                log.error("Call graph edge {} does not have a method as the caller member!", i);
                continue;
            }
            // Only add the caller to the chain if this doesn't create a loop
            if (!curr.successorsContain((MethodSignature) i.caller.signature))
            {
                Node prev = new Node((MethodSignature) i.caller.signature);
                curr.predecessors.add(prev);
                curr.incomingCallLocations.add(i.caller);
                prev.successors.add(curr);
                prev.outgoingCallLocations.add(i.caller);
                predecessors.add(prev);
            }
        }
        return predecessors;
    }

    /**
     * Return all direct successors of curr in this callgraph.
     */
    private static Set<Node> calculateSuccessors(CallGraph callGraph, Node curr)
    {
        Set<Node> successors = new HashSet<>();
        for (Call i : callGraph.outgoing.getOrDefault(curr.signature, Collections.emptySet()))
        {
            // Only add the caller to the chain if this doesn't create a loop
            if (!curr.predecessorsContain(i.getTarget()))
            {
                Node successor = new Node(i.getTarget());
                curr.successors.add(successor);
                curr.outgoingCallLocations.add(i.caller);
                successor.predecessors.add(curr);
                successor.incomingCallLocations.add(i.caller);
                successors.add(successor);
            }
        }
        return successors;
    }
}
