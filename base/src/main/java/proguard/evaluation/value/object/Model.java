package proguard.evaluation.value.object;

import org.jetbrains.annotations.NotNull;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;

/**
 * This interface can be implemented for each class that needs to be modeled during an analysis.
 *
 * <p>The data tracked and logic to handle the methods of the modeled class are
 * implementation-specific.
 *
 * <p>The implementations are expected to override {@link Object#equals(Object)} and {@link
 * Object#hashCode()} to guarantee the expected behavior during the analysis.
 *
 * <p>The interface methods {@link Model#init(MethodExecutionInfo, ValueCalculator) init}, {@link
 * Model#invoke(MethodExecutionInfo, ValueCalculator) invoke}, and {@link
 * Model#invokeStatic(MethodExecutionInfo, ValueCalculator) invokeStatic} are meant to be used to
 * handle the proper method calls on the modeled object.
 */
public interface Model {

  /** Returns the type of the modeled class. */
  @NotNull
  String getType();

  /**
   * Execute a constructor call for the modeled class.
   *
   * <p>It is suggested to add logic to allow running this only on a dummy model without any state.
   *
   * @param methodExecutionInfo execution info of the target method.
   * @param valueCalculator the value calculator that should be used to create any value in the
   *     result.
   * @return the result of the method invocation. Since it's a constructor invocation the return
   *     value of the result is expected to be empty and the constructed value should be set as the
   *     updated instance.
   */
  MethodResult init(MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator);

  /**
   * Execute an instance method on the modeled object. The state of the instance is represented by
   * the state of the model.
   *
   * <p>It is suggested to add logic to allow running this only on a model representing an
   * initialized object.
   *
   * @param methodExecutionInfo execution info of the target method.
   * @param valueCalculator the value calculator that should be used to create any value in the
   *     result.
   * @return the result of the method invocation.
   */
  MethodResult invoke(MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator);

  /**
   * Execute a static method for the modeled class.
   *
   * <p>It is suggested to add logic to allow running this only on a dummy model without any state.
   *
   * @param methodExecutionInfo execution info of the target method.
   * @param valueCalculator the value calculator that should be used to create any value in the
   *     result.
   * @return the result of the method invocation.
   */
  MethodResult invokeStatic(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator);
}
