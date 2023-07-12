package proguard.exception;

/**
 * Class holding all the error ids for exceptions occurring in the program.
 */
public final class ErrorId {
    // this id is for testing purposes, to be removed at the end of T20409
    public static int TEST_ID= 99;

    // Partial evaluator exceptions: Range 1000 - 2000
    public static int VARIABLE_EMPTY_SLOT = 1_000;
    public static int VARIABLE_INDEX_OUT_OF_BOUND = 1_001;
    public static int VARIABLE_TYPE = 1_002;

    /**
     * Private constructor to prevent instantiation of the class.
     */
    private ErrorId() {}
}
