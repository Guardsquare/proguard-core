package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.ClassVisitor;

public class ClassConstantToClassVisitor implements ConstantVisitor {

    private final ClassVisitor classVisitor;

    public ClassConstantToClassVisitor(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
    }

    @Override
    public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
        if (this.classVisitor != null)
        {
            classConstant.referencedClass.accept(this.classVisitor);
        }
    }
}
