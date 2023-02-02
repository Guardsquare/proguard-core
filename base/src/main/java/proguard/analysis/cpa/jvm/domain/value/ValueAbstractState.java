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

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.Value;

import java.util.Objects;

import static proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE;


/**
 * An {@link AbstractState} for tracking JVM values.
 *
 * @author James Hamilton
 */
public class ValueAbstractState implements LatticeAbstractState<ValueAbstractState>
{
    private static final boolean DEBUG = System.getProperty("jvas") != null;

    public static final ValueAbstractState UNKNOWN = new ValueAbstractState(UNKNOWN_VALUE);
    private Value                 value;

    public ValueAbstractState(Value value)
    {
        this.value = value;
    }


    /**
     * Returns the {@link Value} associated with this abstract state.
     */
    public Value getValue()
    {
        return value;
    }

    @Override
    public ValueAbstractState join(ValueAbstractState abstractState)
    {
        ValueAbstractState result = abstractState.equals(this) ?
                this :
                new ValueAbstractState(this.value.generalize(abstractState.value));

        if (DEBUG) System.out.println("join(" + this + ", " + abstractState + ") = " + result);

        return result;
    }

    @Override
    public boolean isLessOrEqual(ValueAbstractState abstractState)
    {
        return abstractState == UNKNOWN || abstractState.equals(this);
    }

    @Override
    public AbstractState copy()
    {
        return new ValueAbstractState(value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValueAbstractState that = (ValueAbstractState) o;

        // We want all equal strings to be treated as the same
        // regardless if it's a different particular reference.
        if (isParticularString(this.value) && isParticularString(that.value))
        {
            return this.value.referenceValue().value().equals(that.value.referenceValue().value());
        }

        return Objects.equals(value, that.value);
    }


    @Override
    public int hashCode()
    {
        if (value instanceof ParticularReferenceValue && ((ParticularReferenceValue)value).value() instanceof String)
        {
            return ((ParticularReferenceValue)value).value().hashCode();
        }

        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "JvmValueAbstractState(" + value + ")";
    }

    private static boolean isParticularString(Value value)
    {
        return value instanceof ParticularReferenceValue &&
                ((ParticularReferenceValue) value).value() instanceof String;
    }

    public void setValue(Value value)
    {
        this.value = value;
    }
}
