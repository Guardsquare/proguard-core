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

package proguard.analysis.cpa.jvm.util;

import java.util.Arrays;
import java.util.function.Supplier;
import proguard.analysis.CallResolver;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCallCfaEdge;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode;
import proguard.analysis.datastructure.callgraph.CallGraph;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.LibraryClass;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.RefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.visitors.JvmIntraproceduralCfaFillerAllInstructionVisitor;

/**
 * This is a util class for creating {@link Cfa}s.
 *
 * @author Dmitry Ivanov
 */
public class CfaUtil
{

    /**
     * Returns a CFA for the given program class pool.
     *
     * @param programClassPool a program class pool
     */
    public static JvmCfa createIntraproceduralCfaFromClassPool(ClassPool programClassPool)
    {
        return createIntraproceduralCfaFromClassPool(programClassPool, () -> true);
    }

    /**
     * Returns a CFA for the given program class pool. Allows to limit the number of processed code attributes with {@code shouldAnalyzeNextCodeAttribute}.
     *
     * @param programClassPool a program class pool
     */
    public static JvmCfa createIntraproceduralCfaFromClassPool(ClassPool programClassPool, Supplier<Boolean> shouldAnalyzeNextCodeAttribute)
    {
        JvmCfa cfa = new JvmCfa();
        programClassPool.classesAccept(new AllMethodVisitor(new AllAttributeVisitor(new JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa)
        {
            @Override
            public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
            {
                // Check whether this code attribute should be analyzed
                if (!shouldAnalyzeNextCodeAttribute.get())
                {
                    return;
                }
                super.visitCodeAttribute(clazz, method, codeAttribute);
            }
        })));
        return cfa;
    }

    /**
     * Adds interprocedural arcs to the given CFA from the call graph.
     *
     * @param cfa       a CFA
     * @param callGraph a call graph
     */
    public static void addInterproceduralEdgesToCfa(JvmCfa cfa, CallGraph callGraph)
    {
        callGraph.outgoing.values().forEach(calls -> calls.forEach(call ->
        {
            if (call instanceof SymbolicCall
                || ((ConcreteCall) call).getTargetClass() instanceof LibraryClass
                || ((ConcreteCall) call).getTargetMethod() instanceof ProgramMethod
                   && Arrays.stream(((ProgramMethod) ((ConcreteCall) call).getTargetMethod()).attributes).noneMatch(a -> a instanceof CodeAttribute))
            {
                cfa.addUnknownTargetInterproceduralEdge(call);
            }
            else
            {
                cfa.addInterproceduralEdge(call);
            }
        }));
    }

    /**
     * Create an interprocedural CFA from the given program class pool and call graph.
     *
     * @param programClassPool a program class pool
     * @param callGraph        a call graph
     */
    public static JvmCfa createInterproceduralCfaFromClassPoolAndCallGraph(ClassPool programClassPool, CallGraph callGraph)
    {
        JvmCfa cfa = createIntraproceduralCfaFromClassPool(programClassPool);
        addInterproceduralEdgesToCfa(cfa, callGraph);
        return cfa;
    }

    /**
     * Create an interprocedural CFA from the given program class pool.
     *
     * @param programClassPool a program class pool
     */
    public static JvmCfa createInterproceduralCfaFromClassPool(ClassPool programClassPool)
    {
        CallGraph callGraph = new CallGraph();
        CallResolver resolver = new CallResolver.Builder(programClassPool,
                                                         new ClassPool(),
                                                         callGraph)
            .setEvaluateAllCode(true)
            .build();
        programClassPool.classesAccept(resolver);
        MethodSignature.clearCache();
        JvmCfa cfa = createIntraproceduralCfaFromClassPool(programClassPool);
        addInterproceduralEdgesToCfa(cfa, callGraph);
        return cfa;
    }

    /**
     * Produces a DOT graph representation of the given JVM control flow
     * automaton.
     */
    public static String toDot(JvmCfa cfa) {
        StringBuffer sb = new StringBuffer();
        sb.append("digraph G { ");
        sb.append("node")
                .append(Integer.toHexString(JvmUnknownCfaNode.INSTANCE.hashCode()))
                .append(" [label=\"unknown\",style=dotted]");

        cfa.getAllNodes().forEach(node -> {
            String nodeAId = "node" + Integer.toHexString(node.hashCode());
            sb.append(nodeAId).append(" [label=\"");

            if (node.isExitNode()) {
                sb.append("exit ").append(node.getSignature());
            } else if (node.isEntryNode()) {
                sb.append("entry ").append(node.getSignature());
            } else {
                sb.append(node.getOffset());
            }
            sb.append("\"");
            if (node.isExitNode() || node.isEntryNode()) {
                sb.append(",shape=rect");
            }
            sb.append("]\n");
            node.getLeavingEdges().forEach(edge -> {
                JvmCfaNode target = edge.getTarget();
                String nodeBId = "node" + Integer.toHexString(target.hashCode());
                sb.append(nodeAId)
                        .append(" -> ")
                        .append(nodeBId)
                        .append("[label=\"");
                if (edge instanceof JvmInstructionCfaEdge) {
                    Instruction instruction = ((JvmInstructionCfaEdge) edge).getInstruction();
                    sb.append(instruction);
                    if (instruction instanceof ConstantInstruction) {
                        edge.getSource().getClazz().constantPoolEntryAccept(((ConstantInstruction) instruction).constantIndex, new ConstantVisitor()
                        {
                            @Override
                            public void visitAnyConstant(Clazz clazz, Constant constant)
                            {
                                sb.append(constant);
                            }

                            @Override
                            public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
                            {
                                sb.append(" ")
                                        .append(refConstant.getClassName(clazz)).append(".")
                                        .append(refConstant.getName(clazz))
                                        .append(refConstant.getType(clazz));
                            }
                        });

                    }
                } else if (edge instanceof JvmCallCfaEdge) {
                    sb.append(((JvmCallCfaEdge) edge).getCall());
                } else {
                    sb.append(edge);
                }
                sb.append("\"");
                if (edge instanceof JvmCallCfaEdge) {
                    if (edge.getTarget() == JvmUnknownCfaNode.INSTANCE) {
                        sb.append(",style=dotted");
                    } else {
                        sb.append(",style=dashed");
                    }
                }
                sb.append("]\n");
            });
        });
        sb.append("}");
        return sb.toString();
    }
}
