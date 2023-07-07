package proguard.evaluation;

/**
 * Represents an exception when the `PartialEvaluator` encounters a semantically incorect java bytecode instruction.
 * A note can be passed to provide some extra information or possible hints to solve the issue.
 *
 * @see PartialEvaluator
 */
public class InstructionEvaluationException extends RuntimeException{
    private final String note;

    InstructionEvaluationException(String message, String note) {
        super(message);
        this.note = note;
    }

    String getNote() {
        return note;
    }
}
