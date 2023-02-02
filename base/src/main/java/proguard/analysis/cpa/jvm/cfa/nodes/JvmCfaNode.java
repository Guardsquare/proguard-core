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

package proguard.analysis.cpa.jvm.cfa.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.util.InstructionClassifier;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;

/**
 * A node representing a code location of a JVM method identified by a {@link MethodSignature} and an offset.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmCfaNode
    implements CfaNode<JvmCfaEdge, MethodSignature>
{

    private final List<JvmCfaEdge> leavingEdges;
    private final List<JvmCfaEdge> enteringEdges;
    private final MethodSignature  signature;
    private final int              offset;
    private final Clazz            clazz;

    /**
     * Create a JVM CFA node without edges. Since in most cases we expect to have just one element in the lists of leaving and entering edges the lists are initialized with size 1.
     *
     * @param signature the signature of the method the node belongs to
     * @param offset    a number indicating the program location offset of the node
     * @param clazz     the class of the method the node belongs to
     */
    public JvmCfaNode(MethodSignature signature, int offset, Clazz clazz)
    {
        leavingEdges = new ArrayList<>(1);
        enteringEdges = new ArrayList<>(1);
        this.signature = signature;
        this.offset = offset;
        this.clazz = clazz;
    }

    /**
     * Create JVM CFA node with the specified entering and exiting edges.
     *
     * @param leavingEdges  a list of edges leaving the node
     * @param enteringEdges a list of edges entering the node
     * @param signature     the signature of the method the node belongs to
     * @param offset        a number indicating the program location offset of the node
     * @param clazz         the class of the method the node belongs to
     */
    public JvmCfaNode(List<JvmCfaEdge> leavingEdges, List<JvmCfaEdge> enteringEdges, MethodSignature signature, int offset, Clazz clazz)
    {
        this.leavingEdges = leavingEdges;
        this.enteringEdges = enteringEdges;
        this.signature = signature;
        this.offset = offset;
        this.clazz = clazz;
    }

    // Implementations for CfaNode

    @Override
    public List<JvmCfaEdge> getLeavingEdges()
    {
        return leavingEdges;
    }

    @Override
    public List<JvmCfaEdge> getEnteringEdges()
    {
        return enteringEdges;
    }

    @Override
    public boolean isEntryNode()
    {
        return offset == 0;
    }

    @Override
    public boolean isExitNode()
    {
        return isReturnExitNode() || isExceptionExitNode();
    }

    @Override
    public MethodSignature getSignature()
    {
        return signature;
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    // each node is identified by a hash calculated from the method signature, and the offset, and the class (e.g. to distinguish the catch nodes from the ones with same offset)
    @Override
    public int hashCode()
    {
        return Objects.hash(signature, offset, getClass());
    }

    /**
     * Returns the class the node belongs to.
     */
    public Clazz getClazz()
    {
        return clazz;
    }

    /**
     * Adds an edge leaving the node.
     */
    public void addLeavingEdge(JvmCfaEdge edge)
    {
        leavingEdges.add(edge);
    }

    /**
     * Adds an edge entering the node.
     */
    public void addEnteringEdge(JvmCfaEdge edge)
    {
        enteringEdges.add(edge);
    }

    /**
     * If the node is the location after a method invocation, returns the entering {@link JvmInstructionCfaEdge} for the method invocation, empty otherwise.
     */
    public Optional<JvmCfaEdge> getEnteringInvokeEdge()
    {
        return enteringEdges.stream()
                            .filter(e -> e instanceof JvmInstructionCfaEdge
                                         && InstructionClassifier.isInvoke(((JvmInstructionCfaEdge) e).getInstruction().opcode))
                            .findFirst();
    }

    /**
     * If the node is the location before a method invocation, returns the leaving {@link JvmInstructionCfaEdge} for the method invocation, empty otherwise.
     */
    public Optional<JvmCfaEdge> getLeavingInvokeEdge()
    {
        return leavingEdges.stream()
                            .filter(e -> e instanceof JvmInstructionCfaEdge
                                         && InstructionClassifier.isInvoke(((JvmInstructionCfaEdge) e).getInstruction().opcode))
                            .findFirst();
    }

    /**
     * Returns the edges entering the node that do not come from another method.
     */
    public Collection<JvmCfaEdge> getEnteringIntraproceduralEdges()
    {
        return enteringEdges.stream()
            .filter(e -> !(e instanceof JvmCallCfaEdge))
            .collect(Collectors.toList());
    }

    /**
     * Returns the edges leaving the node that do not come from another method.
     */
    public Collection<JvmCfaEdge> getLeavingIntraproceduralEdges()
    {
        return leavingEdges
            .stream()
            .filter(e -> !(e instanceof JvmCallCfaEdge))
            .collect(Collectors.toList());
    }

    /**
     * Returns the edges entering the node that come from another method.
     */
    public Collection<JvmCallCfaEdge> getLeavingInterproceduralEdges()
    {
        return leavingEdges.stream()
                           .filter(e -> e instanceof JvmCallCfaEdge)
                           .map(JvmCallCfaEdge.class::cast)
                           .collect(Collectors.toList());
    }

    /**
     * Returns all the interprocedural call edges leaving the node with target method the code of which is known.
     */
    public Collection<JvmCallCfaEdge> getKnownMethodCallEdges()
    {
        return getLeavingInterproceduralEdges().stream()
                                               .filter(e -> !(e.getTarget() instanceof JvmUnknownCfaNode))
                                               .collect(Collectors.toList());
    }

    @Override
    public String toString()
    {
        return "JvmCfaNode{" + signature.toString() + ":" + offset + '}';
    }
}
