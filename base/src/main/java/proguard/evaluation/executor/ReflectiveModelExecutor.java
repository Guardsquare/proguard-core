package proguard.evaluation.executor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.MethodInfo;
import proguard.classfile.MethodSignature;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InitializedClassUtil;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.model.Model;
import proguard.evaluation.value.object.model.reflective.ModelHelper;
import proguard.evaluation.value.object.model.reflective.ReflectiveModel;
import proguard.util.HierarchyProvider;

/**
 * An {@link Executor} with support for {@link ReflectiveModel}s.
 *
 * <p>Checks the parameters sanity so that the models can work assuming they get clean data and
 * matches the target {@link ReflectiveModel} and executes the method on it via the reflection
 * supported by {@link ModelHelper}.
 */
public class ReflectiveModelExecutor implements Executor {
  private static final Logger log = LogManager.getLogger(ReflectiveModelExecutor.class);
  protected final Set<SupportedModelInfo<? extends ReflectiveModel<?>>> supportedModels;
  protected final Set<MethodSignature> supportedSignatures;
  protected final Map<MethodSignature, Class<? extends ReflectiveModel<?>>>
      supportedSignatureToModel;

  protected ReflectiveModelExecutor(
      Set<SupportedModelInfo<?>> supportedModels, HierarchyProvider hierarchy) {
    this.supportedModels = supportedModels;

    Set<MethodSignature> supportedSignaturesMutable = new HashSet<>();
    Map<MethodSignature, Class<? extends ReflectiveModel<?>>> supportedSignatureToModelMutable =
        new HashMap<>();

    for (SupportedModelInfo<? extends ReflectiveModel<?>> modelInfo : supportedModels) {
      generateMethodSignaturesForModel(modelInfo, hierarchy)
          .forEach(
              signature -> {
                supportedSignaturesMutable.add(signature);
                supportedSignatureToModelMutable.put(signature, modelInfo.getModelClass());
              });
    }

    this.supportedSignatures = Collections.unmodifiableSet(supportedSignaturesMutable);
    this.supportedSignatureToModel = Collections.unmodifiableMap(supportedSignatureToModelMutable);
  }

