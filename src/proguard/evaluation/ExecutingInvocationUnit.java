package proguard.evaluation;

import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUFFER;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUILDER;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.TypeConstants;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.ReflectiveMethodCallUtil;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

/**
 * <p>This {@link ExecutingInvocationUnit} reflectively executes the method calls it visits on a given
 * {@link ParticularReferenceValue}.</p>
 *
 * <p>After the (reflective) execution of method, it also takes care to replace values on stack/variables
 * if needed. This needs to be done, as the PartialEvaluator treats all entries on stack/variables as
 * immutable, and assumes that every change creates a new object.</p>
 *
 * <p>Before a method call, the stack/variables can contain multiple references to the same object. If the
 * method call then creates a new Variable (representing an updated object), we need to update all
 * references to this new Variable.</p>
 *
 * <p>There are some methods which always return a new object when the method is actually executed in the
 * JVM (e.g. StringBuilder.toString). For such method calls, we never update the stack/variables
 * (Blacklist, Case 1)</p>
 *
 * <p>For certain methods (e.g. the Constructor), we always need to replace the value on stack/variables
 * (Whitelist, Case 3)</p>
 *
 * <p>In all other cases, we assume the underlying object was changed if the returned value is of the same
 * type as the called upon instance. This is an approximation, which works well for Strings, StringBuffer,
 * StringBuilder (Approximation, Case 3)</p>
 *
 * @author Dennis Titze
 */
