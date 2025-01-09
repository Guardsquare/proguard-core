package proguard.evaluation.executor;

import static proguard.classfile.ClassConstants.*;
import static proguard.classfile.util.ClassUtil.internalClassName;
import static proguard.classfile.util.ClassUtil.internalClassNameFromType;
import static proguard.exception.ErrorId.OBJECT_GET_CLASS_EXECUTOR_UNSUPPORTED_SIGNATURE;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.classfile.visitor.ClassVisitor;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.model.ClassModel;
import proguard.exception.ProguardCoreException;

/**
 * This {@link Executor} provides an implementation for {@link Executor#getMethodResult} which
 * resolves all types of <code>'Object'.getClass</code> {@link Class} calls based on the classes in
 * the class pools.
 *
 * <p>For example <code>classA.getClass()</code> and <code>classB.getClass()</code>
 */
public class ObjectGetClassExecutor implements Executor {

  private final ClassPool programClassPool;
  private final ClassPool libraryClassPool;

  private final Set<MethodSignature> supportedMethodSignatures = new HashSet<>();

  /** Private constructor reserved for the static {@link Builder}. */
  private ObjectGetClassExecutor(ClassPool programClassPool, ClassPool libraryClassPool) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;

    ClassVisitor getClassSignatureCollector =
        clazz ->
            supportedMethodSignatures.add(
                new MethodSignature(
                    clazz.getName(), METHOD_NAME_OBJECT_GET_CLASS, METHOD_TYPE_OBJECT_GET_CLASS));
    programClassPool.classesAccept(getClassSignatureCollector);
    libraryClassPool.classesAccept(getClassSignatureCollector);
  }

  @Override
  public MethodResult getMethodResult(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    if (!METHOD_NAME_OBJECT_GET_CLASS.equals(methodExecutionInfo.getSignature().getMethodName()))
      throw new ProguardCoreException(
          OBJECT_GET_CLASS_EXECUTOR_UNSUPPORTED_SIGNATURE,
          String.format(
              "%s is not a supported method signature.", methodExecutionInfo.getSignature()));

    Value instance = methodExecutionInfo.getInstanceNonStatic();
    if (!(instance instanceof TypedReferenceValue)) return MethodResult.invalidResult();
    TypedReferenceValue typedInstance = (TypedReferenceValue) instance;

    if (typedInstance.getType() == null) return MethodResult.invalidResult();
    Optional<Clazz> clazz = findReferencedClazz(internalClassNameFromType(typedInstance.getType()));
    if (clazz.isPresent()) {
      return createResult(methodExecutionInfo, valueCalculator, new ClassModel(clazz.get()));
    }
    return MethodResult.invalidResult();
  }

  @Override
  public Set<MethodSignature> getSupportedMethodSignatures() {
    return supportedMethodSignatures;
  }

  // Helper methods.

  /**
   * Retrieves the class with the given name from the class pools.
   *
   * @param className Name of the class.
   * @return An {@link Optional<Clazz>} containing the {@link Clazz} with the given name, or empty
   *     if it was not in the class pools.
   */
  private Optional<Clazz> findReferencedClazz(@Nullable String className) {
    if (className == null) return Optional.empty();

    className = internalClassName(className);
    Clazz result = programClassPool.getClass(className);
    return (result != null)
        ? Optional.of(result)
        : Optional.ofNullable(libraryClassPool.getClass(className));
  }

  private static MethodResult createResult(
      MethodExecutionInfo executionInfo,
      ValueCalculator valueCalculator,
      @Nullable Object concreteValue) {
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

  // Builder class.

  /** Builder for {@link ObjectGetClassExecutor}. */
  public static class Builder implements Executor.Builder<ObjectGetClassExecutor> {
    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    private ObjectGetClassExecutor objectGetClassExecutor = null;

    public Builder(@NotNull ClassPool programClassPool, @NotNull ClassPool libraryClassPool) {
      this.programClassPool = programClassPool;
      this.libraryClassPool = libraryClassPool;
    }

    @Override
    public ObjectGetClassExecutor build() {
      if (objectGetClassExecutor == null) {
        objectGetClassExecutor = new ObjectGetClassExecutor(programClassPool, libraryClassPool);
      }
      return objectGetClassExecutor;
    }
  }
}