  @Override
  public MethodResult getMethodResult(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {

    checkInstanceSanity(methodExecutionInfo, supportedSignatureToModel);
    if (!canExecuteWithInstance(methodExecutionInfo)) {
      return MethodResult.invalidResult();
    }

    checkParameterSanity(methodExecutionInfo, supportedSignatureToModel);

    Model instanceModel = getInstanceModel(methodExecutionInfo);
    return execute(methodExecutionInfo, valueCalculator, instanceModel);
  }

  /**
   * For instance methods returns the instance model, for static location and constructors returns a
   * dummy model on which the invocation can be performed.
   */
  private <T extends ReflectiveModel<T>> T getInstanceModel(
      MethodExecutionInfo methodExecutionInfo) {
    ReferenceValue callingInstance = methodExecutionInfo.getInstanceOrNullIfStatic();
    boolean isInstanceParticular = callingInstance != null && callingInstance.isParticular();
    T instanceModel;
    // Set up the calling instance, a dummy instance is used for constructors and static methods,
    // as well as when the instance is not particular
    if (methodExecutionInfo.isConstructor()
        || methodExecutionInfo.isStatic()
        || !isInstanceParticular) {
      Class<T> targetClass =
          (Class<T>)
              Objects.requireNonNull(
                  supportedSignatureToModel.get(methodExecutionInfo.getResolvedTargetSignature()),
                  "The target signature has been already checked to be supported");
      instanceModel = ModelHelper.getDummyObject(targetClass);
    } else {
      // Since we are not analyzing a static method the instance has to be not-null
      instanceModel = (T) Objects.requireNonNull(callingInstance).getValue().getModeledValue();
    }
    return instanceModel;
  }

  private MethodResult execute(
      MethodExecutionInfo methodExecutionInfo,
      ValueCalculator valueCalculator,
      Model instanceModel) {
    if (methodExecutionInfo.isConstructor()) {
      return instanceModel.init(methodExecutionInfo, valueCalculator);
    }

    if (methodExecutionInfo.isStatic()) {
      return instanceModel.invokeStatic(methodExecutionInfo, valueCalculator);
    }

    return instanceModel.invoke(methodExecutionInfo, valueCalculator);
  }

  @Override
  public Set<MethodSignature> getSupportedMethodSignatures() {
    return supportedSignatures;
  }

  // Methods for arguments sanity checks

  /**
   * Checks sanity of the instance, represented as a {@link ReferenceValue} object wrapping an
   * {@link AnalyzedObject} object. The sanity checks performed are:
   *
   * <ul>
   *   <li>For static methods, the instance Value can't be non-null
   *   <li>Vice-versa, for instance methods, it must be non-null
   *   <li>For particular instance Values, the underlying object must be non-null
   *   <li>The value must be modeled
   * </ul>
   *
   * @param methodInfo The method execution information.
   * @param supportedSignatureToModel A mapping from the signature of a method supported by the
   *     executor and the model class that should handle that method invocation.
   * @throws IllegalStateException If any of the sanity checks fail.
   */
  private void checkInstanceSanity(
      MethodExecutionInfo methodInfo,
      Map<MethodSignature, Class<? extends ReflectiveModel<?>>> supportedSignatureToModel) {

    ReferenceValue instanceValue = methodInfo.getInstanceOrNullIfStatic();

    // Static methods can always be executed and the instance is expected to not be assigned
    if (methodInfo.isStatic() && instanceValue != null) {
      throw new IllegalStateException("No instance expected for static methods.");
    } else if (methodInfo.isStatic()) {
      // Static methods don't use an instance, hence need no further checking
      return;
    }

    // Vice-versa, for a non-static method, we need a non-null instance Value object
    if (instanceValue == null) {
      throw new IllegalStateException("Unexpected null instance value for an instance method.");
    }

    // The instance is null, which would cause an NPE in the analyzed code, the case is handled
    // explicitly later.
    if (instanceValue.isParticular() && instanceValue.getValue().isNull()) {
      return;
    }

    // Special checks for particular instances
    // Check if the Value is modeled, since this executor only supports models.
    if (instanceValue.isParticular()) {
      ReferenceValue instanceReferenceValue = instanceValue.referenceValue();
      if (!instanceReferenceValue.getValue().isModeled()) {
        throw new IllegalStateException(
            "Should not use ReflectiveModelExecutor on non-model instances, are you sure you are matching the expected executor?");
      }
      checkArgumentCorrectModel(instanceReferenceValue.getValue(), supportedSignatureToModel);
    }
  }

  /**
   * Additional validity checks on instances that the modeled instances themselves cannot perform
   * for lack of information, namely:
   *
   * <ul>
   *   <li>For constructors, if the instance Value is specific, i.e. identified.
   *   <li>For instance methods, whether the instance is null in the analyzed code.
   * </ul>
   *
   * @param methodInfo The method execution information.
   * @return Whether the executor is able to provide a valid result given the instance.
   */
  private boolean canExecuteWithInstance(MethodExecutionInfo methodInfo) {
    if (methodInfo.isStatic()) {
      return true;
    }

    ReferenceValue instanceValue = methodInfo.getInstanceNonStatic();

    if (methodInfo.isConstructor()) {
      return instanceValue.isSpecific();
    }

    // Null instance value would be an NPE, but the analysis doesn't support
    // exceptional paths, so we approximate by continuing the analysis with an unknown value.
    return !instanceValue.isParticular() || !instanceValue.getValue().isNull();
  }

  /**
   * Performs sanity checking on a list of parameters. The sanity checks consists of:
   *
   * <ul>
   *   <li>The length of the parameter list is equal to arguments specified in the descriptor.
   *   <li>For each parameter, the type matches (including inheritance), and
   *   <li>If it is a modeled value, the correct Model implementation is used.
   * </ul>
   *
   * @throws IllegalStateException If any of the above checks fail.
   */
  private void checkParameterSanity(
      MethodExecutionInfo executionInfo,
      Map<MethodSignature, Class<? extends ReflectiveModel<?>>> supportedSignatureToModel) {

    List<Value> parameters = executionInfo.getParameters();
    List<String> parameterTypes = executionInfo.getSignature().descriptor.getArgumentTypes();

    if (parameterTypes.size() != parameters.size()) {
      throw new IllegalStateException(
          "The number of provided parameters is different from the number of parameters specified in the called method signature");
    }

    for (int i = 0; i < parameters.size(); i++) {
      Value parameter = parameters.get(i);
      String staticType = parameterTypes.get(i);
      Clazz parameterClass = executionInfo.getParametersClasses().get(i);

      checkParameter(staticType, parameter, parameterClass, supportedSignatureToModel);
    }
  }

  private void checkParameter(
      String staticType,
      Value parameter,
      Clazz parameterClass,
      Map<MethodSignature, Class<? extends ReflectiveModel<?>>> supportedSignatureToModel) {
    if (ClassUtil.isInternalPrimitiveType(staticType)) {
      if (ClassUtil.internalPrimitiveTypeToComputationalType(staticType)
          != ClassUtil.internalPrimitiveTypeToComputationalType(parameter.internalType())) {
        throw new IllegalStateException(
            String.format(
                "The parameter should be primitive, but the computational type of \"%s\" and \"%s\" do not correspond",
                parameter.internalType(), staticType));
      }
    } else {

      ReferenceValue parameterValue = parameter.referenceValue();
      String parameterType = parameterValue.getType();

      // Null parameter is always valid
      // We also can't check if the parameter has the right type if we have no type information
      if (parameterType == null
          || (parameterValue.isParticular() && parameterValue.getValue().isNull())
          || !(parameterValue instanceof TypedReferenceValue)) {
        return;
      }

      if (isParameterTypeInvalid(parameterType, parameterClass, staticType)) {
        throw new IllegalStateException(
            String.format(
                "Parameter type is \"%s\", which does not match or inherit from "
                    + "the static type \"%s\"",
                parameterType, staticType));
      }

      if (parameterValue.isParticular() && parameterValue.getValue().isModeled()) {
        checkArgumentCorrectModel(parameterValue.getValue(), supportedSignatureToModel);
      }
    }
  }

  /**
   * A parameter is invalid if all of these conditions hold:
   *
   * <ul>
   *   <li>Its type doesn't correspond to the static type in the signature
   *   <li>Does not extend/implement the target Clazz
   * </ul>
   */
  private boolean isParameterTypeInvalid(
      String parameterType, Clazz parameterClass, String staticType) {
    return !parameterType.equals(staticType)
        && parameterClass != null
        && !InitializedClassUtil.isInstanceOf(parameterType, parameterClass);
  }

  private void checkArgumentCorrectModel(
      AnalyzedObject parameterObject,
      Map<MethodSignature, Class<? extends ReflectiveModel<?>>> supportedSignatureToModel) {
    for (Map.Entry<MethodSignature, Class<? extends ReflectiveModel<?>>> entry :
        supportedSignatureToModel.entrySet()) {
      if (isArgumentWrongModel(
          entry.getKey(),
          entry.getValue(),
          parameterObject.getType(),
          parameterObject.getModeledValue())) {
        throw new IllegalArgumentException(
            String.format(
                "The parameter is of the type supported by ReflectiveModelExecutor \"%s\", but the model is of class \"%s\" instead of the model expected by the executor \"%s\"",
                entry.getKey(),
                parameterObject.getModeledValue().getClass(),
                supportedSignatureToModel.get(entry.getKey())));
      }
    }
  }

  private boolean isArgumentWrongModel(
      MethodSignature supportedSignature,
      Class<?> supportedModel,
      @Nullable String parameterType,
      Model parameterModel) {
    return ClassUtil.internalTypeFromClassName(supportedSignature.getClassName())
            .equals(parameterType)
        && !supportedModel.isInstance(parameterModel);
  }

  // Static methods

  /**
   * Computes the methods the executor needs to support for the specified classes. This consists in
   * all the methods implemented by the specified {@link ReflectiveModel} and additionally a
   * signature for each class extending the class if inheritance has to be supported.
   *
   * @param modelInfo information on which classes the executor wants to support (e.g., only the
   *     class modeled by a model or also everything implementing it, etc.)
   * @param hierarchy the class hierarchy.
   * @return the signatures the executor supports for the clazz.
   * @param <T> the model modeling the clazz.
   */
  public static <T extends ReflectiveModel<T>>
      Set<MethodSignature> generateMethodSignaturesForModel(
          SupportedModelInfo<T> modelInfo, HierarchyProvider hierarchy) {

    T dummy = ModelHelper.getDummyObject(modelInfo.modelClass);
    String className = ClassUtil.internalClassNameFromType(dummy.getType());
    Clazz clazz = hierarchy.getClazz(className);

    if (clazz == null) {
      log.info(
          "The class hierarchy does not contain {}, the executor won't be able to execute methods from it",
          className);
      return Collections.emptySet();
    }

    if ((clazz.getAccessFlags() & AccessConstants.INTERFACE) != 0) {
      log.error(
          "This method has been designed for classes, if it needs to be called for an interface explicit support should be added");
      return Collections.emptySet();
    }

    Set<MethodSignature> supportedMethods = new HashSet<>();

    for (MethodInfo methodInfo : ModelHelper.getSupportedMethods(modelInfo.modelClass)) {
      if (modelInfo.supportsFullInheritance) {
        for (String subClassName : hierarchy.getSubClasses(className)) {
          MethodSignature signature =
              new MethodSignature(
                  subClassName, methodInfo.getMethodName(), methodInfo.getDescriptor());
          supportedMethods.add(signature);
        }
      }
      MethodSignature signature =
          new MethodSignature(className, methodInfo.getMethodName(), methodInfo.getDescriptor());
      supportedMethods.add(signature);
    }

    return supportedMethods;
  }

  /**
   * Information provided by an executor to communicate which classes it supports.
   *
   * @param <T> recursive generic type of the model.
   */
  public static final class SupportedModelInfo<T extends ReflectiveModel<T>> {
    private final Class<T> modelClass;
    private final boolean supportsFullInheritance;

    /**
     * @param modelClass a clazz of a model.
     * @param supportsFullInheritance whether the executor should support classes implementing the
     *     modeled class. "Full" support for inheritance means all methods, regardless of the fact
     *     they override the modeled method or not.
     */
    public SupportedModelInfo(Class<T> modelClass, boolean supportsFullInheritance) {
      this.modelClass = modelClass;
      this.supportsFullInheritance = supportsFullInheritance;
    }

    public Class<T> getModelClass() {
      return modelClass;
    }

    public boolean isSupportsFullInheritance() {
      return supportsFullInheritance;
    }
  }

  /** Builder for {@link ReflectiveModelExecutor}. */
  public static class Builder implements Executor.Builder<ReflectiveModelExecutor> {

    private final Set<SupportedModelInfo<? extends ReflectiveModel<?>>> supportedModels =
        new HashSet<>();
    private final HierarchyProvider hierarchy;

    /**
     * Construct the builder.
     *
     * @param hierarchy the class hierarchy.
     */
    public Builder(HierarchyProvider hierarchy) {
      this.hierarchy = hierarchy;
    }

    /**
     * Add a model to support.
     *
     * @param modelInfo information on which model to support and how to support it.
     * @return the builder.
     * @param <T> recursive generic type of the model.
     */
    public <T extends ReflectiveModel<T>> Builder addSupportedModel(
        SupportedModelInfo<T> modelInfo) {
      supportedModels.add(modelInfo);
      return this;
    }

    /**
     * Add a models to support.
     *
     * @param modelInfo information on which models to support and how to support each of them.
     * @return the builder.
     */
    public Builder addSupportedModels(
        Collection<SupportedModelInfo<? extends ReflectiveModel<?>>> modelInfo) {
      supportedModels.addAll(modelInfo);
      return this;
    }

    @Override
    public ReflectiveModelExecutor build() {
      return new ReflectiveModelExecutor(supportedModels, hierarchy);
    }
  }
}
