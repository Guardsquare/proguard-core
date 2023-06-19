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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;

import java.util.Objects;

import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUFFER;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING_BUILDER;
import static proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE;


/**
 * An {@link AbstractState} for tracking JVM values.
 *
 * @author James Hamilton
 */
public class ValueAbstractState implements LatticeAbstractState<ValueAbstractState>
{
    private static final Logger logger = LogManager.getLogger(ValueAbstractState.class);

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

    /**
     * Update the {@link Value} associated with this abstract state.
     */
    public void setValue(Value value)
    {
        this.value = value;
    }

    @Override
    public ValueAbstractState join(ValueAbstractState abstractState)
    {
        ValueAbstractState result = abstractState.equals(this) ?
                this :
                new ValueAbstractState(this.value.generalize(abstractState.value));

        logger.trace("join({}, {}) = {}", this, abstractState, result);

        return result;
    }

    @Override
    public boolean isLessOrEqual(ValueAbstractState abstractState)
    {
        return abstractState == UNKNOWN || join(abstractState).equals(abstractState);
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


        if (value.internalType() != null)
        {
            switch(value.internalType())
            {
                case TYPE_JAVA_LANG_STRING:
                    // We want all equal strings to be treated as the same
                    // regardless if it's a different particular reference.
                    if (value.isParticular() && that.value.isParticular())
                    {
                        Object stringA = value.referenceValue().value();
                        Object objectB = that.value.referenceValue().value();

                        return stringA.equals(objectB);
                    }
                    break;
                case TYPE_JAVA_LANG_STRING_BUILDER:
                case TYPE_JAVA_LANG_STRING_BUFFER:
                    // if both objects are null they are equal
                    if (value instanceof TypedReferenceValue && that.value instanceof TypedReferenceValue
                        && ((TypedReferenceValue) value).isNull() == Value.ALWAYS && ((TypedReferenceValue) that.value).isNull() == Value.ALWAYS)
                    {
                        return true;
                    }
                    // String String(Builder|Buffer) don't implement equals; we
                    // need to check if they're the same by checking their ID
                    // and their String value.
                    if (value.isParticular() && that.value.isParticular()
                        && value instanceof IdentifiedReferenceValue && that.value instanceof IdentifiedReferenceValue)
                    {
                        Object idA     = ((IdentifiedReferenceValue)      value).id;
                        Object idB     = ((IdentifiedReferenceValue) that.value).id;
                        String stringA = value.referenceValue().value().toString();
                        String stringB = that.value.referenceValue().value().toString();

                        return idA.equals(idB) && stringA.equals(stringB);
                    }
                    break;
            }
        }

        return value.equals(that.value);
    }


    @Override
    public int hashCode()
    {
        if (value.internalType() != null)
        {
            switch (value.internalType())
            {
                case TYPE_JAVA_LANG_STRING:
                    // For particular Strings, we simply use the String hashCode.
                    if (value.isParticular())
                    {
                        return value.referenceValue().value().hashCode();
                    }
                    break;
                case TYPE_JAVA_LANG_STRING_BUILDER:
                case TYPE_JAVA_LANG_STRING_BUFFER:
                    // Since String(Builder|Buffer) don't implement hashCode,
                    // we use their type and ID for the hash code.
                    if (value.isSpecific())
                    {
                        return Objects.hash(value.internalType(), ((IdentifiedReferenceValue) value).id);
                    }
                    break;
            }
        }

        return Objects.hash(value);
    }

    @Override
    public String toString()
    {
        return "ValueAbstractState(" + value + ")";
    }
}
