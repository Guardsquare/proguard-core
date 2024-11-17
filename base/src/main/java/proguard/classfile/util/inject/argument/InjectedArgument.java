package proguard.classfile.util.inject.argument;

/**
 * This interface defines api for modeling arguments to be passed to the method invocation instructions that are
 * injected by {@link proguard.classfile.util.inject.CodeInjector}.
 *
 * @author Kymeng Tang
 */
public interface InjectedArgument {
    // A getter that returns a boxed value of the argument.
    Object getValue();

    // A getter indicating the internal JVM type that describes the argument.
    String getInternalType();
}