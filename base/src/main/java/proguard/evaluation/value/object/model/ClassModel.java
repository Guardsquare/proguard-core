package proguard.evaluation.value.object.model;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.ClassConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.reflective.ModelHelper;
import proguard.evaluation.value.object.model.reflective.ModeledInstanceMethod;
import proguard.evaluation.value.object.model.reflective.ModeledStaticMethod;
import proguard.evaluation.value.object.model.reflective.ReflectiveModel;

/** A {@link Model} to track specific Clazz constants. */
public class ClassModel implements ReflectiveModel<ClassModel> {
  private final Clazz clazz;
  private final Class<?> primitiveClass;
  private final String name;

  /**
   * Mandatory no-argument constructor.
   *
   * @see ModelHelper#getDummyObject(Class)
   */
  private ClassModel() {
    this.clazz = null;
    this.primitiveClass = null;
    this.name = null;
  }

  /**
   * Creates a new {@link ClassModel} for the given {@link Clazz}.
   *
   * @param clazz The class to model.
   */
  public ClassModel(@NotNull Clazz clazz) {
    this.clazz = clazz;
    this.primitiveClass = null;
    this.name = null;
  }

  /**
   * Creates a new {@link ClassModel} for an unknown class.
   *
   * @param name The name of the class.
   */
  public ClassModel(String name) {
    this.clazz = null;
    this.primitiveClass = null;
    this.name = name;
  }

  /**
   * Used for creating a {@link ClassModel} for any of the primitive classes.
   *
   * @param clazz one of the primitive classes. <code>int.class</code>,<code>boolean.class</code>,
   *     ...
   * @throws IllegalArgumentException iff a non-primitive {@link Class} object is passed.
   */
  public ClassModel(@NotNull Class<?> clazz) {
    if (!clazz.isPrimitive()) {
      throw new IllegalArgumentException("The class parameter must be a primitive.");
    }
    this.clazz = null;
    this.primitiveClass = clazz;
    this.name = null;
  }

  /**
   * Use this in case {@link ClassModel#isPrimitive()} is true. Otherwise, use {@link
   * ClassModel#getPrimitiveClass()}
   *
   * @return the modeled {@link Clazz}.
   * @throws IllegalArgumentException if a primitive class is modeled.
   */
  public @Nullable Clazz getClazz() {
    if (isPrimitive()) throw new IllegalArgumentException("A primitive class is modeled.");

    return clazz;
  }

  /**
   * Use this in case {@link ClassModel#isPrimitive()} is false. Otherwise, use {@link
   * ClassModel#getClazz()}.
   *
   * @return the modeled {@link Class}.
   * @throws IllegalArgumentException if a non-primitive class is modeled.
   */
  public Class<?> getPrimitiveClass() {
    if (!isPrimitive()) throw new IllegalArgumentException("A non-primitive class is modeled.");
    return primitiveClass;
  }

  public boolean isPrimitive() {
    return primitiveClass != null;
  }

  public boolean isUnknown() {
    return name != null;
  }

  /**
   * @return the name of the modeled class.
   */
  public @Nullable String getModeledClassName() {
    if (isPrimitive()) return primitiveClass.getName();
    if (clazz != null) return clazz.getName();

    return name;
  }

  // Model implementation.

  @NotNull
  @Override
  public String getType() {
    return ClassConstants.TYPE_JAVA_LANG_CLASS;
  }

  @Override
  public MethodResult init(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    throw new UnsupportedOperationException(
        "Constructors invocation is not supported in ClassModel");
  }

  // Supported method implementations.

  /** Models {@link Class#getName()}. */
  @ModeledInstanceMethod(name = "getName", descriptor = "()Ljava/lang/String;")
  MethodResult getName(ModelHelper.MethodExecutionContext context) {
    if (primitiveClass != null)
      return ModelHelper.createDefaultReturnResult(context, primitiveClass.getName());
    if (clazz != null) return ModelHelper.createDefaultReturnResult(context, clazz.getName());
    return MethodResult.invalidResult();
  }

  /** Models {@link Class#getSimpleName()}. */
  @ModeledInstanceMethod(name = "getSimpleName", descriptor = "()Ljava/lang/String;")
  MethodResult getSimpleName(ModelHelper.MethodExecutionContext context) {
    if (primitiveClass != null) {
      return ModelHelper.createDefaultReturnResult(context, primitiveClass.getSimpleName());
    }
    if (clazz != null) {
      return ModelHelper.createDefaultReturnResult(
          context, ClassUtil.internalSimpleClassName(clazz.getName()));
    }
    return MethodResult.invalidResult();
  }

