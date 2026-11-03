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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.Metrics;
import proguard.analysis.Metrics.MetricType;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.CallGraph;
import proguard.analysis.datastructure.callgraph.Node;
import proguard.classfile.MethodSignature;

/** Generic utilities to traverse the call graph. */
public class CallGraphWalker {

  private static final Logger log = LogManager.getLogger(CallGraphWalker.class);

  /**
   * Starting from one particular method, all methods that are transitively reachable are collected
   * in a single set. The exploration stops after no more reachable methods have been found, or the
   * reachable call graph exceeds maxDepth.
   *
   * @param callGraph The {@link CallGraph} to use as the basis for this exploration.
   * @param start The method that is to be used as the exploration root.
   * @param maxDepth Only explore to this depth. -1 explores the full depth.
   * @return A set of all transitively reachable methods.
   */
  public static Set<MethodSignature> getSuccessors(
      CallGraph callGraph, MethodSignature start, int maxDepth) {
    Set<MethodSignature> visited = new LinkedHashSet<>();
    explore(
        callGraph,
        start,
        CallGraphWalker::calculateSuccessors,
        n -> visited.add(n.signature),
        maxDepth);
    return visited;
  }

  /**
   * Inverse of {@link #getSuccessors(CallGraph, MethodSignature, int)}: Starting from one
   * particular method, all methods that can transitively reach it are collected in a single set.
   * The exploration stops after no more incoming methods have been found, or the
   * inversely-reachable call graph exceeds maxDepth.
   *
   * @param callGraph The {@link CallGraph} to use as the basis for this exploration.
   * @param start The method that is to be used as the exploration root.
   * @param maxDepth Only explore to this depth. -1 explores the full depth.
   * @return A set of all methods that can transitively reach the root.
   */
  public static Set<MethodSignature> getPredecessors(
      CallGraph callGraph, MethodSignature start, int maxDepth) {
    Set<MethodSignature> visited = new LinkedHashSet<>();
    explore(
        callGraph,
        start,
        CallGraphWalker::calculatePredecessors,
        n -> visited.add(n.signature),
        maxDepth);
    return visited;
  }

  /**
   * Interactively explore the <b>outgoing</b> call graph (breadth-first) of a specific method.
   *
   * <p>If you are only interested in which methods are reachable from a start method, but do not
   * care about the individual paths that make this possible, you should use {@link
   * #getSuccessors(CallGraph, MethodSignature, int)} instead.
   *
   * @param callGraph The {@link CallGraph} to use as the basis of this exploration.
   * @param start The method that is to be used as the exploration root.
   * @param handler The callback function that is invoked for newly visited paths. If this returns
   *     false, this specific path is not explored any further, without marking it as {@link
   *     Node#isTruncated}.
   * @param maxDepth Only explore to this depth. -1 explores the full depth.
   * @return The {@link Node} representing the start method and all its successors.
   */
  public static Node successorPathsAccept(
      CallGraph callGraph, MethodSignature start, Predicate<Node> handler, int maxDepth) {
    return explore(callGraph, start, CallGraphWalker::calculateSuccessors, handler, maxDepth);
  }

  /**
   * Interactively explore the <b>incoming</b> call graph (breadth-first) of a specific method.
   *
   * <p>If you are only interested in which methods are reaching a specific method, but do not care
   * about the individual paths that make this possible, you should use {@link
   * #getPredecessors(CallGraph, MethodSignature, int)} instead.
   *
   * @param callGraph The {@link CallGraph} to use as the basis of this exploration.
   * @param start The method that is to be used as the exploration root.
   * @param handler The callback function that is invoked for newly visited paths. If this returns
   *     false, this specific path is not explored any further, without marking it as {@link
   *     Node#isTruncated}.
   * @param maxDepth Only explore to this depth. -1 explores the full depth.
   * @return The {@link Node} representing the start method and all its predecessors.
   */
  public static Node predecessorPathsAccept(
      CallGraph callGraph, MethodSignature start, Predicate<Node> handler, int maxDepth) {
    return explore(callGraph, start, CallGraphWalker::calculatePredecessors, handler, maxDepth);
  }

