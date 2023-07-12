package proguard.exception;

public class ProguardCoreException extends RuntimeException
{

    /**
     * A unique id identifying the error (or group of errors).
     */
    private final int componentErrorId;

    /**
     * Information related to the error.
     */
    private final String[] errorParameters;


    /**
     * Overload of {@link ProguardCoreException#ProguardCoreException(int, Throwable, String, String...)}} without throwable.
     */
    public ProguardCoreException(int componentErrorId, String message,  String... errorParameters)
    {
        this(componentErrorId, null, message, errorParameters);
    }

    /**
     * <b>Main constructor, all other constructors need to call this one in order to do common things (formating string for instance).</b>
     * Same as {@link ProguardCoreException#ProguardCoreException(int, String,  String...)}
     * but takes a Throwable argument to initialize the cause.
     */
    public ProguardCoreException(int componentErrorId, Throwable cause, String message, String... errorParameters)
    {
        super(String.format(message, (Object[]) errorParameters), cause);

        this.componentErrorId = componentErrorId;
        this.errorParameters  = errorParameters;
    }



    /**
     * Returns the id for the error (exception).
     */
    public int getComponentErrorId()
    {
        return componentErrorId;
    }


    /**
     * Returns the list of information related to this error.
     */
    public String[] getErrorParameters()
    {
        return errorParameters;
    }
}
