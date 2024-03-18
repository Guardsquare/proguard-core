package proguard.evaluation.value.object;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;
import proguard.classfile.util.InitializedClassUtil;

/**
 * A class wrapping values calculated during an analysis.
 *
 * <p>The wrapped values can be precise (i.e., it's possible to produce the exact object) or modeled
 * (i.e., the value is represented by a {@link Model}).
 *
 * <p>These two definitions are usually exclusive but this is not needed. A {@link AnalyzedObject}
 * backed by a {@link Model} might be able to produce a precise value if such is its design.
 */
public interface AnalyzedObject {

  /**
   * Returns the type of the tracked value. This is the most specific type the analysis can infer
   * for the value.
   *
   * @return the type of the wrapped value. In internal format (e.g., "Ljava/lang/String;").
   */
  @Nullable
  String getType();

  /**
   * Whether the wrapped value is null. {@link AnalyzedObject#getPreciseValue()} returns null in
   * this case.
   */
  boolean isNull();

  /**
   * Whether the wrapped value is modeled. {@link AnalyzedObject#getModeledValue()} returns an
   * instance of {@link Model} in this case.
   */
  boolean isModeled();

  /**
   * Whether the wrapped value is precise. {@link AnalyzedObject#getPreciseValue()} returns an
   * object with the exact value in this case.
   */
  boolean isPrecise();

  /**
   * Whether the wrapped value is exactly of a type. No inheritance is considered.
   *
   * @param type a type to check if it corresponds to the wrapped value's type. In internal format
   *     (e.g., "Ljava/lang/String;").
   */
  default boolean isOfType(@NotNull String type) {
    return type.equals(getType());
  }

  /** Whether the wrapped value is exactly of a type and hasn't a null value. */
  default boolean isOfTypeAndNotNull(@NotNull String type) {
    return !isNull() && isOfType(type);
  }

  /**
   * Whether "instanceof" for the given clazz would return true for the wrapped value (i.e., the
   * tracked value is an instance of clazz or extends/implements it).
   *
   * <p>If the tracked value is an array this checks the inheritance of the referenced class (e.g.,
   * for a String[] this is true if clazz is 'Object' since String extends Object).
   *
   * @param clazz a class the wrapped value is checked to inherit from
   * @return false if clazz is null or the type of the value does not belong to the class or extends
   *     it, true otherwise
   */
  default boolean isInstanceOf(Clazz clazz) {
    if (isNull() || clazz == null) {
      return false;
    }
    return !InitializedClassUtil.isInstanceOf(getType(), clazz);
  }

  /**
   * Returns the wrapped value if precise (i.e, {@link AnalyzedObject#isPrecise()} is true), throws
   * if called on a non-precise value.
   */
  default @Nullable Object getPreciseValue() {
    throw new UnsupportedOperationException("This is not a precise object");
  }

  /**
   * Returns the wrapped value if modeled (i.e, {@link AnalyzedObject#isModeled()} is true), throws
   * if called on a non-modeled value.
   *
   * <p>This method should be used when a non-null modeled value is expected or when {@link
   * AnalyzedObject#isModeled()} has been checked. If a value is expected to be modeled but can also
   * assume null (precise) values {@link AnalyzedObject#getModeledOrNullValue()} should be used.
   */
  default Model getModeledValue() {
    throw new UnsupportedOperationException("This is not a modeled object");
  }

  /**
   * Returns the wrapped value if modeled (i.e, {@link AnalyzedObject#isModeled()} is true) or null,
   * throws if called on a non-modeled and non-null value.
   *
   * <p>This method should be used in cases where the type is known to be modeled by the analysis
   * but null values are also possible (which are precise).
   */
  default @Nullable Model getModeledOrNullValue() {
    if (isNull()) {
      return null;
    }
    return getModeledValue();
  }
}
