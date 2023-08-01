package proguard.classfile.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;


public class NegativeStackSizeException extends ProguardCoreException
{
    private final Clazz clazz;
    private final Method method;
    private final Instruction instruction;
    private final int instructionOffset;

    public NegativeStackSizeException(Clazz clazz, Method method, Instruction instruction, int instructionOffset)
    {
        super(ErrorId.NEGATIVE_STACK_SIZE, "Stack size becomes negative after instruction %s in [%s.%s%s]",
                instruction.toString(clazz, instructionOffset), clazz.getName(), method.getName(clazz), method.getDescriptor(clazz));
        this.clazz = clazz;
        this.method = method;
        this.instruction = instruction;
        this.instructionOffset = instructionOffset;
    }

    public Clazz getClazz()
    {
        return clazz;
    }

    public Method getMethod()
    {
        return method;
    }

    public Instruction getInstruction()
    {
        return instruction;
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }
}
