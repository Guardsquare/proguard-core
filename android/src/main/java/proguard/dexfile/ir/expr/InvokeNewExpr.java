package proguard.dexfile.ir.expr;

import proguard.dexfile.ir.LabelAndLocalMapper;
import proguard.dexfile.reader.Method;

/**
 * Represents an IR instructor equivalent to Java bytecode: `new X, invoke-special X.<init>`
 * <p>
 * It's possible to construct an {@link InvokeNewExpr} with either
 * just the owner or with an owner and a class name.
 * <p>
 * It's possible that these are different:
 * <p>
 *     <code>
 *         new Foo <-- className
 *         invokespecial java/lang/Object <-- owner
 *     </code>
 * </p>
 */
public class InvokeNewExpr extends InvokeExpr
{
    private final String className;

    public InvokeNewExpr(VT type, Value[] args, String ownerType, String methodName, String[] argumentTypes,
                         String owner)
    {
        this(type, args, ownerType, methodName, argumentTypes, owner, owner);
    }

    public InvokeNewExpr(VT type, Value[] args, String ownerType, String methodName, String[] argumentTypes,
                         String owner, String className)
    {
        super(type, args, ownerType, methodName, argumentTypes, owner);
        this.className = className;
    }

    private InvokeNewExpr(VT type, Value[] args, Method method, String className) {
        super(type, args, method);
        this.className = className;
    }

    public String getClassName()
    {
        return className;
    }

    @Override
    public InvokeExpr clone() {
        return new InvokeNewExpr(vt, cloneOps(), method, className);
    }

    @Override
    public InvokeExpr clone(LabelAndLocalMapper mapper) {
        return new InvokeNewExpr(vt, cloneOps(mapper), method, className);
    }
}
