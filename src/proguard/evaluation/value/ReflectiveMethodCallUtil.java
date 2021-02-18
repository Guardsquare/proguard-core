/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.evaluation.value;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import proguard.classfile.TypeConstants;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InternalTypeEnumeration;

/**
 * This {@link ReflectiveMethodCallUtil} can execute a method call on a given class reflectively.
 * If the call fails, a RuntimeException is thrown, otherwise, a new Value is returned using the
 * given factory.
 *
 * @author Dennis Titze
 */
public class ReflectiveMethodCallUtil
{

    /**
     * Reflectively converts a method descriptor to a list of Classes representing this String.
     *
     * @throws ClassNotFoundException if one of the referenced classes (as string) is not part of the current class pool.
     */
    public static Class<?>[] stringtypesToClasses(String descriptor) throws ClassNotFoundException
    {
        InternalTypeEnumeration typeEnum = new InternalTypeEnumeration(descriptor);
        Class<?>[] parameterClasses = new Class[typeEnum.typeCount()];
        int i = 0;
        while (typeEnum.hasMoreTypes())
        {
            String internalType = typeEnum.nextType();
            Class<?> c;
            // is this parameter a class,
            if (!ClassUtil.isInternalArrayType(internalType) && ClassUtil.isInternalClassType(internalType))
            {
                c = Class.forName(ClassUtil.externalClassName(ClassUtil.internalClassNameFromClassType(internalType)));
            }
            // or a primitive,
            else if (internalType.length() == 1 && ClassUtil.isInternalPrimitiveType(internalType))
            {
                c = ReflectiveMethodCallUtil.getClassForPrimitive(internalType.charAt(0));
            }
            // or an array (not supported yet).
            else
            {
                // System.err.println("Array types not supported as parameters yet");
                return null;
            }
            parameterClasses[i++] = c;
        }
        return parameterClasses;
    }

    /**
     * Extract the Object from the given Value.
     *
     * @param value the value holding the object.
     * @param expectedType the class the returned object should be.
     * @return an object extracted from the value, or null if it could not be extracted.
     */

    public static Object getObjectForValue(Value value, Class<?> expectedType)
    {
        Object ret = null;
        switch (value.computationalType())
        {
            case Value.TYPE_INTEGER:
                ret = value.integerValue().value();
                // box into the correct type.
                if (expectedType == Character.class || expectedType == char.class)
                {
                    ret = (char) ((Integer) ret).intValue();
                }
                else if (expectedType == Byte.class || expectedType == byte.class)
                {
                    ret = (byte) ((Integer) ret).intValue();
                }
                else if (expectedType == Short.class || expectedType == short.class)
                {
                    ret = (short) ((Integer) ret).intValue();
                }
                else if (expectedType == Boolean.class || expectedType == boolean.class)
                {
                    ret = ((int) ret == 1);
                }
                break;
            case Value.TYPE_LONG:
                ret = value.longValue().value();
                break;
            case Value.TYPE_FLOAT:
                ret = value.floatValue().value();
                break;
            case Value.TYPE_DOUBLE:
                ret = value.doubleValue().value();
                break;
            case Value.TYPE_REFERENCE:
                ret = value.referenceValue().value();
                break;
        }

        return ret;
    }

    /**
     * Returns the Class for the given primitive type.
     */
    public static Class<?> getClassForPrimitive(char internalPrimitiveType)
    {
        switch (internalPrimitiveType)
        {
            case TypeConstants.VOID:
                return void.class;
            case TypeConstants.BOOLEAN:
                return boolean.class;
            case TypeConstants.BYTE:
                return byte.class;
            case TypeConstants.CHAR:
                return char.class;
            case TypeConstants.SHORT:
                return short.class;
            case TypeConstants.INT:
                return int.class;
            case TypeConstants.LONG:
                return long.class;
            case TypeConstants.FLOAT:
                return float.class;
            case TypeConstants.DOUBLE:
                return double.class;
            default:
                throw new IllegalArgumentException("Unexpected primitive type [" + internalPrimitiveType + "]");
        }
    }

    /**
     * Reflectively call the constructor of className with the given parameters.
     */
    public static Object callConstructor(String className,
                                         Class<?>[] parameterClasses,
                                         Object[] parameterObjects)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class<?> baseClass = Class.forName(className);
        Constructor<?> constructor = baseClass.getConstructor(parameterClasses);
        return constructor.newInstance(parameterObjects);
    }

    /**
     * Reflectively call the method on the given instance. Instance can be null for static methods.
     */
    public static Object callMethod(String className,
                                    String methodName,
                                    Object instance,
                                    Class<?>[] parameterClasses,
                                    Object[] parameterObjects)
        throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException
    {
        Class<?> baseClass = Class.forName(className);
        Method method = baseClass.getMethod(methodName, parameterClasses);
        return method.invoke(instance, parameterObjects);
    }
}
