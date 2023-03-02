package proguard.evaluation;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.IdentifiedValueFactory;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.TypedReferenceValueFactory;
import proguard.evaluation.value.Value;

/**
 * This {@link TypedReferenceValueFactory} creates reference values that also represent their content.
 * <p>
 * Like {@link IdentifiedValueFactory}, it tracks {@link IdentifiedReferenceValue}s with a unique integer
 * ID.
 * <p>
 * Calling a `createReferenceValue` method will increment the internal referencedID counter and
 * return an object representing that {@link Value} with that new referenceID.
 * <p>
 * Calling a `createReferenceForId` method will return an object representing that {@link Value} with
 * the specified ID.
 */
public class ParticularReferenceValueFactory extends TypedReferenceValueFactory
{
    private int referenceID = 0;

    @Override
    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull,
                                               Clazz   creationClass,
                                               Method  creationMethod,
                                               int     creationOffset)
    {
        return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
    }

    @Override
    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull,
                                               Clazz   creationClass,
                                               Method  creationMethod,
                                               int     creationOffset,
                                               Object  value)
    {
        return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, value);
    }

    @Override
    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull)
    {
        return createReferenceValueForId(type, referencedClass, mayBeExtension, mayBeNull, referenceID++);
    }

    @Override
    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull,
                                               Object  value)
    {
        return createReferenceValueForId(type, referencedClass, mayBeExtension, mayBeNull, referenceID++, value);
    }

    @Override
    public ReferenceValue createReferenceValueForId(String  type,
                                                    Clazz   referencedClass,
                                                    boolean mayBeExtension,
                                                    boolean mayBeNull,
                                                    Object  id)
    {
        return type == null ?
                createReferenceValueNull() :
                new IdentifiedReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, this, id);
    }

    @Override
    public ReferenceValue createReferenceValueForId(String  type,
                                                    Clazz referencedClass,
                                                    boolean mayBeExtension,
                                                    boolean mayBeNull,
                                                    Object  id,
                                                    Object  value)
    {
        return type == null ?
                createReferenceValueNull() :
                new ParticularReferenceValue(type, referencedClass, this, id, value);
    }
}