  /**
   * Generic call graph exploration that performs <b>path enumeration until maxDepth is reached</b>:
   * a method that is reachable via N distinct paths is reported N times (once per path), so the
   * user's handler is invoked for every distinct path.
   *
   * @param callGraph The {@link CallGraph} to use as the basis of this exploration.
   * @param start The method that is to be used as the exploration root.
   * @param getNext After a node has been visited, this yields its direct successors (or
   *     predecessors, depending on the traversal direction), skipping any whose signature is
   *     already visited on the current path. See {@link #calculateSuccessors} and {@link
   *     #calculatePredecessors}.
   * @param handler The callback function that is invoked for newly visited paths. If this returns
   *     false, this specific path is not explored any further, without marking it as {@link
   *     Node#isTruncated}.
   * @param maxDepth Only explore to this depth. -1 explores the full depth.
   * @return The {@link Node} representing the start method and all its successors/predecessors
   *     (depending on the concrete implementation of getNext).
   */
  private static Node explore(
      CallGraph callGraph,
      MethodSignature start,
      NodeExpander getNext,
      Predicate<Node> handler,
      int maxDepth) {
    Node root = new Node(start);
    ArrayDeque<VisitedNodes> worklist = new ArrayDeque<>();
    worklist.add(new VisitedNodes(root, Collections.emptySet()));

    int currDepth = 0;
    while (!worklist.isEmpty()) {
      if (maxDepth != -1 && currDepth >= maxDepth) {
        for (VisitedNodes item : worklist) {
          item.current.isTruncated = true;
        }
        Metrics.increaseCount(MetricType.CALL_GRAPH_RECONSTRUCTION_MAX_DEPTH_REACHED);
        break;
      }
      Metrics.setIfMax(MetricType.CALLGRAPHWALKER_MAX_DEPTH, currDepth);

      int levelSize = worklist.size();
      Metrics.setIfMax(MetricType.CALLGRAPHWALKER_MAX_WIDTH, levelSize);
      for (int i = 0; i < levelSize; i++) {
        VisitedNodes node = worklist.poll();
        if (!handler.test(node.current)) {
          // The handler wants us to stop exploring this path without marking it as truncated
          continue;
        }
        Set<MethodSignature> visitedNodes = node.getVisitedNodes();
        Set<MethodSignature> visitedIncludingCurrent = new HashSet<>(visitedNodes);
        visitedIncludingCurrent.add(node.current.signature);
        Set<MethodSignature> unmodVisitedIncludingCurrent =
            Collections.unmodifiableSet(visitedIncludingCurrent);

        for (Node next : getNext.expand(callGraph, node.current, visitedNodes)) {
          worklist.add(new VisitedNodes(next, unmodVisitedIncludingCurrent));
        }
      }
      currDepth++;
    }

    return root;
  }

  /**
   * A worklist entry pairing a {@link Node} with the signatures visited on its way from the
   * exploration root.
   *
   * <p>The visited-nodes set is immutable.
   */
  private static final class VisitedNodes {
    final Node current;
    /** The immutable set of signatures visited from the root to this node. */
    private final Set<MethodSignature> visitedNodes;

    VisitedNodes(Node current, Set<MethodSignature> parentVisitedNodes) {
      this.current = current;
      this.visitedNodes = parentVisitedNodes;
    }

    /** Returns the immutable set of signatures visited between the root and this node. */
    Set<MethodSignature> getVisitedNodes() {
      return visitedNodes;
    }
  }

  /** Return all direct predecessors of curr in this callgraph, skipping ones already visited. */
  private static Set<Node> calculatePredecessors(
      CallGraph callGraph, Node curr, Set<? extends MethodSignature> visitedNodes) {
    Set<Node> predecessors = new LinkedHashSet<>();
    for (Call i : callGraph.incoming.getOrDefault(curr.signature, Collections.emptySet())) {
      // Only add the caller if it was not already visited to prevent loops.
      if (i.caller.signature instanceof MethodSignature
          && !visitedNodes.contains(i.caller.signature)
          && !i.caller.signature.equals(curr.signature)) {
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

  /** Return all direct successors of curr in this callgraph, skipping ones already visited. */
  private static Set<Node> calculateSuccessors(
      CallGraph callGraph, Node curr, Set<? extends MethodSignature> visitedNodes) {
    Set<Node> successors = new LinkedHashSet<>();
    for (Call i : callGraph.outgoing.getOrDefault(curr.signature, Collections.emptySet())) {
      // Only add the successor if it was not already visited to prevent loops.
      if (!visitedNodes.contains(i.getTarget()) && !i.getTarget().equals(curr.signature)) {
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

  /** Expands a node using the call graph and the signatures visited on the current path. */
  @FunctionalInterface
  private interface NodeExpander {
    Collection<Node> expand(
        CallGraph callGraph, Node node, Set<? extends MethodSignature> visitedNodes);
  }
}
