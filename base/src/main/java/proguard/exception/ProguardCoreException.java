package proguard.exception;

import java.util.List;

public class ProguardCoreException extends RuntimeException
{

    /**
     * A unique id identifying the error (or group of errors).
     */
    private int          componentErrorId;

    /**
     * Information related to the error.
     */
    private List<String> errorParameters;


    /**
     * Overload of {@link ProguardCoreException#ProguardCoreException(String, int, List, Throwable)}} without throwable.
     */
    public ProguardCoreException(String message, int componentErrorId, List<String> errorParameters)
    {
        this(message, componentErrorId, errorParameters, null);
    }

    /**
     * <b>Main constructor, all other constructors need to call this one in order to do common things (formating string for instance).</b>
     * Same as {@link ProguardCoreException#ProguardCoreException(String, int, List)}
     * but takes a Throwable argument to initialize the cause.
     */
    public ProguardCoreException(String message, int componentErrorId, List<String> errorParameters, Throwable cause)
    {
        super(String.format(message, errorParameters), cause);

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
    public List<String> getErrorParameters()
    {
        return errorParameters;
    }
}
