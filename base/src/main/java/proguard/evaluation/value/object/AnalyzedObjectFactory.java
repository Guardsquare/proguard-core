package proguard.evaluation.value.object;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InitializedClassUtil;
import proguard.evaluation.value.Value;

/** Factory methods to create {@link AnalyzedObject}. */
public class AnalyzedObjectFactory {

  private AnalyzedObjectFactory() {}

  /**
   * Create a {@link AnalyzedObject}, representing the tracked value for a reference type. If an
   * object is created successfully it is guaranteed to be modeled if the passed value was an
   * instance of {@link Model}.
   *
   * <p>Consistency checks on the arguments are performed:
   *
   * <ul>
   *   <li>If "value" is null and "referencedClass" isn't, "type" should match the type of
   *       "referencedClass"
   *   <li>If "type" represents a primitive type or primitive array "referencedClass" should be null
   *   <li>If "referencedClass" is not null, the type of "value" (or of the type modeled by "value"
   *       if it is a {@link Model}) has to be the same type of "referencedClass" or of a class
   *       inheriting from it.
   *   <li>If "referencedClass" is null, "type" needs to match the type of "value" (or of the type
   *       modeled by "value" if it is a {@link Model})
   * </ul>
   *
   * @param value the value of the tracked object. Can be the actual value or a {@link Model}. null
   *     iff the tracked value is null
   * @param type the type of the value. This should be the static type specified in the method
   *     signature, might not correspond to the type returned by the created {@link
   *     AnalyzedObject#getType()} because dynamic typing might be taken into consideration.
   * @param referencedClass the class referenced when the value is created (e.g., the class of the
   *     return value of a method call). Always null for primitive types (or arrays of primitive);
   *     can be null if the library class pool is missing or the class pool references have not been
   *     initialized; can also be null in some cases even if everything has been initialized. For
   *     this reason this is used just for sanity checks when not-null, while it being null is not
   *     considered in any way as something incorrect.
   */
  public static AnalyzedObject create(
      @Nullable Object value, @Nullable String type, @Nullable Clazz referencedClass) {
    if (value == null && type == null) {
      checkValidNullObject(referencedClass);
      return createNull();
    }

    checkValidType(type);

    if (value == null) {
      checkValidTypedNullObject(type, referencedClass);
      return createNullOfType(ClassUtil.internalTypeFromClassName(type));
    }

    if (value instanceof Model) {
      Model model = (Model) value;
      checkValidModel(model, referencedClass);
      return createModeled(model);
    }

    // TODO create ArrayObject if value is an array once we fix the code using
    // ParticularReferenceValue for arrays

    checkValidPrecise(value, type, referencedClass);
    return createPrecise(value);
  }

  private static void checkValidNullObject(Clazz referencedClass) {
    if (referencedClass != null) {
      throw new IllegalStateException(
          String.format(
              "'type' is null while 'referencedClass' is \"%s\", should 'type' be the one from 'referencedClass'?",
              referencedClass.getName()));
    }
  }

  private static void checkValidType(String type) {
    if (type == null) {
      throw new IllegalStateException("'type' should be null just for null 'value'");
    }

    if (!ClassUtil.isInternalType(type)) {
      throw new IllegalStateException(
          String.format(
              "the 'type' \"%s\" has an invalid format, should be an internal type (e.g., 'Ljava/lang/String;')",
              type));
    }
  }

  private static void checkValidTypedNullObject(String type, Clazz referencedClass) {
    if (referencedClass != null) {
      String referencedType = ClassUtil.internalTypeFromClassName(referencedClass.getName());
      if (!type.equals(referencedType)) {
        throw new IllegalStateException(
            String.format(
                "'value' is null but 'type' \"%s\" is not the type of 'referencedClass' \"%s\"",
                type, referencedType));
      }
    }
  }

  private static void checkValidModel(Model model, Clazz referencedClass) {
    if (referencedClass != null
        && !InitializedClassUtil.isInstanceOf(model.getType(), referencedClass)) {
      throw new IllegalStateException(
          String.format(
              "The type \"%s\" of model \"%s\" does not extend the referenced clazz \"%s\"",
              model.getType(), model, referencedClass.getName()));
    }
  }

  private static void checkValidPrecise(Object value, String type, Clazz referencedClass) {
    String objectType = ClassUtil.internalType(value.getClass().getTypeName());
    if (referencedClass == null && !type.equals(objectType)) {
      throw new IllegalStateException(
          String.format(
              "The referenced clazz is null, the type \"%s\" of the object needs to be exactly the provided type \"%s\"",
              objectType, type));
    }
    if (referencedClass != null
        && !InitializedClassUtil.isInstanceOf(objectType, referencedClass)) {
      throw new IllegalStateException(
          String.format(
              "The type \"%s\" of the object does not extend the referenced clazz \"%s\"",
              type, referencedClass.getName()));
    }
  }

  /**
   * Create a precise object wrapping the value.
   *
   * @param value The wrapped value
   */
  public static AnalyzedObject createPrecise(@NotNull Object value) {
    Objects.requireNonNull(value, "'value' can't be null");
    // TODO throw IllegalStateException if value is an array once we fix the code using
    // ParticularReferenceValue for arrays
    return new PreciseObject(value);
  }

  /**
   * Create an object the value of which is modeled.
   *
   * @param value The wrapped model
   */
  public static AnalyzedObject createModeled(@NotNull Model value) {
    Objects.requireNonNull(value, "'value' can't be null");
    if (value instanceof ArrayModel) {
      return new ArrayObject((ArrayModel) value);
    }
    return new ModeledObject(value);
  }

  /**
   * Create an object with null value and known type.
   *
   * @param type The known type of the null object
   */
  public static AnalyzedObject createNullOfType(@NotNull String type) {
    Objects.requireNonNull(type, "'type' can't be null");
    return new TypedNullObject(type);
  }

  /** Create an object with unknown type and null value. */
  public static AnalyzedObject createNull() {
    return NullObject.INSTANCE;
  }

  /**
   * Create a modeled object representing a detailed array (i.e., it's model is a {@link
   * ArrayModel}.
   */
  public static AnalyzedObject createDetailedArray(Value[] values, String type) {
    return new ArrayObject(new ArrayModel(values, type));
  }
}
