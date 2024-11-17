package proguard.classfile.util.inject.argument;

import proguard.classfile.ClassConstants;

/**
 * A model representing a constant string argument to be passed to the method invocation instructions that are
 * injected by {@link proguard.classfile.util.inject.CodeInjector}.
 *
 * @author Kymeng Tang
 */
public class ConstantString implements InjectedArgument {
    private final String constantString;
    public ConstantString(String constant) {
        this.constantString = constant;
    }
    public String getConstant() {
        return this.constantString;
    }

    @Override
    public Object getValue() {
        return constantString;
    }

    @Override
    public String getInternalType() {
        return ClassConstants.TYPE_JAVA_LANG_STRING;
    }

    @Override
    public String toString() {
        return String.format("\"%s\":String", constantString);
    }
}