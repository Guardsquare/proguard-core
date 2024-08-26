package proguard.analysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.constant.AnyMethodrefConstant;

/** Utility methods for call resolution. */
public class CallUtil {
  private CallUtil() {}

  /**
   * The <code>invokevirtual</code> and <code>invokeinterface</code> resolution algorithm, annotated
   * with <a
   * href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokevirtual">JVM
   * spec §6.5.invokevirtual</a> citations where appropriate, so that the specified lookup process
   * can easily be compared to this implementation.
   *
   * @param callingClass JVM spec: "current class".
   * @param thisPointerType The type of the <code>this</code> pointer of the call (JVM spec:
   *     "objectref").
   * @param ref The {@link AnyMethodrefConstant} specifying name and descriptor of the method to be
   *     invoked.
   * @return The fully qualified names of potential call target clases (usually just one, but see
   *     {@link #resolveFromSuperinterfaces(Clazz, String, String)} for details on when there might
   *     be multiple).
   */
  public static Set<String> resolveVirtual(
      Clazz callingClass, Clazz thisPointerType, AnyMethodrefConstant ref) {
    return resolveVirtual(thisPointerType, ref.getName(callingClass), ref.getType(callingClass));
  }

  /**
   * The <code>invokevirtual</code> and <code>invokeinterface</code> resolution algorithm, annotated
   * with <a
   * href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokevirtual">JVM
   * spec §6.5.invokevirtual</a> citations where appropriate, so that the specified lookup process
   * can easily be compared to this implementation.
   *
   * @param thisPointerType The type of the <code>this</code> pointer of the call (JVM spec:
   *     "objectref").
   * @param methodName The name of the invoked method.
   * @param descriptor The descriptor of the invoked method.
   * @return The fully qualified names of potential call target clases (usually just one, but see
   *     {@link #resolveFromSuperinterfaces(Clazz, String, String)} for details on when there might
   *     be multiple).
   */
  public static Set<String> resolveVirtual(
      Clazz thisPointerType, String methodName, String descriptor) {
    if (thisPointerType == null) {
      return Collections.emptySet();
    }

    // 1. + 2. (Search the class belonging to the this pointer type and all its transitive
    // superclasses)
    return resolveFromSuperclasses(thisPointerType, methodName, descriptor)
        .map(Collections::singleton)
        // 3. (Otherwise find maximally specific method from superinterfaces)
        .orElseGet(() -> resolveFromSuperinterfaces(thisPointerType, methodName, descriptor));
  }

  /**
   * Adapter of {@link CallUtil#resolveVirtual(Clazz, String, String)} returning {@link
   * MethodSignature}.
   *
   * @param thisPointerType The type of the <code>this</code> pointer of the call (JVM spec:
   *     "objectref").
   * @param methodName The name of the invoked method.
   * @param descriptor The descriptor of the invoked method.
   * @return The {@link MethodSignature}s of potential call target (usually just one, but see {@link
   *     #resolveFromSuperinterfaces(Clazz, String, String)} for details on when there might be
   *     multiple).
   */
  public static Set<MethodSignature> resolveVirtualSignatures(
      Clazz thisPointerType, String methodName, String descriptor) {
    return resolveVirtual(thisPointerType, methodName, descriptor).stream()
        .map(className -> new MethodSignature(className, methodName, descriptor))
        .collect(Collectors.toSet());
  }

  /**
   * Search for the invocation target in a specific class and recursively in all superclasses.
   *
   * @param start The {@link Clazz} where the lookup is to be started.
   * @param name The name of the method.
   * @param descriptor The method descriptor.
   * @return An {@link Optional} with the fully qualified name of the class containing the target
   *     method, empty if it couldn't be found.
   */
  public static Optional<String> resolveFromSuperclasses(
      Clazz start, String name, String descriptor) {
    Clazz curr = start;
    while (curr != null) {
      Method targetMethod = curr.findMethod(name, descriptor);
      if (targetMethod != null && (targetMethod.getAccessFlags() & AccessConstants.ABSTRACT) == 0) {
        return Optional.of(curr.getName());
      }

      curr = curr.getSuperClass();
    }
    return Optional.empty();
  }

  /**
   * Search for a maximally specific default implementation in all superinterfaces of a class. This
   * step is potentially unintuitive and difficult to grasp, see <a
   * href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-5.html#jvms-5.4.3.3">JVM spec
   * §5.4.3.3</a> for more information, as well as this great <a
   * href="https://jvilk.com/blog/java-8-wtf-ambiguous-method-lookup/">blog post</a> concerning the
   * resolution pitfalls. The following is based on the information on those websites.
   *
   * @param start The {@link Clazz} whose superinterfaces are to be searched.
   * @param name The target method name.
   * @param descriptor The target method descriptor.
   * @return The fully qualified name of the class(es) that contain the method to be invoked. Be
   *     aware that purely from a JVM point of view, this choice can be ambiguous, in which case it
   *     just chooses the candidate randomly. Here, we don't want to gamble, but rather want to add
   *     call graph edges for every possibility, if this ever happens. Javac ensures that such a
   *     case never occurs, but who knows how the bytecode has been generated, so this possibility
   *     is implemented just in case.
   */
  public static Set<String> resolveFromSuperinterfaces(
      Clazz start, String name, String descriptor) {
    Set<Clazz> superInterfaces = new HashSet<>();
    getSuperinterfaces(start, superInterfaces);
    // Get all transitive superinterfaces that have a matching method.
    Set<Clazz> applicableInterfaces =
        superInterfaces.stream()
            .filter(
                i -> {
                  Method m = i.findMethod(name, descriptor);
                  return m != null
                      && (m.getAccessFlags()
                              & (AccessConstants.PRIVATE
                                  | AccessConstants.STATIC
                                  | AccessConstants.ABSTRACT))
                          == 0;
                })
            .collect(Collectors.toSet());

    // Tricky part: Find the "maximally specific" implementation,
    // i.e. the lowest applicable interface in the type hierarchy.
    for (Clazz iface : new HashSet<>(applicableInterfaces)) {
      superInterfaces.clear();
      getSuperinterfaces(iface, superInterfaces);
      // If an applicable interface overrides another applicable interface, it is more specific than
      // the
      // one being overridden -> the overridden interface is no longer applicable.
      superInterfaces.forEach(applicableInterfaces::remove);
    }

    return applicableInterfaces.stream().map(Clazz::getName).collect(Collectors.toSet());
  }

  /**
   * Get the transitive superinterfaces of a class/interface recursively.
   *
   * @param start The {@link Clazz} where the collection process is to be started.
   * @param accumulator The current set of superinterfaces, so that only one set is constructed at
   *     runtime.
   */
  public static void getSuperinterfaces(Clazz start, Set<Clazz> accumulator) {
    for (int i = 0; i < start.getInterfaceCount(); i++) {
      Clazz iface = start.getInterface(i);
      if (iface == null) {
        Metrics.increaseCount(Metrics.MetricType.MISSING_CLASS);
        continue;
      }
      accumulator.add(iface);
      getSuperinterfaces(iface, accumulator);
    }
    if (start.getSuperClass() != null) {
      getSuperinterfaces(start.getSuperClass(), accumulator);
    }
  }
}
