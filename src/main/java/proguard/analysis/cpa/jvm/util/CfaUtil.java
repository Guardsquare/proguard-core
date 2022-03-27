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
import proguard.analysis.CallResolver;
import proguard.analysis.cpa.defaults.Cfa;
import proguard.analysis.datastructure.callgraph.CallGraph;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.classfile.ClassPool;
import proguard.classfile.LibraryClass;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
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
        JvmCfa cfa = new JvmCfa();
        programClassPool.classesAccept(new AllMethodVisitor(new AllAttributeVisitor(new JvmIntraproceduralCfaFillerAllInstructionVisitor(cfa))));
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
}
