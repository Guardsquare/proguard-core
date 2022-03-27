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

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;
import proguard.classfile.util.ClassUtil;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;

/**
 * The {@link JvmTaintTransferRelation} is parametrized by a set of {@link TaintSource} methods.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintTransferRelation
    extends JvmTransferRelation<TaintAbstractState>
{

    private final Map<String, TaintSource> taintSources;

    /**
     * Create a taint transfer relation.
     *
     * @param taintSources a mapping from fully qualified names to taint sources
     */
    public JvmTaintTransferRelation(Map<String, TaintSource> taintSources)
    {
        this.taintSources = taintSources;
    }

    // implementations for JvmTransferRelation

    @Override
    public void invokeMethod(JvmAbstractState<TaintAbstractState> state, Call call, List<TaintAbstractState> operands)
    {
        // TODO heap tainting
        MethodSignature target = call.getTarget();
        TaintSource detectedSource = taintSources.get(target.getFqn());
        int pushCount = ClassUtil.internalTypeSize(target.descriptor.returnType == null ? "?" : target.descriptor.returnType);
        TaintAbstractState answerContent = operands.stream().reduce(getAbstractDefault(), TaintAbstractState::join);

        if (detectedSource != null && detectedSource.taintsReturn && !answerContent.contains(detectedSource))
        {
            answerContent = answerContent.copy();
            answerContent.add(detectedSource);
        }

        for (int i = 0; i < pushCount; i++)
        {
            state.push(answerContent);
        }
        Map<String, TaintAbstractState> fqnToValue = new HashMap<>();

        if (detectedSource != null)
        {
            TaintAbstractState newValue = new TaintAbstractState(detectedSource);
            for (String fqn : detectedSource.taintsGlobals)
            {
                fqnToValue.merge(fqn, newValue, TaintAbstractState::join);
            }
        }
        fqnToValue.forEach(state::setStatic);
    }

    @Override
    public TaintAbstractState getAbstractDefault()
    {
        return TaintAbstractState.bottom;
    }
}