public class ExecutingInvocationUnit
    extends BasicInvocationUnit
{

    public static boolean DEBUG = System.getProperty("eiu") != null;

    // Used to store the parameters for one method call.
    private Value[] parameters;

    public ExecutingInvocationUnit(ValueFactory valueFactory)
    {
        super(valueFactory);
    }

    // Overrides for BasicInvocationUnit

    @Override
    public void setMethodParameterValue(Clazz                clazz,
                                        AnyMethodrefConstant anyMethodrefConstant,
                                        int                  parameterIndex,
                                        Value                value)
    {
        if (parameters == null)
        {
            // This is the first invocation of setMethodParameterValue for this method.
            // Initialize array
            String type = anyMethodrefConstant.getType(clazz);
            int parameterCount = ClassUtil.internalMethodParameterCount(type, isStatic);
            parameters = new Value[parameterCount];
        }
        parameters[parameterIndex] = value;
    }

    @Override
    public boolean methodMayHaveSideEffects(Clazz clazz,
                                            AnyMethodrefConstant anyMethodrefConstant,
                                            String returnType)
    {
        // Only execute methods which have at least one parameter.
        // If the method is a static method, this means that at least one parameter is set,
        // if the method is an instance call, at least the instance needs to be set.
        // Static calls without parameters will not be called, as the side-effects of those cannot currently be tracked.
        return parameters != null && parameters.length > 0;
    }

    @Override
    public Value getMethodReturnValue(Clazz                clazz,
                                      AnyMethodrefConstant anyMethodrefConstant,
                                      String               returnType)
    {

        String baseClassName = anyMethodrefConstant.getClassName(clazz);

        if (!isSupportedMethodCall(baseClassName))
        {
            return valueFactory.createValue(returnType, clazz, true, true);
        }

        Value reflectedReturnValue = handleMethodCall(clazz, anyMethodrefConstant, returnType, parameters, isStatic);

        updateStackAndVariables(clazz, anyMethodrefConstant, returnType, reflectedReturnValue);

        if (returnType.charAt(0) == TypeConstants.VOID)
        {
            // For a void-method, null is expected as return value.
            return null;
        }
        else if (reflectedReturnValue == null)
        {
            // The reflective call failed. Create a simple new Value of the correct type.
            return valueFactory.createValue(returnType, clazz, true, true);
        }
        else
        {
            return reflectedReturnValue;
        }
    }


    @Override
    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        super.visitAnyMethodrefConstant(clazz, anyMethodrefConstant);
        this.parameters = null;
    }

    // private methods

    /**
     * Handles a method call, by reflectively trying to call it with the given parameters.
     *
     * @param clazz the class this call is contained in.
     * @param anyMethodrefConstant the method to be called
     * @param returnType the return type.
     * @param parameter an array containing the parameter values (for a non-static call, parameter[0] is the instance.
     * @param isStatic is this a static method call.
     * @return the return value of the method call. Even for a void method, a created value might be returned, which might need to be replaced on stack/values (e.g. for Constructors).
     */
    private Value handleMethodCall(Clazz clazz,
                                   AnyMethodrefConstant anyMethodrefConstant,
                                   String returnType,
                                   Value[] parameter,
                                   boolean isStatic)
    {

        String baseClassName = anyMethodrefConstant.getClassName(clazz);
        String methodName = anyMethodrefConstant.getName(clazz);
        String parameterType = anyMethodrefConstant.getType(clazz);

        if (!isStatic)
        {
            if (!parameter[0].isSpecific()) // instance must be at least specific.
            {
                return null;
            }
        }

        // if this is a non-static call, the first parameter is the instance, so all accesses to the arrays need to be offset by 1.
        // if this is a static call, the first parameter is actually the first.
        int paramOffset = (isStatic ? 0 : 1);

        // check if any of the parameters is Unknown. If yes, the result is unknown as well.
        for (int i = paramOffset; i < parameter.length; i++)
        {
            if (!parameter[i].isParticular())
            {
                return null;
            }
        }

        boolean resultMayBeNull = true;
        boolean resultMayBeExtension = true;
        Object methodResult;

        try
        {
            // collect all class types of the parameters (to search for the correct method reflectively).
            Class<?>[] parameterClasses = ReflectiveMethodCallUtil.stringtypesToClasses(parameterType);

            // collect all objects of the parameters.
            Object[] parameterObjects = new Object[parameter.length - paramOffset];
            for (int i = paramOffset; i < parameter.length; i++)
            {
                parameterObjects[i - paramOffset] = ReflectiveMethodCallUtil.getObjectForValue(parameter[i], parameterClasses[i - paramOffset]);
            }

            if (methodName.equals(ClassConstants.METHOD_NAME_INIT)) //CTOR
            {
                methodResult = ReflectiveMethodCallUtil.callConstructor(baseClassName.replace('/', '.'),
                                                                        parameterClasses,
                                                                        parameterObjects);


                resultMayBeNull = false; // the return of the ctor will not be null if it does not throw.
                resultMayBeExtension = false; // the return of the ctor will always be this specific type.
            }
            else // non-constructor method call.
            {
                String className;
                Object callingInstance = null; // correct for static.
                if (isStatic)
                {
                    className = baseClassName.replace('/', '.');
                }
                else
                {
                    ReferenceValue instance = parameter[0].referenceValue();
                    className = ClassUtil.externalClassName(ClassUtil.internalClassNameFromClassType(instance.getType()));

                    switch (baseClassName)
                    {
                        // create a copy of the object stored in the ParticularReferenceValue.
                        case NAME_JAVA_LANG_STRING_BUILDER:
                            callingInstance = new StringBuilder((StringBuilder) instance.value());
                            break;
                        case NAME_JAVA_LANG_STRING_BUFFER:
                            callingInstance = new StringBuffer((StringBuffer) instance.value());
                            break;
                        case NAME_JAVA_LANG_STRING:
                            callingInstance = (String) instance.value();
                            break;
                    }
                }
                methodResult = ReflectiveMethodCallUtil.callMethod(className, methodName, callingInstance, parameterClasses, parameterObjects);
            }
        }
        catch (NullPointerException | InvocationTargetException e)
        {
            // a parameter might be null, and the method might not be able to handle it -> NullPointerException, or
            // an index might be wrongly / not initialized -> StringIndexOutOfBoundsException.
            if (DEBUG)
            {
                System.err.println("Invocation exception during method execution: " + e.getCause().getClass().getSimpleName() + ": - " + e.getCause().getMessage());
            }
            return null;
        }
        catch (RuntimeException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException e)
        {
            if (DEBUG)
            {
                System.err.println("Error reflectively calling " + baseClassName + "." + methodName + parameterType + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
            return null;
        }

        // If the return value is a primitive, store this inside its corresponding ParticularValue, e.g. int -> ParticularIntegerValue.
        if (returnType.length() == 1 && ClassUtil.isInternalPrimitiveType(returnType))
        {
            return createPrimitiveValue(methodResult, returnType);
        }

        // If it is not a primitiveValue, it will be stored in a ParticularReferenceValue.
        // If the return type is void, we still return an object, since this might need to be replaced on stack/variables.
        // To create a referenceValue with a correct type (i.e. not void), we update the type from the instance we called upon.
        if (!isStatic && returnType.equals("V"))
        {
            returnType = parameter[0].referenceValue().getType();
        }
        return valueFactory.createReferenceValue(ClassUtil.internalClassNameFromClassType(returnType), clazz, resultMayBeExtension, resultMayBeNull, methodResult);

    }


    /**
     * Create a ParticularValue containing the object, using the factory of this instance
     *
     * @param obj the object to wrap.
     * @param type the type the resulting Value should have.
     * @return the new ParticularValue, or null if the type cannot be converted to a ParticularValue.
     */
    private Value createPrimitiveValue(Object obj, String type)
    {
        switch (type.charAt(0))
        {
            case TypeConstants.BOOLEAN:
                return valueFactory.createIntegerValue(((Boolean) obj) ? 1 : 0);
            case TypeConstants.CHAR:
                return valueFactory.createIntegerValue((Character) obj);
            case TypeConstants.BYTE:
                return valueFactory.createIntegerValue((Byte) obj);
            case TypeConstants.SHORT:
                return valueFactory.createIntegerValue((Short) obj);
            case TypeConstants.INT:
                return valueFactory.createIntegerValue((Integer) obj);
            case TypeConstants.FLOAT:
                return valueFactory.createFloatValue((Float) obj);
            case TypeConstants.DOUBLE:
                return valueFactory.createDoubleValue((Double) obj);
            case TypeConstants.LONG:
                return valueFactory.createLongValue((Long) obj);
        }
        // unknown type
        return null;
    }

    private boolean isSupportedMethodCall(String baseClassName)
    {
        switch (baseClassName)
        {
            case ClassConstants.NAME_JAVA_LANG_STRING_BUILDER:
            case ClassConstants.NAME_JAVA_LANG_STRING_BUFFER:
            case ClassConstants.NAME_JAVA_LANG_STRING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Iterate the variables and replace all occurrences of oldValue with newValue.
     *
     * @param newValue  the value which should be put in.
     * @param oldValue  the value to replace.
     * @param variables the variables to look through.
     */
    private void replaceReferenceInVariables(Value newValue,
                                             Value oldValue,
                                             Variables variables)
    {
        if (oldValue.isSpecific())
        {
            // we need to replace all instances on the stack and in the vars with new instances now.
            if (variables != null)
            {
                for (int i = 0; i < variables.size(); i++)
                {
                    Value value = variables.getValue(i);
                    if (Objects.equals(value, oldValue))
                    {
                        variables.store(i, newValue);
                    }
                    if (value != null && value.isCategory2())
                    {
                        i++;
                    }
                }
            }
        }
    }

    /**
     * Iterate the stack and replace all occurrences of oldValue with newValue.
     *
     * @param newValue the value which should be put in.
     * @param oldValue the value to replace.
     * @param stack    the stack to look through.
     */
    private void replaceReferenceOnStack(Value newValue, Value oldValue, Stack stack)
    {
        if (oldValue.isSpecific())
        {
            for (int i = 0; i < stack.size(); i++)
            {
                Value top = stack.getTop(i);
                if (Objects.equals(top, oldValue))
                {
                    stack.setTop(i, newValue);
                }
            }
        }
    }


    /**
     * Returns true, if a call to clazzName.methodName should return a new (i.e. not-linked) instance.
     *
     * @param clazzName full class name of the base class of the method (e.g. java/lang/StringBuilder).
     * @param methodName method name.
     * @return if it should create a new instance.
     */
    private static boolean alwaysReturnsNewInstance(String clazzName, String methodName)
    {
        switch (clazzName)
        {
            case NAME_JAVA_LANG_STRING_BUILDER:
                if ("toString".equals(methodName))
                {
                    return true;
                }
            case NAME_JAVA_LANG_STRING_BUFFER:
                if ("toString".equals(methodName))
                {
                    return true;
                }
            case NAME_JAVA_LANG_STRING:
                if ("valueOf".equals(methodName))
                {
                    return true;
                }
        }
        return false;
    }

    /**
     * Returns true, if a call to clazzName.methodName always modified the called upon instance.
     * The modified value will be returned by the reflective method call
     *
     * @param clazzName full class name of the base class of the method (e.g. java/lang/StringBuilder).
     * @param methodName method name.
     * @return if the instance is always modified.
     */
    private static boolean alwaysModifiesInstance(String clazzName, String methodName)
    {
        if (methodName.equals(ClassConstants.METHOD_NAME_INIT))
        {
            // The constructor always modifies the instance.
            return true;
        }
        return false;
    }

    /**
     * Determines if the stack/variables need to be updated and initiates the updating.
     *
     * Limitations:
     * We are only checking if the instance of the call was modified, i.e.
     * - for static calls, no value will be replaced on stack/variables (since no instance exists), and
     * - the method call assumes that the parameters are not changed.
     *
     * This is an approximation which works well for StringBuilder/StringBuffer and Strings.
     */
    private void updateStackAndVariables(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String returnType, Value reflectedReturnValue)
    {
        String baseClassName = anyMethodrefConstant.getClassName(clazz);
        String methodName = anyMethodrefConstant.getName(clazz);

        if (isStatic)
        {
            // For a static method, there is never an instance to update,
            // we do not track internal state, so no static internals of the class are stored.
            return;
        }

        if (alwaysReturnsNewInstance(baseClassName, methodName))
        {
            // Blacklist (Case 1).
            // The value returned by the PartialEvaluator never needs to be replaced.
            return;
        }

        if (alwaysModifiesInstance(baseClassName, methodName) || // Whitelist (Case 2). The instance always needs to be replaced.
            Objects.equals(returnType, parameters[0].referenceValue().getType())) // Approximation (Case 3)
            // For now, we assume that the instance is changed, if the method is not in the Blacklist, and the returnType equals
            // the type of the called upon instance. E.g., if StringBuilder.append() returns a StringBuilder, we assume that this
            // is the instance which needs to be replace.
        {
            Value updateValue = reflectedReturnValue;

            // If the reflective method call fails, it returned null. We create an unknown value to use as replacement on stack/variables.
            // this can happen e.g. if we have a substring with incorrect lengths, or unknown values in the parameters, etc.
            if (updateValue == null)
            {
                // To create a correct (failed) object, we need to know the type. For void methods, we use the type of the instance
                updateValue = valueFactory.createValue((returnType.charAt(0) == TypeConstants.VOID) ?
                                                           ClassUtil.internalTypeFromClassName(baseClassName) :
                                                           returnType,
                                                       clazz,
                                                       true,
                                                       true);
            }

            replaceReferenceInVariables(updateValue, parameters[0], variables);
            replaceReferenceOnStack(updateValue, parameters[0], stack);
        }
    }

}
