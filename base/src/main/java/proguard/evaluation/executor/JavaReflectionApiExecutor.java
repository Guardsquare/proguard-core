package proguard.evaluation.executor;

import static proguard.classfile.ClassConstants.*;
import static proguard.classfile.util.ClassUtil.canonicalClassName;
import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.classfile.util.ClassUtil.externalPackageName;
import static proguard.classfile.util.ClassUtil.internalClassName;
import static proguard.classfile.util.ClassUtil.internalClassNameFromType;
import static proguard.classfile.util.ClassUtil.internalSimpleClassName;
import static proguard.exception.ErrorId.EVALUATION_JAVA_REFLECTION_EXECUTOR;

import java.util.Arrays;
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
import proguard.evaluation.value.object.ClassModel;
import proguard.exception.ProguardCoreException;

/**
 * This {@link Executor} provides an implementation for {@link Executor#getMethodResult} which
 * resolves a number of simple {@link Class} and {@link ClassLoader} API methods.
 */
public class JavaReflectionApiExecutor implements Executor {

  private final ClassPool programClassPool;
  private final ClassPool libraryClassPool;

  private final Set<MethodSignature> supportedMethodSignatures;

  /** Private constructor reserved for the static {@link Builder}. */
  private JavaReflectionApiExecutor(ClassPool programClassPool, ClassPool libraryClassPool) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;

    supportedMethodSignatures =
        new HashSet<>(
            Arrays.asList(
                CLASS_GET_NAME_SIGNATURE,
                CLASS_GET_SIMPLE_NAME_SIGNATURE,
                CLASS_GET_TYPE_NAME_SIGNATURE,
                CLASS_GET_PACKAGE_NAME_SIGNATURE,
                CLASS_GET_SUPERCLASS_SIGNATURE,
                CLASS_GET_CANONICAL_NAME_SIGNATURE,
                CLASSLOADER_LOAD_CLASS_SIGNATURE,
                CLASSLOADER_LOAD_CLASS_SIGNATURE2,
                CLASS_FOR_NAME_SIGNATURE,
                CLASS_FOR_NAME_SIGNATURE2));

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
    MethodSignature target = methodExecutionInfo.getSignature();

    // Handling these signatures requires the class name (String).
    if (target.equals(CLASS_FOR_NAME_SIGNATURE)
        || target.equals(CLASS_FOR_NAME_SIGNATURE2)
        || target.equals(CLASSLOADER_LOAD_CLASS_SIGNATURE)
        || target.equals(CLASSLOADER_LOAD_CLASS_SIGNATURE2)) {
      Value className = methodExecutionInfo.getParameters().get(0);
      if (!className.isParticular()) return MethodResult.invalidResult();

      Optional<Clazz> clazz =
          findReferencedClazz((String) className.referenceValue().getValue().getPreciseValue());
      if (clazz.isPresent()) {
        return createResult(methodExecutionInfo, valueCalculator, new ClassModel(clazz.get()));
      }
      return MethodResult.invalidResult();
    }

    // Handling these signatures only requires the type of the instance.
    if (METHOD_NAME_OBJECT_GET_CLASS.equals(target.getMethodName())) {
      Value instance = methodExecutionInfo.getInstanceNonStatic();
      if (!(instance instanceof TypedReferenceValue)) return MethodResult.invalidResult();
      TypedReferenceValue typedInstance = (TypedReferenceValue) instance;

      Optional<Clazz> clazz =
          findReferencedClazz(internalClassNameFromType(typedInstance.getType()));
      if (clazz.isPresent()) {
        return createResult(methodExecutionInfo, valueCalculator, new ClassModel(clazz.get()));
      }
    }

    // Handling these signatures requires a modeled class (ClassModel) instance.
    if (target.equals(CLASS_GET_NAME_SIGNATURE)
        || target.equals(CLASS_GET_TYPE_NAME_SIGNATURE)
        || target.equals(CLASS_GET_SIMPLE_NAME_SIGNATURE)
        || target.equals(CLASS_GET_CANONICAL_NAME_SIGNATURE)
        || target.equals(CLASS_GET_PACKAGE_NAME_SIGNATURE)
        || target.equals(CLASS_GET_SUPERCLASS_SIGNATURE)) {
      Value instance = methodExecutionInfo.getInstanceNonStatic();
      if (!(instance instanceof TypedReferenceValue)) return MethodResult.invalidResult();

      TypedReferenceValue typedInstance = (TypedReferenceValue) instance;
      if (!typedInstance.isParticular() || typedInstance.isNull() == Value.ALWAYS)
        return MethodResult.invalidResult();

      ClassModel modeledValue =
          (ClassModel) typedInstance.referenceValue().getValue().getModeledValue();
      String externalClassName = externalClassName(modeledValue.getClazz().getName());

      Object concreteValue = null;
      if (target.equals(CLASS_GET_NAME_SIGNATURE) || target.equals(CLASS_GET_TYPE_NAME_SIGNATURE)) {
        concreteValue = externalClassName;
      } else if (target.equals(CLASS_GET_SIMPLE_NAME_SIGNATURE)) {
        concreteValue = internalSimpleClassName(modeledValue.getClazz().getName());
      } else if (target.equals(CLASS_GET_CANONICAL_NAME_SIGNATURE)) {
        concreteValue = canonicalClassName(externalClassName);
      } else if (target.equals(CLASS_GET_PACKAGE_NAME_SIGNATURE)) {
        concreteValue = externalPackageName(externalClassName);
      } else if (target.equals(CLASS_GET_SUPERCLASS_SIGNATURE)) {
        concreteValue = new ClassModel(modeledValue.getClazz().getSuperClass());
      }
      return createResult(methodExecutionInfo, valueCalculator, concreteValue);
    }

    throw new ProguardCoreException(
        EVALUATION_JAVA_REFLECTION_EXECUTOR,
        String.format(
            "%s is not a supported method signature.", methodExecutionInfo.getSignature()));
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

  /** Builder for {@link JavaReflectionApiExecutor}. */
  public static class Builder implements Executor.Builder<JavaReflectionApiExecutor> {
    private final ClassPool programClassPool;
    private final ClassPool libraryClassPool;

    private JavaReflectionApiExecutor javaReflectionApiExecutor = null;

    public Builder(@NotNull ClassPool programClassPool, @NotNull ClassPool libraryClassPool) {
      this.programClassPool = programClassPool;
      this.libraryClassPool = libraryClassPool;
    }

    @Override
    public JavaReflectionApiExecutor build() {
      if (javaReflectionApiExecutor == null) {
        javaReflectionApiExecutor =
            new JavaReflectionApiExecutor(programClassPool, libraryClassPool);
      }
      return javaReflectionApiExecutor;
    }
  }
}
