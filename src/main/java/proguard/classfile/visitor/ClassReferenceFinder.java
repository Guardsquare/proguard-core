package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;

public class ClassReferenceFinder implements ConstantVisitor
{
    private final Clazz referencedClass;
    private boolean classIsReferenced = false;

    public ClassReferenceFinder(Clazz referencedClass)
    {
        this.referencedClass = referencedClass;
    }

    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        if (classConstant.referencedClass != null && classConstant.referencedClass.equals(referencedClass))
        {
            this.classIsReferenced = true;
        }
    }

    public boolean classReferenceFound()
    {
        return this.classIsReferenced;
    }
}
