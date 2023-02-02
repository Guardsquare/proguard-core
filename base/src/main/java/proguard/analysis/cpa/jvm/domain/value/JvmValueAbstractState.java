/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.analysis.cpa.jvm.domain.value;

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.classfile.Clazz;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ValueFactory;

import java.util.Objects;

import static proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState.FAKE_FIELD;
import static proguard.evaluation.value.ParticularReferenceValue.UNINITIALIZED;

/**
 */
public class JvmValueAbstractState extends JvmAbstractState<ValueAbstractState>
{
    private final ValueFactory valueFactory;

    /**
     * Create a JVM value abstract state.
     *
     * @param valueFactory    a ValueFactory which is used to create abstract values.
     * @param programLocation a CFA node
     * @param frame           a frame abstract state
     * @param heap            a heap abstract state
     * @param staticFields    a static field table
     */
    public JvmValueAbstractState(ValueFactory                                 valueFactory,
                                 JvmCfaNode                                   programLocation,
                                 JvmFrameAbstractState<ValueAbstractState>    frame,
                                 JvmHeapAbstractState<ValueAbstractState>     heap,
                                 MapAbstractState<String, ValueAbstractState> staticFields)
    {
        super(programLocation, frame, heap, staticFields);
        this.valueFactory = valueFactory;
    }

    /**
     * Returns the {@link ValueFactory}. 
     */
    public ValueFactory getValueFactory()
    {
        return valueFactory;
    }

    /**
     * Returns an abstract state at the {@code index}th position of the variable array,
     * the corresponding heap value for an {@link IdentifiedReferenceValue} or {@code defaultState} if there is no entry.
     */
    @Override
    public ValueAbstractState getVariableOrDefault(int index, ValueAbstractState defaultState)
    {
        ValueAbstractState value = super.getVariableOrDefault(index, defaultState);
        return value.getValue() instanceof IdentifiedReferenceValue ?
                heap.getFieldOrDefault(((IdentifiedReferenceValue) value.getValue()).id, FAKE_FIELD, defaultState) :
                value;
    }

    /**
     * Sets the {@code index}th position of the variable array to {@code state} and returns {@code state}.
     * If the array has to be extended, the added cells are padded with {@code defaultState}.
     * 
     * If the value is an {@link IdentifiedReferenceValue}, the corresponding heap value is also updated.
     */
    @Override
    public ValueAbstractState setVariable(int index, ValueAbstractState state, ValueAbstractState defaultState)
    {
        if (state.getValue() instanceof IdentifiedReferenceValue)
        {
            heap.setField(((IdentifiedReferenceValue) state.getValue()).id, FAKE_FIELD, state);
        }

        return super.setVariable(index, state, defaultState);
    }

    /**
     * Returns an {@link ValueAbstractState} for a new object of the given {@code className}.
     */
    @Override
    public ValueAbstractState newObject(String className)
    {
        IdentifiedReferenceValue value = (IdentifiedReferenceValue) valueFactory.createReferenceValue(className, null, true, true, UNINITIALIZED);
        ValueAbstractState jvmValueAbstractState = new ValueAbstractState(value);
        setField(value.id, FAKE_FIELD, jvmValueAbstractState);
        return jvmValueAbstractState;
    }

    /**
     * Returns an {@link ValueAbstractState} state for a new object of the given {@link Clazz}.
     */
    @Override
    public ValueAbstractState newObject(Clazz clazz)
    {
        IdentifiedReferenceValue value = (IdentifiedReferenceValue) valueFactory.createReferenceValue(clazz, UNINITIALIZED);
        ValueAbstractState jvmValueAbstractState = new ValueAbstractState(value);
        setField(value.id, FAKE_FIELD, jvmValueAbstractState);
        return jvmValueAbstractState;
    }

    @Override
    public JvmValueAbstractState join(JvmAbstractState<ValueAbstractState> abstractState)
    {
        JvmValueAbstractState answer = new JvmValueAbstractState(
            valueFactory,
            programLocation.equals(abstractState.getProgramLocation()) ? programLocation : topLocation,
            frame.join(abstractState.getFrame()),
            heap.join(abstractState.getHeap()),
            staticFields.join(abstractState.getStaticFields())
        );
        return equals(answer) ? this : answer;
    }

    @Override
    public JvmValueAbstractState copy()
    {
        return new JvmValueAbstractState(valueFactory, programLocation, frame.copy(), heap.copy(), staticFields.copy());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof JvmAbstractState))
        {
            return false;
        }
        JvmValueAbstractState other = (JvmValueAbstractState) obj;
        return valueFactory.equals(other.valueFactory) &&
                programLocation.equals(other.programLocation) &&
                frame.equals(other.frame) &&
                heap.equals(other.heap) &&
                staticFields.equals(other.staticFields);
    }


    @Override
    public int hashCode() {
        return Objects.hash(valueFactory, programLocation, frame, heap, staticFields);
    }
}
