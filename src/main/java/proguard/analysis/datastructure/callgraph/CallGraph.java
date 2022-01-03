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

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.util.CallGraphWalker;

/**
 * Collection of all {@link Call}s in a program, optimized for retrieval
 * of incoming and outgoing edges for any method in constant time.
 *
 * @author Samuel Hopstock
 */
public class CallGraph
{

    private static final transient Logger                          log      = LogManager.getLogger(CallGraph.class);
    public final                   Map<MethodSignature, Set<Call>> incoming = new HashMap<>();
    public final                   Map<MethodSignature, Set<Call>> outgoing = new HashMap<>();

    /**
     * If true, incoming edges are not explored further for known entry points.
     */
    private static final boolean STOP_AT_ENTRYPOINT = true;

    /**
     * Add a {@link Call} to this call graph.
     *
     * @param call The call to be added.
     */
    public void addCall(Call call)
    {
        if (!(call.caller.signature instanceof MethodSignature))
        {
            log.error("Location of call {} is not a method", call);
            return;
        }
        if (call.getTarget() == null)
        {
            log.error("Target of call {} is null", call);
            return;
        }

        outgoing.computeIfAbsent((MethodSignature) call.caller.signature, e -> new HashSet<>())
                .add(call);

        incoming.computeIfAbsent(call.getTarget(), e -> new HashSet<>())
                .add(call);
    }

    /**
     * Clear the call graph references.
     */
    public void clear()
    {
        incoming.clear();
        outgoing.clear();
    }

    /**
     * See {@link #reconstructCallGraph(ClassPool, MethodSignature, int, int)}
     *
     * @param programClassPool The current {@link ClassPool} of the program that can be used for mapping.
     *                         class names to the actual {@link Clazz}.
     * @param start            The {@link MethodSignature} of the method whose incoming call graph
     *                         should be calculated.
     * @return A {@link Node} that represents the single call graph root, i.e. the start method.
     */
    public Node reconstructCallGraph(ClassPool programClassPool, MethodSignature start)
    {
        return CallGraphWalker.predecessorPathsAccept(this,
                                                      start,
                                                      n -> handleUntilEntryPoint(programClassPool, n, null));
    }


    /**
     *Calculate the incoming call graph for a method of interest, showing how it can be reached.
     *
     * <p>
     * We have an inverted tree structure like the following example:
     * <pre>
     *     {@code
     *     onCreate() <-- predecessor -- proxy() <-- predecessor -- root()
     *                                onResume() <-- predecessor ----|  |
     *                            unusedMethod() <-- predecessor -------|
     *     }
     * </pre>
     * Here, {@code root()} is the method whose call graph is to be calculated, and the graph now shows
     * that it can be reached from {@code onCreate()} via {@code proxy()}, and also directly from
     * {@code onResume()} or {@code unusedMethod()}.
     * </p>
     *
     * <p>
     * Generally, we still can't be sure whether the top most methods (leaves in the tree) can be
     * reached themselves, if we don't find any incoming edges. But if these methods are {@link EntryPoint}s
     * of an Android app, they will most likely be called at some point in the app lifecycle.
     * </p>
     *
     * @param programClassPool The current {@link ClassPool} of the program that can be used for mapping.
     *                         class names to the actual {@link Clazz}.
     * @param start            The {@link MethodSignature} of the method whose incoming call graph
     *                         should be calculated.
     * @param maxDepth maximal depth of reconstructed {@link CallGraph} similar to {@link CallGraphWalker#MAX_DEPTH_DEFAULT}.
     * @param maxWidth maximal width of reconstructed {@link CallGraph} similar to {@link CallGraphWalker#MAX_WIDTH_DEFAULT}.
     * @return A {@link Node} that represents the single call graph root, i.e. the start method.
     */
    public Node reconstructCallGraph(ClassPool programClassPool, MethodSignature start, int maxDepth, int maxWidth)
    {
        return CallGraphWalker.predecessorPathsAccept(this,
                                                      start,
                                                      n -> handleUntilEntryPoint(programClassPool, n, null),
                                                      maxDepth,
                                                      maxWidth);
    }

