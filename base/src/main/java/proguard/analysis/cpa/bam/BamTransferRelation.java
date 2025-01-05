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

package proguard.analysis.cpa.bam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentTransferRelation;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.Waitlist;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.AccessConstants;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * This {@link TransferRelation} extends an analysis inter-procedurally. The transfer relation
 * applies as close as possible the algorithms described in {@see
 * https://dl.acm.org/doi/pdf/10.1145/3368089.3409718}. On a high level the task of this
 * domain-independent transfer relation is to extend the intra-procedural domain-dependent transfer
 * relation of a {@link CpaWithBamOperators} inter-procedurally. For more details on how the
 * transfer relation works see {@link
 * BamTransferRelation#generateAbstractSuccessors(JvmAbstractState, Precision)}.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class BamTransferRelation<ContentT extends LatticeAbstractState<ContentT>>
    implements TransferRelation<JvmAbstractState<ContentT>> {

  private final BamCpa<ContentT> bamCpa;
  private final JvmCfa cfa;
  private final Stack<StackEntry> stack = new Stack<>();
  private final JvmCfaNode mainLocation;
  private final BamCache<ContentT> cache;
  private final int maxCallStackDepth;
  private final StopOperator<JvmAbstractState<ContentT>> fixedPointStopOperator;
  private boolean fixedPointReached = false;

  /**
   * Create a BAM transfer relation with an unlimited call stack.
   *
   * @param bamCpa a BAM cpa. It wraps a CPA that is called recursively when a method call is
   *     analyzed.
   * @param cfa a control flow automaton.
   * @param mainFunction the signature of the main function of an analyzed program.
   * @param cache a cache for the block abstractions.
   */
  public BamTransferRelation(
      BamCpa<ContentT> bamCpa, JvmCfa cfa, MethodSignature mainFunction, BamCache<ContentT> cache) {
    this(bamCpa, cfa, mainFunction, cache, -1);
  }

  /**
   * Create a BAM transfer relation with a specified maximum call stack depth. When the call stack
   * meets its size limit the method call analysis is delegated to the wrapped intra-procedural
   * transfer relation.
   *
   * @param bamCpa a BAM cpa. It wraps a CPA that is called recursively when a method call is
   *     analyzed.
   * @param cfa a control flow automaton.
   * @param mainFunction the signature of the main function of an analyzed program.
   * @param cache a cache for the block abstractions.
   * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally. 0 means
   *     intra-procedural analysis. < 0 means no maximum depth.
   */
  public BamTransferRelation(
      BamCpa<ContentT> bamCpa,
      JvmCfa cfa,
      MethodSignature mainFunction,
      BamCache<ContentT> cache,
      int maxCallStackDepth) {
    this.bamCpa = bamCpa;
    this.cfa = cfa;
    this.mainLocation = cfa.getFunctionEntryNode(mainFunction);
    this.cache = cache;
    this.fixedPointStopOperator = new StopSepOperator<>(bamCpa.getAbstractDomain());
    this.maxCallStackDepth = maxCallStackDepth;
  }

  // implementations for TransferRelation

  /**
   * In order to implement an inter-procedural analysis the abstract successors are calculated for
   * the following cases:
   *
   * <p>- Run the fixed point algorithm from the entry of the main method, continuing the analysis
   * until a fixed point is reached (i.e. a function summary is provided for each function, also the
   * recursive ones). If there are no recursive calls the fixed point is reached after the first
   * iteration, while in case of recursive calls, depending on the domain-dependent transfer
   * relation, they can be unrolled at each iteration until the fixed point is reached.
   *
   * <p>- Run the applyBlockAbstraction algorithm at every known procedure call. This algorithm
   * takes care of retrieving the summary of the procedure from the cache (if available) or calls
   * {@link CpaAlgorithm} recursively on the new function to compute and store in the cache the
   * summary of the procedure when called from the specified {@link AbstractState} (i.e. different
   * parameters or global state result in a different summary). Since we have no information on the
   * code of the target of {@link SymbolicCall} this type of calls is delegated to the
   * intra-procedural transfer relation instead of being analyzed by the applyBlockAbstraction
   * algorithm. The result of the block abstraction on the intra-procedural level is simply
   * generating a successor (or successors in case there are multiple call edges, e.g. for unknown
   * runtime type of the instance) abstract state that has as location the next node of the {@link
   * Cfa}. The recursion can be limited at a maximum call stack size. The intra-procedural transfer
   * relation is also applied in case the max call stack size is reached.
   *
   * <p>- Apply the underlying intra-procedural transfer relation to all the other non-exit nodes in
   * order to act as the wrapped transfer relation when procedure calls are not involved.
   *
   * <p>- Exit nodes reached are the base cases of the recursion (along with the stop operator), in
   * this case the transfer relation returns with no successors.
   */
  @Override
  public Collection<JvmAbstractState<ContentT>> generateAbstractSuccessors(
      JvmAbstractState<ContentT> abstractState, Precision precision) {
    JvmCfaNode currentLocation = abstractState.getProgramLocation();
    Collection<JvmAbstractState<ContentT>> abstractSuccessors = new ArrayList<>();

    // executed only on the first call
    if (stack.isEmpty() && currentLocation.equals(mainLocation)) {
      abstractSuccessors.addAll(fixedPoint(abstractState, precision));
    }
    // call location
    else if (currentLocation.getLeavingEdges().stream()
        .anyMatch(JvmCallCfaEdge.class::isInstance)) {
      // there might be multiple calls, in this case we generate successors for all of them and
      // eventually let the merge operator handle the results
      for (JvmCfaEdge callEdge :
          currentLocation.getLeavingEdges().stream()
              .filter(JvmCallCfaEdge.class::isInstance)
              .collect(Collectors.toList())) {
        // if the call target is an unknown node (i.e., either the method code of a symbolic call is
        // not available or a library call is excluded from the analysis)
        // similarly if the maximum call depth is reached we delegate to the intra-procedural
        // wrapped transfer relation instead of calling recursively applyBlockAbstraction
        if ((maxCallStackDepth < 0 || stack.size() < maxCallStackDepth)
            && !callEdge.getTarget().isUnknownNode()) {
          abstractSuccessors.addAll(
              applyBlockAbstraction(abstractState, precision, (JvmCallCfaEdge) callEdge));
        } else {
          abstractSuccessors.addAll(
              ((ProgramLocationDependentTransferRelation)
                      bamCpa.getIntraproceduralTransferRelation())
                  .generateEdgeAbstractSuccessors(abstractState, callEdge, precision));
        }
      }
    }
    // non-call instruction, apply wrapped inter-procedural transfer relation
    // the exit node case is not checked specifically because in case of exit node the wrapped
    // intra-procedural transfer relation does not produce successors
    else {
      abstractSuccessors.addAll(
          bamCpa
              .getIntraproceduralTransferRelation()
              .generateAbstractSuccessors(abstractState, precision));
    }

    return abstractSuccessors;
  }

  /**
   * By default, the {@link Waitlist} used by the applyBlockAbstraction algorithm is a {@link
   * BreadthFirstWaitlist}, this method can be overridden to provide a different waitlist.
   */
  protected Waitlist<JvmAbstractState<ContentT>> getWaitlist() {
    return new BreadthFirstWaitlist<>();
  }

  /**
   * By default, the {@link ReachedSet} used by the applyBlockAbstraction algorithm is a {@link
   * ProgramLocationDependentReachedSet}, this method can be overridden to provide a different
   * reached set.
   */
  protected ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> getReachedSet() {
    return new ProgramLocationDependentReachedSet<>();
  }

  /** Returns BAM cache storing analysis result for various method calls. */
  public BamCache<ContentT> getCache() {
    return cache;
  }

  /** Returns the CFA used by the transfer relation. */
  public JvmCfa getCfa() {
    return cfa;
  }

  private Collection<JvmAbstractState<ContentT>> fixedPoint(
      JvmAbstractState<ContentT> entryState, Precision precision) {
    Collection<JvmAbstractState<ContentT>> blockResult = Collections.emptyList();

    while (!fixedPointReached) {
      fixedPointReached = true;
      blockResult = applyBlockAbstraction(entryState, precision, null);
    }

    return blockResult;
  }

  private Collection<JvmAbstractState<ContentT>> applyBlockAbstraction(
      JvmAbstractState<ContentT> callState, Precision precision, JvmCallCfaEdge callEdge) {

    ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> reached = getReachedSet();
    Waitlist<JvmAbstractState<ContentT>> waitlist = getWaitlist();
    Call call = callEdge == null ? null : callEdge.getCall();
    JvmCfaNode entryNode =
        call != null
            ? cfa.getFunctionEntryNode(callEdge.getTarget().getSignature())
            : callState.getProgramLocation();
    MethodSignature currentFunction = entryNode.getSignature();

    // TODO: maybe we can have a better way to identify that the function was called from fixedPoint
    // than having a null call
    // apply the reduce operator to the entry state (i.e. discard information not relevant in the
    // called procedure context (e.g. local variables of the caller)
    // this step is not necessary if we are calculating the block abstraction of the main method
    JvmAbstractState<ContentT> reducedEntryState =
        call != null
            ? bamCpa
                .getReduceOperator()
                .reduce(callState, cfa.getFunctionEntryNode(currentFunction), call)
            : bamCpa.getReduceOperator().onMethodEntry(callState, isCallStatic(callState));

    Optional<JvmAbstractState<ContentT>> previousCall =
        stack.stream()
            .filter(
                x ->
                    x.function.equals(currentFunction)
                        && bamCpa
                            .getAbstractDomain()
                            .isLessOrEqual(reducedEntryState, x.entryState))
            .map(x -> x.entryState)
            .findFirst();

    // check if there are calls to the same function on the stack that cover the current call
    if (previousCall.isPresent()) {
      // if this is not the first call, get from the cache the result of the unrolling of the
      // recursive procedure that has been already calculated
      BlockAbstraction<ContentT> cacheEntry =
          cache.get(previousCall.get(), precision, currentFunction);
      if (cacheEntry != null) {
        reached = cacheEntry.getReachedSet();
      }
      // if this is the first unrolling the recursive call is not analyzed
      else {
        stack.peek().incompleteCallStates.add(callState);
        fixedPointReached = false;
      }
    } else {
      // get previously calculated results from the cache
      BlockAbstraction<ContentT> cacheEntry =
          cache.get(reducedEntryState, precision, currentFunction);

      if (cacheEntry != null) {
        // TODO: these might be different waitlist/reached set types if the cache is initialized
        // externally (things that is not currently possible). Shall we add all elements from the
        // them
        //  instead of copying?
        reached = cacheEntry.getReachedSet();
        waitlist = cacheEntry.getWaitlist();
      } else {
        reached.add(reducedEntryState);
        waitlist.add(reducedEntryState);
      }

      stack.push(new StackEntry(currentFunction, reducedEntryState));

      // analyze the current procedure call with the CPA algorithm, this is the recursive step of
      // the BAM CPA
      // n.b. if the procedure has been already analyzed completely for the input the CPA algorithm
      // will return immediately
      new CpaAlgorithm<>(bamCpa).run(reached, waitlist);

      StackEntry stackEntry = stack.pop();

      // since the fixed point has not been reached all the calls not analyzed are added to the
      // waitlist
      if (!stackEntry.incompleteCallStates.isEmpty()) {
        // the call to the current method will be added to the waitlist of the caller
        if (!stack.isEmpty()) {
          stack.peek().incompleteCallStates.add(callState);
        }

        for (JvmAbstractState<ContentT> incompleteCallState : stackEntry.incompleteCallStates) {
          waitlist.add(incompleteCallState);
        }
      }

      cacheEntry = cache.get(reducedEntryState, precision, currentFunction);

      // check if the fixed point has not been reached at this iteration (i.e. some new exit state
      // is not covered by one already calculated)
      if (cacheEntry != null) {
        // TODO: as above, we might want to add all instead of copying
        ProgramLocationDependentReachedSet<JvmAbstractState<ContentT>> reachedOld =
            cacheEntry.getReachedSet();

        for (JvmAbstractState<ContentT> reachedState : reached.asCollection()) {
          JvmCfaNode reachedLocation = reachedState.getProgramLocation();
          if (reachedLocation.getSignature().equals(currentFunction)
              && reachedLocation.isExitNode()
              && !(fixedPointStopOperator.stop(reachedState, reachedOld.asCollection(), null))) {
            if (!stack.isEmpty()) {
              stack.peek().incompleteCallStates.add(callState);
            }
            fixedPointReached = false;
            break;
          }
        }
      }

      cache.put(
          reducedEntryState, precision, currentFunction, new BlockAbstraction<>(reached, waitlist));
    }

    Collection<JvmAbstractState<ContentT>> exitStates = reached.asCollection();
    // TODO: as before, maybe we can have a better way to identify that the function was called from
    // fixedPoint
    if (call != null) {
      // reconstruct the next state of the caller procedure applying the expand and reduce
      // operators.
      exitStates =
          exitStates.stream()
              .filter(e -> e.getProgramLocation().isExitNode())
              .map(e -> bamCpa.getExpandOperator().expand(callState, e, entryNode, call))
              .map(e -> bamCpa.getRebuildOperator().rebuild(callState, e))
              .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    return exitStates;
  }

  private boolean isCallStatic(JvmAbstractState<ContentT> callState) {
    JvmCfaNode programLocation = callState.getProgramLocation();
    String methodName = programLocation.getSignature().getMethodName();
    String descriptor = String.valueOf(programLocation.getSignature().getDescriptor());
    return (programLocation.getClazz().findMethod(methodName, descriptor).getAccessFlags()
            & AccessConstants.STATIC)
        != 0;
  }

  private class StackEntry {

    public final Signature function;
    public final JvmAbstractState<ContentT> entryState;
    public final Set<JvmAbstractState<ContentT>> incompleteCallStates = new LinkedHashSet<>();

    public StackEntry(Signature function, JvmAbstractState<ContentT> entryState) {
      this.function = function;
      this.entryState = entryState;
    }
  }
}
