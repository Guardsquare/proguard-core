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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.Signature;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.defaults.BreadthFirstWaitlist;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CallEdge;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentTransferRelation;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This {@link TransferRelation} extends an analysis inter-procedurally. The transfer relation applies as close as possible the algorithms described in {@see
 * https://dl.acm.org/doi/pdf/10.1145/3368089.3409718}. On a high level the task of this domain-independent transfer relation is to extend the intra-procedural domain-dependent transfer relation of a
 * {@link CpaWithBamOperators} inter-procedurally. For more details on how the transfer relation works see {@link BamTransferRelation#getAbstractSuccessors(AbstractState, Precision)}.
 *
 * @author Carlo Alberto Pozzoli
 */
public class BamTransferRelation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    implements TransferRelation
{

    private static final Logger                                              log               = LogManager.getLogger(BamTransferRelation.class);
    private final        CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa;
    // TODO: maybe we don't need the cfa
    private final        Cfa<CfaNodeT, CfaEdgeT, SignatureT>                 cfa;
    private final        Stack<StackEntry>                                   stack             = new Stack<>();
    private              boolean                                             fixedPointReached = false;
    private final        CfaNodeT                                            mainLocation;
    private final        BamCache<SignatureT>                                cache;
    private              int                                                 maxCallStackDepth = -1;
    private final        StopOperator                                        fixedPointStopOperator;
    private final        AbortOperator                                       abortOperator;

    /**
     * Create a BAM transfer relation with an unlimited call stack.
     *
     * @param wrappedCpa   a wrapped CPA with BAM operators
     * @param cfa          a control flow automaton
     * @param mainFunction the signature of the main function of an analyzed program
     * @param cache        a cache for the block abstractions
     */
    public BamTransferRelation(CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa,
                               Cfa<CfaNodeT, CfaEdgeT, SignatureT> cfa,
                               SignatureT mainFunction, BamCache<SignatureT> cache)
    {
        this(wrappedCpa, cfa, mainFunction, cache, -1, NeverAbortOperator.INSTANCE);
    }

    /**
     * Create a BAM transfer relation with a specified maximum call stack depth. When the call stack meets its size limit the method call analysis is delegated to the wrapped intra-procedural transfer
     * relation.
     *
     * @param wrappedCpa        a wrapped CPA with BAM operators
     * @param cfa               a control flow automaton
     * @param mainFunction      the signature of the main function of an analyzed program
     * @param cache             a cache for the block abstractions
     * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally.
     *                          0 means intra-procedural analysis.
     *                          < 0 means no maximum depth.
     * @param abortOperator     an abort operator used for computing block abstractions
     */
    public BamTransferRelation(CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> wrappedCpa,
                               Cfa<CfaNodeT, CfaEdgeT, SignatureT> cfa,
                               SignatureT mainFunction,
                               BamCache<SignatureT> cache,
                               int maxCallStackDepth,
                               AbortOperator abortOperator)
    {
        this.wrappedCpa = wrappedCpa;
        this.cfa = cfa;
        this.mainLocation = cfa.getFunctionEntryNode(mainFunction);
        this.cache = cache;
        this.fixedPointStopOperator = new StopSepOperator(wrappedCpa.getAbstractDomain());
        this.maxCallStackDepth = maxCallStackDepth;
        this.abortOperator = abortOperator;
    }

    // implementations for TransferRelation

    /**
     * In order to implement an inter-procedural analysis the abstract successors are calculated for the following cases:
     *
     * <p>- Run the fixed point algorithm from the entry of the main method, continuing the analysis until a fixed point is reached (i.e. a function summary is provided for each function, also the
     * recursive ones). If there are no recursive calls the fixed point is reached after the first iteration, while in case of recursive calls, depending on the domain-dependent transfer relation,
     * they can be unrolled at each iteration until the fixed point is reached.
     *
     * <p>- Run the applyBlockAbstraction algorithm at every known procedure call. This algorithm takes care of retrieving the summary of the procedure from the cache (if available) or calls {@link
     * CpaAlgorithm} recursively on the new function to compute and store in the cache the summary of the procedure when called from the specified {@link AbstractState} (i.e. different parameters or
     * global state result in a different summary). Since we have no information on the code of the target of {@link SymbolicCall} this type of calls is delegated to the intra-procedural transfer
     * relation instead of being analyzed by the applyBlockAbstraction algorithm. The result of the block abstraction on the intra-procedural level is simply generating a successor (or successors in
     * case there are multiple call edges, e.g. for unknown runtime type of an object) abstract state that has as location the next node of the {@link Cfa}. The recursion can be limited at a maximum
     * call stack size. The intra-procedural transfer relation is also applied in case the max call stack size is reached.
     *
     * <p>- Apply the underlying intra-procedural transfer relation to all the other non-exit nodes in order to act as the wrapped transfer relation when procedure calls are not involved.
     *
     * <p>- Exit nodes reached are the base cases of the recursion (along with the stop operator), in this case the transfer relation returns with no successors.
     */
    @Override
    public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState abstractState, Precision precision)
    {
        if (!(abstractState instanceof ProgramLocationDependent))
        {
            throw new IllegalArgumentException("The abstract state of type " + AbstractState.class + " is not location dependent");
        }

        CfaNodeT                  currentLocation    = ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) abstractState).getProgramLocation();
        Collection<AbstractState> abstractSuccessors = new ArrayList<>();

        // executed only on the first call
        if (stack.isEmpty() && currentLocation.equals(mainLocation))
        {
            abstractSuccessors.addAll(fixedPoint(abstractState, currentLocation, precision));
        }
        // call location
        else if (currentLocation.getLeavingEdges().stream().anyMatch(e -> e instanceof CallEdge))
        {
            // there might be multiple calls, in this case we generate successors for all of them and eventually let the merge operator handle the results
            for (CfaEdgeT callEdge : currentLocation.getLeavingEdges().stream().filter(e -> e instanceof CallEdge).collect(Collectors.toList()))
            {
                // if the call target is an unknown node (i.e., either the method code of a symbolic call is not available or a library call is excluded from the analysis)
                // similarly if the maximum call depth is reached we delegate to the intra-procedural wrapped transfer relation instead of calling recursively applyBlockAbstraction
                if ((maxCallStackDepth < 0 || stack.size() < maxCallStackDepth)
                    && !callEdge.getTarget().isUnknownNode())
                {
                    abstractSuccessors.addAll(applyBlockAbstraction(abstractState, precision, (CfaEdge<CfaNodeT> & CallEdge) callEdge));
                }
                else
                {
                    abstractSuccessors.add(((ProgramLocationDependentTransferRelation) wrappedCpa.getTransferRelation()).getEdgeAbstractSuccessor(abstractState, callEdge, precision));
                }
            }
        }
        // non-call instruction, apply wrapped inter-procedural transfer relation
        // the exit node case is not checked specifically because in case of exit node the wrapped intra-procedural transfer relation does not produce successors
        else
        {
            abstractSuccessors.addAll(wrappedCpa.getTransferRelation().getAbstractSuccessors(abstractState, precision));
        }

        return abstractSuccessors;
    }

    /**
     * Returns the maximal call stack depth. If negative the maximum call stack depth is unlimited.
     */
    public int getMaxCallStackDepth()
    {
        return maxCallStackDepth;
    }

    /**
     * Returns the wrapped domain-dependent intra-procedural CPA.
     */
    public CpaWithBamOperators<CfaNodeT, CfaEdgeT, SignatureT> getWrappedCpa()
    {
        return wrappedCpa;
    }

    /**
     * By default the {@link Waitlist} used by the applyBlockAbstraction algorithm is a {@link BreadthFirstWaitlist}, this method can be overridden to provide a different waitlist.
     */
    protected Waitlist getWaitlist()
    {
        return new BreadthFirstWaitlist();
    }

    /**
     * By default the {@link ReachedSet} used by the applyBlockAbstraction algorithm is a {@link ProgramLocationDependentReachedSet}, this method can be overridden to provide a different reached set.
     */
    protected ReachedSet getReachedSet()
    {
        return new ProgramLocationDependentReachedSet<>();
    }

    /**
     * Returns BAM cache storing analysis result for various method calls.
     */
    public BamCache<SignatureT> getCache()
    {
        return cache;
    }

    /**
     * Returns the CFA used by the transfer relation.
     */
    public Cfa<CfaNodeT, CfaEdgeT, SignatureT> getCfa()
    {
        return cfa;
    }

    private Collection<? extends AbstractState> fixedPoint(AbstractState entryState, CfaNodeT currentLocation, Precision precision)
    {
        Collection<? extends AbstractState> blockResult = Collections.emptyList();

        while (!fixedPointReached)
        {
            fixedPointReached = true;
            blockResult = applyBlockAbstraction(entryState, precision, null);
        }

        return blockResult;
    }

    private <CfaCallEdgeT extends CfaEdge<CfaNodeT> & CallEdge> Collection<? extends AbstractState> applyBlockAbstraction(AbstractState callState, Precision precision, CfaCallEdgeT callEdge)
    {

        ReachedSet reached         = getReachedSet();
        Waitlist   waitlist        = getWaitlist();
        Call       call            = callEdge == null ? null : callEdge.getCall();
        CfaNodeT   entryNode       = call != null
                                     ? cfa.getFunctionEntryNode(callEdge.getTarget().getSignature())
                                     : ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) callState).getProgramLocation();
        SignatureT currentFunction = entryNode.getSignature();

        // TODO: maybe we can have a better way to identify that the function was called from fixedPoint than having a null call
        // apply the reduce operator to the entry state (i.e. discard information not relevant in the called procedure context (e.g. local variables of the caller)
        // this step is not necessary if we are calculating the block abstraction of the main method
        AbstractState reducedEntryState = call != null
                                          ? wrappedCpa.getReduceOperator().reduce(callState, cfa.getFunctionEntryNode(currentFunction), call)
                                          : callState;

        Optional<AbstractState> previousCall = stack.stream()
                                                    .filter(x -> x.function.equals(currentFunction) && wrappedCpa.getAbstractDomain().isLessOrEqual(reducedEntryState, x.entryState))
                                                    .map(x -> x.entryState)
                                                    .findFirst();

        // check if there are calls to the same function on the stack that cover the current call
        if (previousCall.isPresent())
        {
            // if this is not the first call, get from the cache the result of the unrolling of the recursive procedure that has been already calculated
            BlockAbstraction cacheEntry = cache.get(previousCall.get(), precision, currentFunction);
            if (cacheEntry != null)
            {
                reached = cacheEntry.getReachedSet();
            }
            // if this is the first unrolling the recursive call is not analyzed
            else
            {
                stack.peek().incompleteCallStates.add(callState);
                fixedPointReached = false;
            }
        }
        else
        {
            // get previously calculated results from the cache
            BlockAbstraction cacheEntry = cache.get(reducedEntryState, precision, currentFunction);

            if (cacheEntry != null)
            {
                // TODO: these might be different waitlist/reached set types if the cache is initialized externally (things that is not currently possible). Shall we add all elements from the them
                //  instead of copying?
                reached = cacheEntry.getReachedSet();
                waitlist = cacheEntry.getWaitlist();
            }
            else
            {
                reached.add(reducedEntryState);
                waitlist.add(reducedEntryState);
            }

            stack.push(new StackEntry(currentFunction, reducedEntryState));

            // analyze the current procedure call with the CPA algorithm, this is the recursive step of the BAM CPA
            // n.b. if the procedure has been already analyzed completely for the input the CPA algorithm will return immediately
            new CpaAlgorithm(this, wrappedCpa.getMergeOperator(), wrappedCpa.getStopOperator(), wrappedCpa.getPrecisionAdjustment()).run(reached, waitlist, abortOperator);

            StackEntry stackEntry = stack.pop();

            // since the fixed point has not been reached all the calls not analyzed are added to the waitlist
            if (!stackEntry.incompleteCallStates.isEmpty())
            {
                // the call to the current method will be added to the waitlist of the caller
                if (!stack.isEmpty())
                {
                    stack.peek().incompleteCallStates.add(callState);
                }

                for (AbstractState incompleteCallState : stackEntry.incompleteCallStates)
                {
                    waitlist.add(incompleteCallState);
                }
            }

            cacheEntry = cache.get(reducedEntryState, precision, currentFunction);

            // check if the fixed point has not been reached at this iteration (i.e. some new exit state is not covered by one already calculated)
            if (cacheEntry != null)
            {
                // TODO: as above, we might want to add all instead of copying
                ReachedSet reachedOld = cacheEntry.getReachedSet();

                for (AbstractState reachedState : reached.asCollection())
                {
                    CfaNodeT reachedLocation = ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) reachedState).getProgramLocation();
                    if (reachedLocation.getSignature().equals(currentFunction)
                        && reachedLocation.isExitNode()
                        && !(fixedPointStopOperator.stop(reachedState, reachedOld.asCollection(), null)))
                    {
                        if (!stack.isEmpty())
                        {
                            stack.peek().incompleteCallStates.add(callState);
                        }
                        fixedPointReached = false;
                        break;
                    }
                }
            }

            cache.put(reducedEntryState, precision, currentFunction, new BlockAbstraction(reached, waitlist));
        }

        Collection<? extends AbstractState> exitStates = reached.asCollection();
        // TODO: as before, maybe we can have a better way to identify that the function was called from fixedPoint
        if (call != null)
        {
            // reconstruct the next state of the caller procedure applying the expand and reduce operators.
            exitStates = exitStates.stream()
                                   .filter(e -> ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) e).getProgramLocation().isExitNode())
                                   .map(e -> wrappedCpa.getExpandOperator().expand(callState, e, entryNode, call))
                                   .map(e -> wrappedCpa.getRebuildOperator().rebuild(callState, e))
                                   .collect(Collectors.toSet());
        }

        return exitStates;
    }

    private class StackEntry
    {

        public final SignatureT         function;
        public final AbstractState      entryState;
        public final Set<AbstractState> incompleteCallStates = new HashSet<>();

        public StackEntry(SignatureT function, AbstractState entryState)
        {
            this.function = function;
            this.entryState = entryState;
        }
    }
}
