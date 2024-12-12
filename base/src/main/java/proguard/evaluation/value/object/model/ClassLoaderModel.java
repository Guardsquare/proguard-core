package proguard.evaluation.value.object.model;

import static proguard.classfile.util.ClassUtil.internalClassName;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.ClassConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.reflective.ModelHelper;
import proguard.evaluation.value.object.model.reflective.ModeledInstanceMethod;
import proguard.evaluation.value.object.model.reflective.ReflectiveModel;

public class ClassLoaderModel implements ReflectiveModel<ClassLoaderModel> {

  // Implementations for Model.

  @Override
  public @NotNull String getType() {
    return ClassConstants.TYPE_JAVA_LANG_CLASS_LOADER;
  }

  @Override
  public MethodResult init(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    throw new UnsupportedOperationException(
        "Constructors invocation is not supported in " + this.getClass().getName());
  }

  // ClassLoader implementations.

  /** Models {@link ClassLoader#loadClass(String)}. */
  @ModeledInstanceMethod(name = "loadClass", descriptor = "(Ljava/lang/String;)Ljava/lang/Class;")
  MethodResult loadClass(ModelHelper.MethodExecutionContext context, Value classNameValue) {
    if (!classNameValue.isParticular()) return MethodResult.invalidResult();

    Optional<Clazz> clazz =
        findReferencedClazz(
            (String) classNameValue.referenceValue().getValue().getPreciseValue(),
            context.getExecutionInfo().getProgramClassPool(),
            context.getExecutionInfo().getLibraryClassPool());
    if (clazz.isPresent())
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(clazz.get()));

    return MethodResult.invalidResult();
  }

  /** Models {@link ClassLoader#loadClass(String, boolean)}. */
  @ModeledInstanceMethod(name = "loadClass", descriptor = "(Ljava/lang/String;Z)Ljava/lang/Class;")
  MethodResult loadClass(
      ModelHelper.MethodExecutionContext context, Value classNameValue, Value resolve) {
    if (!classNameValue.isParticular()) return MethodResult.invalidResult();

    Optional<Clazz> clazz =
        findReferencedClazz(
            (String) classNameValue.referenceValue().getValue().getPreciseValue(),
            context.getExecutionInfo().getProgramClassPool(),
            context.getExecutionInfo().getLibraryClassPool());
    if (clazz.isPresent())
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(clazz.get()));

    return MethodResult.invalidResult();
  }

  /** Models {@link ClassLoader#findLoadedClass(String)}. */
  @ModeledInstanceMethod(
      name = "findLoadedClass",
      descriptor = "(Ljava/lang/String;)Ljava/lang/Class;")
  MethodResult findLoadedClass(ModelHelper.MethodExecutionContext context, Value classNameValue) {
    if (!classNameValue.isParticular()) return MethodResult.invalidResult();

    Optional<Clazz> clazz =
        findReferencedClazz(
            (String) classNameValue.referenceValue().getValue().getPreciseValue(),
            context.getExecutionInfo().getProgramClassPool(),
            context.getExecutionInfo().getLibraryClassPool());
    if (clazz.isPresent())
      return ModelHelper.createDefaultReturnResult(context, new ClassModel(clazz.get()));

    return MethodResult.invalidResult();
  }

  // Private helper methods.

  /**
   * Retrieves the class with the given name from the class pools.
   *
   * @param className Name of the class.
   * @return An {@link Optional <Clazz>} containing the {@link Clazz} with the given name, or empty
   *     if it was not in the class pools.
   */
  private Optional<Clazz> findReferencedClazz(
      @Nullable String className, ClassPool programClassPool, ClassPool libraryClassPool) {
    if (className == null) return Optional.empty();

    className = internalClassName(className);
    Clazz result = programClassPool.getClass(className);
    return (result != null)
        ? Optional.of(result)
        : Optional.ofNullable(libraryClassPool.getClass(className));
  }
}