    /**
     * Extension of {@link #reconstructCallGraph(ClassPool, MethodSignature)} that also collects
     * all {@link EntryPoint}s found along the way.
     *
     * @param programClassPool The current {@link ClassPool} of the program that can be used for mapping.
     * @param start            The {@link MethodSignature} of the method whose incoming call graph
     *                         should be calculated.
     * @param entryPoints      A set that will be filled with all {@link EntryPoint}s that are part
     *                         of the incoming call graph.
     * @return A {@link Node} that represents the single call graph root, i.e. the start method.
     */
    public Node reconstructCallGraph(ClassPool programClassPool, MethodSignature start, Set<EntryPoint> entryPoints)
    {
        return CallGraphWalker.predecessorPathsAccept(this,
                                                      start,
                                                      n -> handleUntilEntryPoint(programClassPool, n, entryPoints));
    }

    /**
     * Handler implementation for {@link CallGraphWalker#predecessorPathsAccept(CallGraph, MethodSignature, Predicate)}
     * that checks discovered paths if they have arrived at a known entry point.
     *
     * @param programClassPool The current {@link ClassPool} of the program that can be used for mapping
     *                         class names to the actual {@link Clazz}.
     * @param curr             The {@link Node} that represents the currently discovered call graph
     *                         node and its successors.
     * @param entryPoints      a set containing the entrypoints seen on this path, will be filled
     *                         during the reconstruction of the callgraph.
     * @return true if we have arrived at an entry point, so that the {@link CallGraphWalker} stops
     *     exploring this particular path.
     */
    private boolean handleUntilEntryPoint(ClassPool programClassPool, Node curr, Set<EntryPoint> entryPoints)
    {
        // Get all classes that contain known entryPoints and are superclasses of the current one
        Clazz currClass = programClassPool.getClass(curr.signature.getClassName());
        if (currClass == null)
        {
            log.error("Could not find class {} in class pool", curr.signature.getClassName());
            curr.isTruncated = true;
            return false;
        }
        Set<String> entrypointSuperclassNames = EntryPoint.WELL_KNOWN_ENTRYPOINT_CLASSES
            .stream()
            .filter(e -> classExtendsOrEquals(currClass, e.replace('.', '/')))
            .collect(Collectors.toSet());
        // If we are in a method overriding any known entrypoint, that's a call graph leaf
        Optional<EntryPoint> matchingEntrypoint = EntryPoint.WELL_KNOWN_ENTRYPOINTS
            .stream()
            .filter(e -> entrypointSuperclassNames.contains(e.className)
                         && e.methodName.equals(curr.signature.method))
            .findFirst();
        if (matchingEntrypoint.isPresent())
        {
            curr.matchingEntrypoint = matchingEntrypoint.get();
            if (entryPoints != null)
            {
                entryPoints.add(new EntryPoint(matchingEntrypoint.get().type, curr.signature.getClassName(), curr.signature.method));
            }
            return !STOP_AT_ENTRYPOINT;
        }
        return true;
    }

    /**
     * Check if a {@link Clazz} either matches the provided class name or extends
     * this provided class. Both direct and transitive inheritance is allowed.
     *
     * @param currClass The {@link Clazz} that might be equal to or a subclass of the provided class name.
     * @param className The potential super class name.
     * @return True if currClass is equal to className or is one of its subclasses.
     */
    private boolean classExtendsOrEquals(Clazz currClass, String className)
    {
        if (Objects.equals(currClass.getSuperName(), className))
        {
            return true;
        }
        if (currClass.getSuperClass() != null)
        {
            if (Objects.equals(currClass.getSuperClass().getName(), className))
            {
                return true;
            }
            return classExtendsOrEquals(currClass.getSuperClass(), className);
        }
        return false;
    }
}
