package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.Utf8Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;

public class DescriptorTypeUpdater implements ConstantVisitor
{
    private final String originalType;
    private final String replacingType;
    public DescriptorTypeUpdater(String originalType, String replacingType)
    {
        this.originalType  = originalType;
        this.replacingType = replacingType;
    }

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant) {
        utf8Constant.setString(utf8Constant.getString().replace(originalType, replacingType));
    }
}
