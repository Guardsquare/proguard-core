package proguard.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.MethodSignature;
import proguard.evaluation.executor.Executor;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.util.HierarchicalWildcardMap;

/** Class for performing lookups of registered executors based on method signatures. */
final class ExecutorLookup {

  private static final Logger log = LogManager.getLogger(ExecutorLookup.class);
  /**
   * Lookup table for the executors. The mapping is from (class name, method name, method
   * descriptor) to an instance of Executor. A null value in the key indicates a wildcard.
   */
  private final HierarchicalWildcardMap<MethodSignature, Executor, Object> executorFromSignature =
      new HierarchicalWildcardMap<>(
          3,
          signature ->
              new Object[] {
                signature.getClassName(), signature.getMethodName(), signature.getDescriptor()
              },
          null);

  /**
   * Constructor
   *
   * @param registeredExecutors A list of executors that the lookup procedures run on.
   */
  public ExecutorLookup(List<Executor> registeredExecutors) {
    for (Executor executor : registeredExecutors) {
      for (MethodSignature signature : executor.getSupportedMethodSignatures()) {
        executorFromSignature.put(signature, executor);
      }
    }
  }

  /**
   * Get list of possible object types from an instance used in a method call. Always returns at
   * least one type.
   *
   * @param object The instance value the invocation was made with.
   * @return A list of its types, ordered from the most specific to the least specific one. The
   *     returned list is never empty.
   */
  private static List<String> getTypesTheGivenInstanceValueIsAssignableTo(@NotNull Value object) {
    if (!(object instanceof ReferenceValue)) {
      log.error(
          "It's impossible to have a non-reference value as an instance object. Defaulting to \"{}\"",
          ClassConstants.NAME_JAVA_LANG_OBJECT);
      return Collections.singletonList(ClassConstants.NAME_JAVA_LANG_OBJECT);
    }

    // try to collect the types by traversing the class hierarchy
    ReferenceValue refValue = ((ReferenceValue) object);
    Clazz clazz = refValue.getReferencedClass();
    List<String> types = new ArrayList<>();
    while (clazz != null) {
      types.add(clazz.getName());
      clazz = clazz.getSuperClass();
    }

    // the above might have failed, so we try different method
    if (types.isEmpty()) {
      if (refValue.getType() != null) {
        return Collections.singletonList(refValue.getType());
      } else {
        log.error("The instance value does not have a referenced class or a type!");
        return Collections.singletonList(ClassConstants.NAME_JAVA_LANG_OBJECT);
      }
    } else {
      return types;
    }
  }

  /**
   * Find and return an executor that handles the given method execution.
   *
   * @param info information about the method invocation
   * @return Executor, if the method can be handled. Null otherwise.
   */
  public @Nullable Executor lookupExecutor(@NotNull MethodExecutionInfo info) {
    // The main problem this method solves is that a variable of type `Object` can contain a
    // `String` and we want to execute e.g. `toString()` with the string executor rather
    // than saying that there is no executor for objects.
    //
    // We do that by first collecting a list of method signatures with varying types, ordered
    // from the most specific type to the least specific (e.g. [String, Object])

    List<MethodSignature> possibleMethodSignatures = new ArrayList<>();
    MethodSignature signature = info.getSignature();
    if (info.isInstanceMethod()) {
      // For instance classes, collect all possible signatures that could be executed with a
      // dynamic dispatch on the particular instance type and the corresponding 'invoke'
      // instructions. Order the list from most specific to most generic
      //
      // As an example, if we have a hierarchy of classes [Object < A < B < C < D], and we
      // called method B.foo() on instance of D, we would create a list [D#foo, C#foo, B#foo]
      for (String type : getTypesTheGivenInstanceValueIsAssignableTo(info.getInstanceNonStatic())) {
        possibleMethodSignatures.add(
            new MethodSignature(type, signature.method, signature.descriptor));
        // we don't want to go lower than in the hierarchy than what the instruction expects
        if (type.equals(signature.getClassName())) break;
      }
    } else {
      // For the remaining invocations - constructors and static methods - always use
      // just the signature in the instruction
      possibleMethodSignatures.add(signature);
    }

    // lookup the executors - complexity O(#possibleMethodSignatures)
    for (MethodSignature possibleSignature : possibleMethodSignatures) {
      Executor result = this.executorFromSignature.get(possibleSignature);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Checks whether there is an executor for the provided method execution info.
   *
   * @param info The method execution info to use for the executor lookup
   * @return True if an executor exists that can handle this
   */
  public boolean hasExecutorFor(@NotNull MethodExecutionInfo info) {
    return this.lookupExecutor(info) != null;
  }

  /**
   * Check, whether the given signature is supported by this executor. Does not resolve types for
   * dynamic dispatch check. Wildcards are honoured. Prefer to use {@link
   * #hasExecutorFor(MethodExecutionInfo)} whenever possible.
   *
   * @param methodSignature The given method signature to check.
   * @return True iff the method can be executed. Sometimes also returns false when it would be
   *     possible to execute.
   */
  public boolean hasExecutorFor(@NotNull MethodSignature methodSignature) {
    return this.executorFromSignature.containsKey(methodSignature);
  }

  /**
   * Checks whether it makes sense to track objects of the given class, i.e. if there exists a
   * method on that class that we are able to execute.
   *
   * @param clazz The class to be tested
   * @return True if instances of that class should be tracked
   */
  public boolean shouldTrackInstancesOfType(@NotNull Clazz clazz) {
    while (clazz != null) {
      MethodSignature signature = new MethodSignature(clazz);
      if (this.executorFromSignature.containsKey((signature))) {
        return true;
      }
      clazz = clazz.getSuperClass();
    }
    return false;
  }

  /**
   * Checks whether it makes sense to track objects of the given class, i.e. if there exists a
   * method on that class that we are able to execute. Prefer to use {@link
   * #shouldTrackInstancesOfType(Clazz)} whenever possible.
   *
   * @param className The class to be tested
   * @return True if instances of that class should be tracked
   */
  public boolean shouldTrackInstancesOfType(@NotNull String className) {
    MethodSignature signature = new MethodSignature(className);
    return this.executorFromSignature.containsKey(signature);
  }
}
