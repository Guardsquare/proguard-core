package proguard.evaluation;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;

/**
 * A class modeling the results of a method invocation. This includes the returned value and whether
 * any side effect happened either on the calling instance or one of the arguments.
 *
 * <p>In case of side effects the new value should have a reference identifier corresponding to the
 * original argument. If the analysis supports it, this information can be used to replace all the
 * values with the same reference value with the updated one.
 */
public class MethodResult {

  private static final MethodResult INVALID_RESULT = new MethodResult();

  private final @Nullable Value returnValue;
  private final boolean isReturnValuePresent;
  private final @Nullable ReferenceValue updatedInstance;
  private final @Nullable List<Value> updatedParameters;

  private MethodResult() {
    this(null, false, null, null);
  }

  private MethodResult(
      @Nullable Value returnValue,
      boolean isReturnValuePresent,
      @Nullable ReferenceValue updatedInstance,
      @Nullable List<Value> updatedParameters) {
    this.returnValue = returnValue;
    this.isReturnValuePresent = isReturnValuePresent;
    this.updatedInstance = updatedInstance;
    this.updatedParameters = updatedParameters;
  }

  /**
   * Returns a result communicating to the receiver that the creator is not able to provide any
   * additional information about the method execution.
   *
   * <p>This should be the only way to communicate this type of information, while any other result
   * with all empty parameters means a method returning void and with no side effects.
   *
   * @return an invalid result.
   */
  public static MethodResult invalidResult() {
    return INVALID_RESULT;
  }

  /**
   * Returns whether the result is invalid (i.e., whether it was created via {@link
   * MethodResult#invalidResult()}).
   *
   * @return whether the result is invalid.
   */
  public boolean isResultValid() {
    return this != INVALID_RESULT;
  }

  /**
   * Whether the result provides a return value. We need to specify this since null is a valid
   * return value.
   *
   * @return whether the result provides a return value.
   */
  public boolean isReturnValuePresent() {
    return isReturnValuePresent;
  }

  /**
   * Whether the calling instance was updated during method execution.
   *
   * @return whether the calling instance was updated during method execution.
   */
  public boolean isInstanceUpdated() {
    return updatedInstance != null;
  }

  /**
   * Whether any parameter was updated during method execution.
   *
   * @return whether any parameter was updated during method execution.
   */
  public boolean isAnyParameterUpdated() {
    return updatedParameters != null;
  }

  /**
   * The return value of the method invocation if {@link MethodResult#isReturnValuePresent}, throws
   * otherwise.
   *
   * @return the return value of the method invocation.
   */
  public @Nullable Value getReturnValue() {
    if (!isReturnValuePresent()) {
      throw new IllegalStateException(
          "Should not try to retrieve an invalid return value, check 'isReturnValueValid()' first");
    }
    return returnValue;
  }

  /**
   * The updated instance value after a method invocation if {@link
   * MethodResult#isInstanceUpdated()}, throws otherwise.
   *
   * <p>The identifier of the returned value can be matched to identify the old values using the
   * same reference.
   *
   * @return the updated instance value after a method invocation.
   */
  public @Nullable ReferenceValue getUpdatedInstance() {
    if (!isInstanceUpdated()) {
      throw new IllegalStateException(
          "Should not try to retrieve the updated instance value if the instance was not updated");
    }
    return updatedInstance;
  }

  /**
   * The updated parameter value after a method invocation if {@link
   * MethodResult#isAnyParameterUpdated()}, throws otherwise.
   *
   * <p>Each element of the list (corresponding to the parameter position, instance not included and
   * the first parameter is always element 0) contains either null, if the specific parameter was
   * not updated, or the updated parameter value.
   *
   * <p>The identifier of the returned values can be matched to identify the old values using the
   * same reference.
   *
   * @return the updated parameters value after a method invocation.
   */
  public @Nullable List<Value> getUpdatedParameters() {
    if (!isAnyParameterUpdated()) {
      throw new IllegalStateException(
          "Should not try to retrieve the updated parameters if no parameter was updated");
    }
    return updatedParameters;
  }

  /**
   * A builder for {@link MethodResult}. A value should be set only if the creator is able to
   * provide that information.
   *
   * <p>{@link MethodResult.Builder#setReturnValue(Value)} should not be called if the method
   * returns void or if no return value can be provided (if the method execution fails {@link
   * MethodResult#invalidResult()} should be used instead of the builder). Similarly {@link
   * MethodResult.Builder#setUpdatedInstance(ReferenceValue)} and {@link
   * MethodResult.Builder#setUpdatedParameters(List)} being used means that there really were side
   * effects on respectively instance or parameters during the analyzed method invocation; this
   * methods should not be used if no side effects happen.
   */
  public static class Builder {
    private @Nullable Value returnValue;
    private boolean isReturnValuePresent;
    private @Nullable ReferenceValue updatedInstance;
    private @Nullable List<Value> updatedParameters;

    /**
     * Sets the value returned by the analyzed method.
     *
     * <p>Should not be invoked for methods returning void.
     *
     * @param returnValue the return value of an analyzed method.
     * @return the builder.
     */
    public Builder setReturnValue(@Nullable Value returnValue) {
      if (isReturnValuePresent) {
        throw new IllegalStateException("The return value should just be set once");
      }
      isReturnValuePresent = true;
      this.returnValue = returnValue;
      return this;
    }

    /**
     * Set the updated value of the invocation instance after the invocation of the analyzed method.
     *
     * <p>Should not be invoked if there were no side effects on the calling instance during the
     * method execution.
     *
     * <p>Should always be used for setting the result of a constructor's invocation.
     *
     * <p>The caller should make sure the updated instance to have the same reference identifier of
     * the old instance.
     *
     * @param updatedInstance the instance updated during the analyzed method's invocation.
     * @return the builder.
     */
    public Builder setUpdatedInstance(@NotNull ReferenceValue updatedInstance) {
      if (this.updatedInstance != null) {
        throw new IllegalStateException("The updated instance value should just be set once");
      }
      Objects.requireNonNull(updatedInstance);
      this.updatedInstance = updatedInstance;
      return this;
    }

    /**
     * Set the updated value of the parameters after the invocation of the analyzed method.
     *
     * <p>Should not be invoked if there were no side effects on the parameters during the method
     * execution.
     *
     * <p>The caller should make sure the updated parameters to have the same reference identifier
     * of the old ones.
     *
     * @param updatedParameters the parameters updated during the analyzed method's invocation. The
     *     size of the list should be the same as the number of parameters of the analyzed method
     *     excluded the calling instance (the first parameter is always in position 0 for both
     *     instance and static methods). Each parameter should be null if it was not updated, the
     *     updated value otherwise.
     * @return the builder.
     */
    public Builder setUpdatedParameters(List<Value> updatedParameters) {
      if (this.updatedParameters != null) {
        throw new IllegalStateException("The updated parameter values should just be set once");
      }
      Objects.requireNonNull(updatedParameters);
      this.updatedParameters = updatedParameters;
      return this;
    }

    /**
     * Builds a {@link MethodResult} given the set parameters.
     *
     * @return the new {@link MethodResult}.
     */
    public MethodResult build() {
      return new MethodResult(
          returnValue, isReturnValuePresent, updatedInstance, updatedParameters);
    }
  }
}
