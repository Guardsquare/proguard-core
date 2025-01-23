package proguard.evaluation;

import org.jetbrains.annotations.NotNull;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.IdentifiedValueFactory;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.TypedReferenceValueFactory;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.AnalyzedObjectFactory;

/**
 * This {@link TypedReferenceValueFactory} creates reference values that also represent their
 * content.
 *
 * <p>Like {@link IdentifiedValueFactory}, it tracks {@link IdentifiedReferenceValue}s with a unique
 * integer ID.
 *
 * <p>Calling a `createReferenceValue` method will increment the internal referencedID counter and
 * return an object representing that {@link Value} with that new referenceID.
 *
 * <p>Calling a `createReferenceForId` method will return an object representing that {@link Value}
 * with the specified ID.
 */
public class ParticularReferenceValueFactory extends TypedReferenceValueFactory {

  /**
   * Deprecated, use {@link ParticularReferenceValueFactory#createReferenceValue(String, Clazz,
   * boolean, boolean, CodeLocation)}
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset) {
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      CodeLocation creationLocation) {
    checkCreationLocation(creationLocation);
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  /**
   * Deprecated, use {@link ParticularReferenceValueFactory#createReferenceValue(Clazz, boolean,
   * boolean, CodeLocation, AnalyzedObject)}
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset,
      Object value) {
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, value);
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      CodeLocation creationLocation,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    checkCreationLocation(creationLocation);
    return createReferenceValue(referencedClass, mayBeExtension, mayBeNull, value);
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    return createReferenceValueForId(
        type,
        referencedClass,
        mayBeExtension,
        mayBeNull,
        IdentifiedValueFactory.generateReferenceId());
  }

  /**
   * Deprecated, use {@link ParticularReferenceValueFactory#createReferenceValue(Clazz, boolean,
   * boolean, AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object value) {
    return createReferenceValueForId(
        type,
        referencedClass,
        mayBeExtension,
        mayBeNull,
        IdentifiedValueFactory.generateReferenceId(),
        value);
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    return createReferenceValueForId(
        referencedClass,
        mayBeExtension,
        mayBeNull,
        IdentifiedValueFactory.generateReferenceId(),
        value);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object id) {
    return type == null
        ? createReferenceValueNull()
        : new IdentifiedReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, this, id);
  }

  /**
   * Deprecated, use {@link ParticularReferenceValueFactory#createReferenceValueForId(Clazz,
   * boolean, boolean, Object, AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValueForId(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Object id,
      Object value) {
    if (type == null) {
      return createReferenceValueNull();
    }
    AnalyzedObject object = AnalyzedObjectFactory.create(value, type, referencedClass);
    return new ParticularReferenceValue(referencedClass, this, id, object);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Object id,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    if (value.getType() == null) {
      return createReferenceValueNull();
    }
    return new ParticularReferenceValue(referencedClass, this, id, value);
  }
}
