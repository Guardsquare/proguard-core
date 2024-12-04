package proguard.evaluation.value.object.model.reflective;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.BasicMethodInfo;
import proguard.classfile.ClassConstants;
import proguard.classfile.MethodDescriptor;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.Model;
import proguard.util.PartialEvaluatorUtils;

/** Helper methods to use {@link proguard.evaluation.value.object.model.Model}s. */
public final class ModelHelper {

  private static final Map<Class<? extends Model>, Map<BasicMethodInfo, Method>>
      INSTANCE_METHOD_HANDLERS_CACHE = new HashMap<>();
  private static final Map<Class<? extends Model>, Map<BasicMethodInfo, Method>>
      CONSTRUCTOR_HANDLERS_CACHE = new HashMap<>();
  private static final Map<Class<? extends Model>, Map<BasicMethodInfo, Method>>
      STATIC_HANDLERS_CACHE = new HashMap<>();
  private static final Map<Class<? extends Model>, ReflectiveModel<?>> DUMMY_OBJECTS =
      new HashMap<>();

  private ModelHelper() {}

  /**
   * Given a model class, use reflection to build a mapping from {@link BasicMethodInfo}s used by
   * the analysis to identify a constructor to the {@link Method} handler to the method modeling it,
   * in order to be able to invoke it.
   *
   * <p>The model needs to annotate the methods modeling constructors with {@link
   * ModeledConstructor}.
   *
   * @param modelClass the {@link Model}'s class
   * @return a mapping from a signature to the method modeling the constructor it represents
   */
  public static <T extends ReflectiveModel<T>> Map<BasicMethodInfo, Method> getConstructorHandlers(
      Class<T> modelClass) {
    return CONSTRUCTOR_HANDLERS_CACHE.computeIfAbsent(
        modelClass,
        cls ->
            Arrays.stream(cls.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ModeledConstructor.class))
                .map(ModelHelper::checkModeledMethodNotPublic)
                .collect(
                    Collectors.toMap(
                        method ->
                            createConstructorSignature(
                                method.getAnnotation(ModeledConstructor.class)),
                        method -> method)));
  }

  private static BasicMethodInfo createConstructorSignature(ModeledConstructor annotation) {
    return new BasicMethodInfo(
        ClassConstants.METHOD_NAME_INIT, new MethodDescriptor(annotation.descriptor()));
  }

  /**
   * Given a model class, use reflection to build a mapping from {@link BasicMethodInfo}s used by
   * the analysis to identify an instance method to the {@link Method} handler to the method
   * modeling it, in order to be able to invoke it.
   *
   * <p>The model needs to annotate the methods modeling instance methods with {@link
   * ModeledInstanceMethod}.
   *
   * @param modelClass the {@link Model}'s class
   * @return a mapping from a signature to the method modeling the instance method it represents
   */
  public static <T extends ReflectiveModel<T>>
      Map<BasicMethodInfo, Method> getInstanceMethodHandlers(Class<T> modelClass) {
    return INSTANCE_METHOD_HANDLERS_CACHE.computeIfAbsent(
        modelClass,
        cls ->
            Arrays.stream(cls.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ModeledInstanceMethod.class))
                .map(ModelHelper::checkModeledMethodNotPublic)
                .collect(
                    Collectors.toMap(
                        method ->
                            createInstanceMethodSignature(
                                method.getAnnotation(ModeledInstanceMethod.class)),
                        method -> method)));
  }

  private static BasicMethodInfo createInstanceMethodSignature(ModeledInstanceMethod annotation) {
    return new BasicMethodInfo(annotation.name(), new MethodDescriptor(annotation.descriptor()));
  }

  /**
   * Given a model class, use reflection to build a mapping from {@link BasicMethodInfo}s used by
   * the analysis to identify an instance method to the {@link Method} handler to the method
   * modeling it, in order to be able to invoke it.
   *
   * <p>The model needs to annotate the methods modeling instance methods with {@link
   * ModeledInstanceMethod}.
   *
   * @param modelClass the {@link Model}'s class
   * @return a mapping from a signature to the method modeling the static method it represents
   */
  public static <T extends ReflectiveModel<T>> Map<BasicMethodInfo, Method> getStaticMethodHandlers(
      Class<T> modelClass) {
    return STATIC_HANDLERS_CACHE.computeIfAbsent(
        modelClass,
        cls ->
            Arrays.stream(cls.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ModeledStaticMethod.class))
                .map(ModelHelper::checkModeledMethodNotPublic)
                .collect(
                    Collectors.toMap(
                        method ->
                            createStaticMethodSignature(
                                method.getAnnotation(ModeledStaticMethod.class)),
                        method -> method)));
  }

  private static BasicMethodInfo createStaticMethodSignature(ModeledStaticMethod annotation) {
    return new BasicMethodInfo(annotation.name(), new MethodDescriptor(annotation.descriptor()));
  }

  private static Method checkModeledMethodNotPublic(Method method) {
    if (Modifier.isPublic(method.getModifiers())) {
      throw new IllegalStateException(
          String.format("Modeling methods should not be public, but '%s' is", method));
    }
    return method;
  }

  public static <T extends ReflectiveModel<T>> T getDummyObject(Class<T> modelClass) {
    return (T)
        DUMMY_OBJECTS.computeIfAbsent(
            modelClass,
            cls -> {
              try {
                Constructor<T> constructor = modelClass.getDeclaredConstructor();
                // it is likely desirable to keep the constructor private, so we just force
                // our way there with reflection ignoring the accessibility modifiers
                constructor.setAccessible(true); // NOSONAR
                return constructor.newInstance();
              } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                    "The model "
                        + modelClass.getName()
                        + " does not implement the mandatory no-argument constructor.");
              } catch (InvocationTargetException e) {
                throw new IllegalStateException(
                    "The model "
                        + modelClass.getName()
                        + " does not correctly implement the mandatory no-argument constructor.",
                    e);
              } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(
                    "Failed to instantiate the "
                        + modelClass.getName()
                        + " using the mandatory no-argument constructor.",
                    e);
              }
            });
  }

  public static <T extends ReflectiveModel<T>> Collection<BasicMethodInfo> getSupportedMethods(
      Class<T> modelClass) {
    return Stream.of(
            ModelHelper.getStaticMethodHandlers(modelClass),
            ModelHelper.getConstructorHandlers(modelClass),
            ModelHelper.getInstanceMethodHandlers(modelClass))
        .flatMap(methodInfoMethodMap -> methodInfoMethodMap.keySet().stream())
        .collect(Collectors.toList());
  }

  /**
   * Simple helper method to check if all values in the given list are particular.
   *
   * @param values Values to check.
   * @return Whether all of the values are particular.
   */
  public static boolean allParticular(List<Value> values) {
    return values.stream().allMatch(Value::isParticular);
  }

  /**
   * Check whether both the instance (for instance methods) and all parameters of a method call are
   * particular.
   *
   * @param executionInfo information on a method execution.
   * @return whether both the instance and the parameters are particular.
   */
  public static boolean areInstanceAndParametersParticular(MethodExecutionInfo executionInfo) {
    List<Value> allArguemnts = new ArrayList<>();
    if (executionInfo.isInstanceMethod()) {
      allArguemnts.add(executionInfo.getInstanceNonStatic());
    }
    allArguemnts.addAll(executionInfo.getParameters());
    return allParticular(allArguemnts);
  }

  /**
   * Utility method to execute a modeled method on a model using the provided Map of supported
   * handlers.
   *
   * @param context The method execution context.
   * @param handlers Map of {@link BasicMethodInfo} to {@link Method}s which are supported.
   * @param instance Instance to execute the method on.
   * @return The result of the method execution.
   * @throws UnsupportedOperationException If a method without handler is requested.
   * @throws RuntimeException For any exception during reflective execution of the method.
   */
  public static MethodResult executeViaHandler(
      MethodExecutionContext context, Map<BasicMethodInfo, Method> handlers, Model instance) {

    MethodExecutionInfo executionInfo = context.executionInfo;
    BasicMethodInfo methodInfo = new BasicMethodInfo(executionInfo.getSignature());

    if (!handlers.containsKey(methodInfo)) {
      throw new UnsupportedOperationException(
          String.format("Unsupported method %s", executionInfo));
    }

    try {
      List<Object> modeledMethodParameters = new ArrayList<>();
      modeledMethodParameters.add(context);
      modeledMethodParameters.addAll(executionInfo.getParameters());
      Method method = handlers.get(methodInfo);
      method.setAccessible(true); // NOSONAR
      return (MethodResult) method.invoke(instance, modeledMethodParameters.toArray());
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Exception while trying execute method handler", e);
    }
  }

  /**
   * Helper to create a value from a standard modeled constructor call. This should be used for
   * constructor where all of these are true:
   *
   * <ul>
   *   <li>Create a known (particular) value
   *   <li>Have no side effects besides constructing the object
   * </ul>
   *
   * @param context The method execution context.
   * @param concreteValue The value of the constructed object.
   * @return The result containing the updated instance.
   */
  public static MethodResult createDefaultConstructorResult(
      MethodExecutionContext context, Model concreteValue) {
    ValueCalculator valueCalculator = context.getValueCalculator();
    MethodExecutionInfo executionInfo = context.getExecutionInfo();

    ReferenceValue constructedValue =
        valueCalculator
            .apply(
                executionInfo.getTargetType(),
                executionInfo.getTargetClass(),
                true,
                concreteValue,
                false,
                executionInfo.getSpecificInstance().id)
            .referenceValue();

    return new MethodResult.Builder().setUpdatedInstance(constructedValue).build();
  }

  /**
   * Helper to create a value from a standard modeled method call. This should be used for calls
   * where all of these are true:
   *
   * <ul>
   *   <li>Return a known (particular) value
   *   <li>Is a pure function (i.e. no side-effects, just returns a value)
   * </ul>
   *
   * @param context The method execution context.
   * @param concreteValue The value of the returned object, can be precise or a {@link Model}.
   * @return The result containing the return value.
   */
  public static MethodResult createDefaultReturnResult(
      MethodExecutionContext context, @Nullable Object concreteValue) {
    ValueCalculator valueCalculator = context.getValueCalculator();
    MethodExecutionInfo executionInfo = context.getExecutionInfo();

    Value returnValue =
        valueCalculator.apply(
            executionInfo.getReturnType(),
            executionInfo.getReturnClass(),
            true,
            concreteValue,
            false,
            null);

    return new MethodResult.Builder().setReturnValue(returnValue).build();
  }

  /**
   * Helper to create a method result containing "this", the instance a method has been called on.
   * This should be used for calls where all of these are true:
   *
   * <ul>
   *   <li>Returns the instance the method was invoked on
   *   <li>Have no side effects besides the modification of the instance
   * </ul>
   *
   * <p>Usually, this would be used on classes with a fluent interface, e.g. setters when using the
   * Builder design pattern:
   *
   * <pre>{@code Builder setValue(value) {
   *     this.value = value; // modify instance
   *     return this;
   * }}</pre>
   *
   * @param context The method execution context.
   * @param newInstance The new instance, can be precise or a {@link Model}.
   * @return The result containing the return value.
   */
  public static MethodResult createDefaultBuilderResult(
      MethodExecutionContext context, Object newInstance) {
    ValueCalculator valueCalculator = context.getValueCalculator();
    MethodExecutionInfo executionInfo = context.getExecutionInfo();

    ReferenceValue instance = executionInfo.getInstanceNonStatic();
    // If the instance id is unknown we assign a new id
    Object id =
        instance.isSpecific()
            ? PartialEvaluatorUtils.getIdFromSpecificReferenceValue(instance)
            : null;

    Value returnValue =
        valueCalculator.apply(
            executionInfo.getReturnType(),
            executionInfo.getReturnClass(),
            true,
            newInstance,
            false,
            id);

    return new MethodResult.Builder()
        .setReturnValue(returnValue)
        .setUpdatedInstance(returnValue.referenceValue())
        .build();
  }

  /**
   * Helper to create a method result containing "this" with unknown value. The helper should be
   * used only for calls where all of these are true:
   *
   * <ul>
   *   <li>Returns the instance the method was invoked on, the instance value might or might not be
   *       already known
   *   <li>Have no other side effects
   *   <li>The value of the instance is now unknown
   * </ul>
   *
   * <p>Usually, this would be used on classes with a fluent interface, e.g. setters when using the
   * Builder design pattern:
   *
   * <pre>{@code Builder setValue(value) {
   *     this.value = value; // modify instance
   *     return this;
   * }}</pre>
   *
   * @param context The method execution context.
   * @return The result containing the return value.
   */
  public static MethodResult createUnknownBuilderResult(MethodExecutionContext context) {
    ValueCalculator valueCalculator = context.getValueCalculator();
    MethodExecutionInfo executionInfo = context.getExecutionInfo();

    // If the instance id is not available there is nothing to update
    if (!executionInfo.getInstanceNonStatic().isSpecific()) {
      return MethodResult.invalidResult();
    }

    Value returnValue =
        valueCalculator.apply(
            executionInfo.getReturnType(),
            executionInfo.getReturnClass(),
            false,
            null,
            false,
            context.executionInfo.getSpecificInstance().id);

    return new MethodResult.Builder()
        .setReturnValue(returnValue)
        .setUpdatedInstance(returnValue.referenceValue())
        .build();
  }

  /**
   * A helper class grouping {@link MethodExecutionInfo} and {@link ValueCalculator} as the
   * execution context of a method.
   */
  public static final class MethodExecutionContext {
    private final MethodExecutionInfo executionInfo;
    private final ValueCalculator valueCalculator;

    public MethodExecutionContext(
        MethodExecutionInfo executionInfo, ValueCalculator valueCalculator) {
      this.executionInfo = executionInfo;
      this.valueCalculator = valueCalculator;
    }

    public MethodExecutionInfo getExecutionInfo() {
      return executionInfo;
    }

    public ValueCalculator getValueCalculator() {
      return valueCalculator;
    }

    @Override
    public String toString() {
      return "MethodExecutionContext["
          + "executionInfo="
          + executionInfo
          + ", "
          + "valueCalculator="
          + valueCalculator
          + ']';
    }
  }
}
