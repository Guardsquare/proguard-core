package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.Utf8Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;

public class FieldRenamer implements MemberVisitor, ConstantVisitor {

    private final String newFieldNamePrefix;
    private int newFieldNameIndex;

    public FieldRenamer(String newFieldNamePrefix)
    {
        this.newFieldNamePrefix = newFieldNamePrefix;
        this.newFieldNameIndex  = 0;
    }

    public void resetIndex()
    {
        this.newFieldNameIndex = 0;
    }

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        programClass.constantPoolEntryAccept(programField.u2nameIndex, this);
    }

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        this.newFieldNameIndex++;
        utf8Constant.setString(this.newFieldNamePrefix + this.newFieldNameIndex);
    }

    public String getNextFieldName()
    {
        return this.newFieldNamePrefix + (this.newFieldNameIndex + 1);
    }
}
