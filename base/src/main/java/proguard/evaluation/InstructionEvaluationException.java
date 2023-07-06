package proguard.evaluation;

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
