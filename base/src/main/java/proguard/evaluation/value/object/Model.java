package proguard.evaluation.value.object;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.MethodSignature;
import proguard.evaluation.value.Value;

/**
 * This interface can be implemented for each class that needs to be modeled during an analysis.
 *
 * <p>The data tracked and logic to handle the methods of the modeled class are
 * implementation-specific.
 *
 * <p>The implementations are expected to override {@link Object#equals(Object)} and {@link
 * Object#hashCode()} to guarantee the expected behavior during the analysis.
 *
 * <p>The interface methods {@link Model#init(MethodSignature, List, Function) init}, {@link
 * Model#invoke(MethodSignature, List, Function) invoke}, and {@link
 * Model#invokeStatic(MethodSignature, List, Function) invokeStatic} are meant to be used to handle
 * the proper method calls on the modeled object. These methods should be implemented with the
 * assumption that the callers are checking for the validity of the invocation parameters.
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
   * @param signature the signature of the invoked constructor.
   * @param parameters the parameters of the call (starting from the first argument, calling
   *     instance not included). The implementations should not worry about checking the validity of
   *     the parameters, so callers should be sure that the passed parameters are supported by the
   *     model.
   * @param valueCalculator a function mapping a result (can be an Object with the result if the
   *     calculated value is the real one or a {@link Model}) to the appropriate {@link Value} used
   *     by the analysis.
   * @return an {@link Optional} containing a {@link Value} containing the constructed object.
   *     Should be empty if the invoked constructor would have thrown an exception with the
   *     specified parameters (should be modified once the analysis supports proper exception
   *     handling for the analyzed code).
   */
  Optional<Value> init(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator);

  /**
   * Execute an instance method on the modeled object. The state of the instance is represented by
   * the state of the model.
   *
   * <p>It is suggested to add logic to allow running this only on a model representing an
   * initialized object.
   *
   * @param signature the signature of the invoked method.
   * @param parameters the parameters of the call (starting from the first argument, calling
   *     instance not included). The implementations should not worry about checking the validity of
   *     the parameters, so callers should be sure that the passed parameters are supported by the
   *     model.
   * @param valueCalculator a function mapping a result (can be an Object with the result if the
   *     calculated value is the real one or a {@link Model}) to the appropriate {@link Value} used
   *     by the analysis.
   * @return an {@link Optional} containing a {@link Value} containing the return value. Should be
   *     empty if the method returns void. Should be empty if the invoked method would have thrown
   *     an exception with the specified parameters (should be modified once the analysis supports
   *     proper exception handling for the analyzed code).
   */
  Optional<Value> invoke(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator);

  /**
   * Execute a static method for the modeled class.
   *
   * <p>It is suggested to add logic to allow running this only on a dummy model without any state.
   *
   * @param signature the signature of the invoked method.
   * @param parameters the parameters of the call. The implementations should not worry about
   *     checking the validity of the parameters, so callers should be sure that the passed
   *     parameters are supported by the model.
   * @param valueCalculator a function mapping a result (can be an Object with the result if the
   *     calculated value is the real one or a {@link Model}) to the appropriate {@link Value} used
   *     by the analysis.
   * @return an {@link Optional} containing a {@link Value} containing the return value. Should be
   *     empty if the method returns void. Should be empty if the invoked method would have thrown
   *     an exception with the specified parameters (should be modified once the analysis supports
   *     proper exception handling for the analyzed code).
   */
  Optional<Value> invokeStatic(
      MethodSignature signature, List<Value> parameters, Function<Object, Value> valueCalculator);
}