  /** Models {@link Class#getCanonicalName()}. */
  @ModeledInstanceMethod(name = "getCanonicalName", descriptor = "()Ljava/lang/String;")
  MethodResult getCanonicalName(ModelHelper.MethodExecutionContext context) {
    if (primitiveClass != null) {
      return ModelHelper.createDefaultReturnResult(context, primitiveClass.getCanonicalName());
    }
    if (clazz != null) {
      return ModelHelper.createDefaultReturnResult(
          context, ClassUtil.canonicalClassName(clazz.getName()));
    }
    return MethodResult.invalidResult();
  }

  /** Models {@link Class#getPackageName()}. */
  @ModeledInstanceMethod(name = "getPackageName", descriptor = "()Ljava/lang/String;")
  MethodResult getPackageName(ModelHelper.MethodExecutionContext context) {
    if (primitiveClass != null) {
      return ModelHelper.createDefaultReturnResult(
          context,
          // Hard coded since getPackage is not available on our language version.
          "java.lang");
    }
    if (clazz != null) {
      return ModelHelper.createDefaultReturnResult(
          context, ClassUtil.externalPackageName(ClassUtil.externalClassName(clazz.getName())));
    }
    return MethodResult.invalidResult();
  }

  /** Models {@link Class#getTypeName()}. */
  @ModeledInstanceMethod(name = "getTypeName", descriptor = "()Ljava/lang/String;")
  MethodResult getTypeName(ModelHelper.MethodExecutionContext context) {
    if (primitiveClass != null) {
      return ModelHelper.createDefaultReturnResult(context, primitiveClass.getTypeName());
    }
    if (clazz != null) {
      return ModelHelper.createDefaultReturnResult(
          context, ClassUtil.externalClassName(clazz.getName()));
    }
    return MethodResult.invalidResult();
  }

  /** Models {@link Class#getSuperclass()}. */
  @ModeledInstanceMethod(name = "getSuperclass", descriptor = "()Ljava/lang/Class;")
  private MethodResult getSuperclass(ModelHelper.MethodExecutionContext context) {
    if (isPrimitive()) return MethodResult.invalidResult();

    if (clazz == null) return MethodResult.invalidResult();

    Clazz superClass = clazz.getSuperClass();
    if (superClass != null)
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(superClass));

    return MethodResult.invalidResult();
  }

  /** Models {@link Class#forName(String)}. */
  @ModeledStaticMethod(name = "forName", descriptor = "(Ljava/lang/String;)Ljava/lang/Class;")
  MethodResult forName(ModelHelper.MethodExecutionContext context, Value classNameValue) {
    if (!classNameValue.isParticular()) return MethodResult.invalidResult();

    String className = (String) classNameValue.referenceValue().getValue().getPreciseValue();
    Optional<Clazz> clazz =
        findReferencedClazz(
            className,
            context.getExecutionInfo().getProgramClassPool(),
            context.getExecutionInfo().getLibraryClassPool());
    if (clazz.isPresent())
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(clazz.get()));

    return ModelHelper.createDefaultReturnResult(context, new ClassModel(className));
  }

  /** Models {@link Class#forName(String, boolean, ClassLoader)}. */
  @ModeledStaticMethod(
      name = "forName",
      descriptor = "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")
  MethodResult forName(
      ModelHelper.MethodExecutionContext context,
      Value classNameValue,
      Value initializeValue,
      Value classLoaderValue) {
    if (!classNameValue.isParticular()) return MethodResult.invalidResult();

    String className = (String) classNameValue.referenceValue().getValue().getPreciseValue();
    Optional<Clazz> clazz =
        findReferencedClazz(
            className,
            context.getExecutionInfo().getProgramClassPool(),
            context.getExecutionInfo().getLibraryClassPool());
    if (clazz.isPresent())
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(clazz.get()));

    return ModelHelper.createDefaultReturnResult(context, new ClassModel(className));
  }

  // Object overrides.

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassModel that = (ClassModel) o;

    return Objects.equals(clazz, that.clazz)
        && Objects.equals(primitiveClass, that.primitiveClass)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, primitiveClass, name);
  }

  @Override
  public String toString() {
    return String.format("ClassModel{%s}", getModeledClassName());
  }

  // Private helper methods.

  /**
   * Retrieves the class with the given name from the class pools.
   *
   * @param className Name of the class.
   * @return An {@link Optional Clazz} containing the {@link Clazz} with the given name, or empty if
   *     it was not in the class pools.
   */
  private static Optional<Clazz> findReferencedClazz(
      @Nullable String className, ClassPool programClassPool, ClassPool libraryClassPool) {
    if (className == null) return Optional.empty();

    className = ClassUtil.internalClassName(className);
    Clazz result = programClassPool.getClass(className);
    return (result != null)
        ? Optional.of(result)
        : Optional.ofNullable(libraryClassPool.getClass(className));
  }
}
