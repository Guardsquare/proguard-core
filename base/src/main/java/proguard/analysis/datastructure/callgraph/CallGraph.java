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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.util.CallGraphWalker;

/**
 * Collection of all {@link Call}s in a program, optimized for retrieval of incoming and outgoing
 * edges for any method in constant time.
 */
public class CallGraph {

  private static final transient Logger log = LogManager.getLogger(CallGraph.class);
  public final Map<MethodSignature, Set<Call>> incoming;
  public final Map<MethodSignature, Set<Call>> outgoing;

  private final boolean concurrent;

  /** Create an empty call graph. */
  public CallGraph() {
    this(new HashMap<>(), new HashMap<>(), false);
  }

  protected CallGraph(
      Map<MethodSignature, Set<Call>> incoming,
      Map<MethodSignature, Set<Call>> outgoing,
      boolean concurrent) {
    this.incoming = incoming;
    this.outgoing = outgoing;
    this.concurrent = concurrent;
  }

  /**
   * Provides concurrency ready {@link CallGraph}, backed by {@link ConcurrentHashMap}s and by
   * {@link Collections#synchronizedSet(Set) synchronizedSet}s. Not needed without multithreading.
   */
  public static CallGraph concurrentCallGraph() {
    return new CallGraph(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), true);
  }

  /**
   * Add a {@link Call} to this call graph.
   *
   * @param call The call to be added.
   */
  public void addCall(Call call) {
    if (!(call.caller.signature instanceof MethodSignature)) {
      log.warn("Location of call {} is not a method", call);
      return;
    }
    if (call.getTarget() == null) {
      log.warn("Target of call {} is null", call);
      return;
    }

    outgoing.computeIfAbsent((MethodSignature) call.caller.signature, e -> newCallSet()).add(call);

    incoming.computeIfAbsent(call.getTarget(), e -> newCallSet()).add(call);
  }

  private Set<Call> newCallSet() {
    return concurrent ? Collections.synchronizedSet(new LinkedHashSet<>()) : new LinkedHashSet<>();
  }

  /** Clear the call graph references. */
  public void clear() {
    incoming.clear();
    outgoing.clear();
  }

  /**
   * See {@link #reconstructCallGraph(ClassPool, MethodSignature, int, int, Set)}
   *
   * @param programClassPool The current {@link ClassPool} of the program that can be used for
   *     mapping. class names to the actual {@link Clazz}.
   * @param start The {@link MethodSignature} of the method whose incoming call graph should be
   *     calculated.
   * @param stopMethods Set of {@link MethodSignature} to stop exploration at, if desired.
   * @return A {@link Node} that represents the single call graph root, i.e. the start method.
   */
  public Node reconstructCallGraph(
      ClassPool programClassPool, MethodSignature start, Set<MethodSignature> stopMethods) {
    return CallGraphWalker.predecessorPathsAccept(
        this, start, n -> handleUntil(programClassPool, n, stopMethods, null));
  }

  /**
   * Calculate the incoming call graph for a method of interest, showing how it can be reached from
   * a given Set of stop methods, which typically are Android lifecycle methods such as an
   * Activity's onCreate() method:
   *
   * <p>We have an inverted tree structure like the following example:
   *
   * <pre>{@code
   * onCreate() <-- predecessor -- proxy() <-- predecessor -- root()
   *                            onResume() <-- predecessor ----|  |
   *                        unusedMethod() <-- predecessor -------|
   *
   * }</pre>
   *
   * Here, {@code root()} is the method whose call graph is to be calculated, and the graph now
   * shows that it can be reached from {@code onCreate()} via {@code proxy()}, and also directly
   * from {@code onResume()} or {@code unusedMethod()}.
   *
   * @param programClassPool The current {@link ClassPool} of the program that can be used for
   *     mapping. class names to the actual {@link Clazz}.
   * @param start The {@link MethodSignature} of the method whose incoming call graph should be
   *     calculated.
   * @param maxDepth maximal depth of reconstructed {@link CallGraph} similar to {@link
   *     CallGraphWalker#MAX_DEPTH_DEFAULT}.
   * @param maxWidth maximal width of reconstructed {@link CallGraph} similar to {@link
   *     CallGraphWalker#MAX_WIDTH_DEFAULT}.
   * @param stopMethods Set of method signatures to stop exploration, for example for entry points
   * @return A {@link Node} that represents the single call graph root, i.e. the start method.
   */
  public Node reconstructCallGraph(
      ClassPool programClassPool,
      MethodSignature start,
      int maxDepth,
      int maxWidth,
      Set<MethodSignature> stopMethods) {
    return CallGraphWalker.predecessorPathsAccept(
        this, start, n -> handleUntil(programClassPool, n, stopMethods, null), maxDepth, maxWidth);
  }

  /**
   * Extension of {@link #reconstructCallGraph(ClassPool, MethodSignature, Set)} that also collects
   * all reached stop methods.
   *
   * @param programClassPool The current {@link ClassPool} of the program that can be used for
   *     mapping.
   * @param start The {@link MethodSignature} of the method whose incoming call graph should be
   *     calculated.
   * @param stopMethods A set of {@link MethodSignature} to stop exploration, e.g. app entry points
   * @param reachedMethods A set that will be filled with all reached stop methods
   * @return A {@link Node} that represents the single call graph root, i.e. the start method.
   */
  public Node reconstructCallGraph(
      ClassPool programClassPool,
      MethodSignature start,
      Set<MethodSignature> stopMethods,
      Set<MethodSignature> reachedMethods) {
    return CallGraphWalker.predecessorPathsAccept(
        this, start, n -> handleUntil(programClassPool, n, stopMethods, reachedMethods));
  }

  /**
   * Handler implementation for {@link CallGraphWalker#predecessorPathsAccept(CallGraph,
   * MethodSignature, Predicate)} that checks if one of a given set of stop methods has been reached
   * along the call graph paths.
   *
   * @param programClassPool The current {@link ClassPool} of the program that can be used for
   *     mapping class names to the actual {@link Clazz}.
   * @param current The {@link Node} that represents the currently discovered call graph node and
   *     its successors.
   * @param reachedMethods A set for collecting reached stop methods, can be null.
   * @return true if call graph exploration should continue, false otherwise.
   */
  private boolean handleUntil(
      ClassPool programClassPool,
      Node current,
      Set<MethodSignature> stopMethods,
      @Nullable Set<MethodSignature> reachedMethods) {

    MethodSignature currentSignature = current.signature;
    String currentClassName = currentSignature.getClassName();
    Clazz currentClass = programClassPool.getClass(currentClassName);

    if (currentClass == null) {
      log.warn("Could not find class {} in class pool", currentClassName);
      current.isTruncated = true;
      return false;
    }

    if (stopMethods.contains(currentSignature)) {
      if (reachedMethods != null) {
        reachedMethods.add(currentSignature);
      }
      return false;
    }

    return true;
  }
}
