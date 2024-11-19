package proguard.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.executor.Executor;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.TypedReferenceValue;

/**
 * Class for performing lookups of registered executors based on method signatures.
 *
 * <p>Dynamic lookup is performed for instance methods by using the analyzed type, if available,
 * instead of the static type of the target method.
 *
 * <p>Executors are expected to provide exactly all the methods they expect to be able to support
 * via {@link Executor#getSupportedMethodSignatures()}, for example to support inheritance the
 * executors need to specify both the parent and child class in the returned signatures.
 */
final class ExecutorLookup {

  private static final Logger log = LogManager.getLogger(ExecutorLookup.class);
  private static final boolean PRINT_ERRORS =
      System.getProperty("proguard.value.logerrors") != null;
  private final Map<MethodSignature, Executor> executorFromSignature = new HashMap<>();

  private final Set<String> supportedClasses = new HashSet<>();

  /**
   * Constructor
   *
   * @param registeredExecutors A list of executors that the lookup procedures run on.
   */
  public ExecutorLookup(List<Executor> registeredExecutors) {
    for (Executor executor : registeredExecutors) {
      for (MethodSignature signature : executor.getSupportedMethodSignatures()) {
        if (PRINT_ERRORS && signature.isIncomplete()) {
          log.warn(
              "Wildcard signatures are not supported by ExecutorLookup, they will get ignored");
          continue;
        }

        if (executorFromSignature.putIfAbsent(signature, executor) != null) {
          if (PRINT_ERRORS)
            log.warn(
                "Signature {} is supported by multiple executors. {} will be ignored",
                signature,
                executor.getClass().getSimpleName());
        } else {
          supportedClasses.add(signature.getClassName());
        }
      }
    }
  }

  /**
   * Find and return an executor that handles the given method execution.
   *
   * @param info information about the method invocation
   * @return Executor, if the method can be handled. Null otherwise.
   */
  public @Nullable Executor lookupExecutor(@NotNull MethodExecutionInfo info) {
    MethodSignature targetSignature;
    MethodSignature staticSignature = info.getSignature();
    boolean isTargetDynamic = false;

    if (info.isInstanceMethod()
        && (info.getInstanceNonStatic() instanceof TypedReferenceValue)
        && info.getInstanceNonStatic().getType() != null) {
      // Try to perform a "dynamic" lookup for instance methods if additional type information is
      // available
      isTargetDynamic = true;
      TypedReferenceValue instance = (TypedReferenceValue) info.getInstanceNonStatic();
      targetSignature =
          new MethodSignature(
              ClassUtil.internalClassNameFromClassType(instance.getType()),
              staticSignature.method,
              staticSignature.descriptor);
    } else {
      // For the remaining invocations just use the static signature
      targetSignature = staticSignature;
    }

    info.setResolvedTargetSignature(targetSignature);

    Executor targetExecutor = executorFromSignature.get(targetSignature);

    if (PRINT_ERRORS
        && isTargetDynamic
        && targetExecutor == null
        && executorFromSignature.get(staticSignature) != null) {
      log.warn(
          "Dynamic target {} is not supported by the executors but static target {} is, check if your executor should also support the child class methods",
          targetSignature,
          staticSignature);
    }

    return targetExecutor;
  }

  /**
   * Check, whether the given signature is supported by this executor.
   *
   * <p>NB: inheritance is not taken into account if not explicitly specified. i.e., if a certain
   * method is supported it does not necessarily mean that the same method from a child class is
   * supported, even if the method was not overridden.
   *
   * @param methodSignature The given method signature to check.
   * @return True iff the method can be executed.
   */
  public boolean hasExecutorFor(@NotNull MethodSignature methodSignature) {
    return this.executorFromSignature.containsKey(methodSignature);
  }

  /**
   * Checks whether it makes sense to track objects of the given class, i.e. if there exists a
   * method on that class that we are able to execute.
   *
   * <p>NB: inheritance is not taken into account if not explicitly specified. i.e., if an object of
   * a certain type is supported it does not mean that objects of its child classes are.
   *
   * @param clazz The class to be tested
   * @return True if instances of that class should be tracked
   */
  public boolean shouldTrackInstancesOf(@NotNull Clazz clazz) {
    return shouldTrackInstancesOf(clazz.getName());
  }

  /**
   * Checks whether it makes sense to track objects of the given class, i.e. if there exists a
   * method on that class that we are able to execute.
   *
   * <p>NB: inheritance is not taken into account if not explicitly specified. i.e., if an object of
   * a certain type is supported it does not mean that objects of its child classes are.
   *
   * @param className The class to be tested
   * @return True if instances of that class should be tracked
   */
  public boolean shouldTrackInstancesOf(@NotNull String className) {
    return supportedClasses.contains(className);
  }
}
