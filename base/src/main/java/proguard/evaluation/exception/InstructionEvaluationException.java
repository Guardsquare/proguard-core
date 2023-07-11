package proguard.evaluation.exception;

import proguard.evaluation.PartialEvaluator;
import proguard.exception.ProguardCoreException;

import java.util.Collections;

/**
 * Represents an exception when the `PartialEvaluator` encounters a semantically incorrect java bytecode instruction.
 *
 * @see PartialEvaluator
 */
public class InstructionEvaluationException extends ProguardCoreException
{
    public InstructionEvaluationException(String genericMessage)
    {
        super(genericMessage, 4, Collections.emptyList());
    }
}
