package proguard.classfile.util.inject.location;

import proguard.classfile.ClassConstants;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.util.ConstructorInvocationOffsetFinder;

/**
 * An implementation of the InjectStrategy interface to find the earliest location suitable for injecting method
 * invocation instructions.
 */
public class FirstBlock implements InjectStrategy {
    ConstructorInvocationOffsetFinder offsetFinder = new ConstructorInvocationOffsetFinder();

    /**
     * Find the first offset to inject a method invocation. If the target method is a constructor, the offset is right
     * after the `invokespecial` to the constructor of the current class or its super class.
     * @param targetClass The class holding the method in which a method invocation shall be injected into.
     * @param targetMethod the target method to have a method invocation injected into.
     * @return An InjectLocation instance indicating the first offset suitable for injection.
     */
    @Override
    public InjectLocation getSingleInjectionLocation(ProgramClass targetClass, ProgramMethod targetMethod) {
        boolean isConstructor = targetMethod.getName(targetClass).equals(ClassConstants.METHOD_NAME_INIT);
        if (isConstructor) {
            targetMethod.accept(targetClass, offsetFinder);
        }
        return isConstructor
            ? new InjectLocation(offsetFinder.getConstructorCallOffset(), false)
            : new InjectLocation(0, true);
    }

    /**
     * Find the first offset to inject a method invocation. If the target method is a constructor, the offset is right
     * after the `invokespecial` to the constructor of the current class or its super class.
     * @param targetClass The class holding the method in which a method invocation shall be injected into.
     * @param targetMethod the target method to have a method invocation injected into.
     * @return An array of one InjectLocation instance indicating the first offset suitable for injection.
     */
    @Override
    public InjectLocation[] getAllSuitableInjectionLocation(ProgramClass targetClass, ProgramMethod targetMethod) {
        return new InjectLocation[] {getSingleInjectionLocation(targetClass, targetMethod)};
    }
}
