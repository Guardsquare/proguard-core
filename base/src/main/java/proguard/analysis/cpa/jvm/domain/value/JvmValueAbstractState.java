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


/**
 * An {@link AbstractState} for tracking JVM values.
 */
public class JvmValueAbstractState implements LatticeAbstractState<JvmValueAbstractState>
{
    
    private        final Value                 value;
    public  static final JvmValueAbstractState top = new JvmValueAbstractState(null);

    public JvmValueAbstractState(Value value)
    {
        this.value = value;
    }

    @Override
    public JvmValueAbstractState join(JvmValueAbstractState abstractState)
    {
        return abstractState.equals(this) ? this : top;
    }

    @Override
    public boolean isLessOrEqual(JvmValueAbstractState abstractState)
    {
        return abstractState == top || abstractState.equals(this);
    }

    @Override
    public AbstractState copy()
    {
        return new JvmValueAbstractState(value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JvmValueAbstractState that = (JvmValueAbstractState) o;
        
        if (value instanceof ParticularReferenceValue &&
            that.value instanceof ParticularReferenceValue &&    
            ((ParticularReferenceValue)value).value() instanceof String &&
            ((ParticularReferenceValue)that.value).value() instanceof String
        )
        {
            return ((ParticularReferenceValue)value).value().equals(((ParticularReferenceValue)that.value).value());
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
}
