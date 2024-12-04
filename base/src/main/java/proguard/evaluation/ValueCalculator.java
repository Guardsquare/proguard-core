package proguard.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.Model;

/**
 * This functional interface can be used to specify a way of creating {@link Value}s given a series
 * of parameters.
 *
 * <p>For now this is split from {@link proguard.evaluation.value.ValueFactory} and the function can
 * be implemented in a way that all the calls to the specific value factory methods go through the
 * same function. It could be argued that instead of having a functional interface this could be a
 * new method in {@link proguard.evaluation.value.ValueFactory}, but until something like this is
 * needed in different location having a separate function looks cleaner.
 */
@FunctionalInterface
public interface ValueCalculator {

  /**
   * Create a {@link Value} given all available known information about it (included the actual
   * tracked value or a {@link Model} of it, if available, and its reference identifier if the value
   * has the same reference as an existing one).
   *
   * <p>This method is not limited to particular value and can be used to create any value given the
   * available amount of information.
   *
   * @param type the static type of the created value (runtime type might implement/extend it).
   * @param referencedClass the {@link Clazz} of the value (if it's a reference value).
   * @param isParticular whether the value to create is particular. If not the `concreteValue`
   *     parameter will be ignored.
   * @param concreteValue the value of the tracked object. Can be the actual value or a {@link
   *     Model}.
   * @param valueMayBeExtension whether the created value might actually be an extension of the
   *     type. This should always be false for the instance created by constructors since they
   *     always create an object of the exact type. This should usually be {@link
   *     ClassUtil#isExtendable(Clazz)} for non constructor calls or more precise information if the
   *     caller is able to provide it.
   * @param valueId the already known reference identifier of the created value. Null if the
   *     identifier was not previously known. This is particularly important for constructors, since
   *     they always return void and the only way to associate the constructed object to its
   *     existing references is via the valueId.
   * @return The {@link Value} corresponding to the given parameters.
   */
  Value apply(
      @NotNull String type,
      @Nullable Clazz referencedClass,
      boolean isParticular,
      @Nullable Object concreteValue,
      boolean valueMayBeExtension,
      @Nullable Object valueId);
}
