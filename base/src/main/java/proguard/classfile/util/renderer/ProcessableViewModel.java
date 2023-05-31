package proguard.classfile.util.renderer;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import proguard.util.ProcessingFlags;

/**
 * A base ViewModel class for implementing classes of the Processable interface.
 * This class takes care of rendering the processing flags.
 *
 * @author Kymeng Tang
 */
public abstract class ProcessableViewModel
{
    private static final HashMap<Integer, String> PROCESSING_FLAG_MAP = new HashMap<>();
    private static final HashSet<Class<?>>        KNOWN_FLAG_SET      = new HashSet<>();

    protected List<String> processingFlags;
    protected Object       processingInfo;

    static
    {
        addExtraProcessingFlags(ProcessingFlags.class);
    }

    /**
     * A utility method for rendering the processing flags of a processable instance.
     *
     * @param processingFlags   The processing flags of a processable to be rendered.
     * @return                  A list of strings containing rendered flags.
     */
    protected static List<String> renderProcessingFlags(int processingFlags)
    {
        List<String> renderedFlags = new ArrayList<>();
        for (int key : PROCESSING_FLAG_MAP.keySet())
        {
            if ((processingFlags & key) != 0)
            {
                renderedFlags.add(PROCESSING_FLAG_MAP.get(key));
            }
        }
        return renderedFlags;
    }

    /**
     * A utility method that allows for adding additional processing flags that can be rendered as strings.
     * @param extraProcessingFlagsHolder    A sub class of ProcessingFlags that holds the additional flags.
     */
    protected static <T extends ProcessingFlags> void addExtraProcessingFlags(Class<T> extraProcessingFlagsHolder)
    {
        if (KNOWN_FLAG_SET.contains(extraProcessingFlagsHolder))
            return;

        KNOWN_FLAG_SET.add(extraProcessingFlagsHolder);
        Arrays.stream(extraProcessingFlagsHolder.getDeclaredFields()).forEach( field ->
        {
            try
            {
                if (Modifier.isStatic(field.getModifiers()))
                {
                    field.setAccessible(true);
                    PROCESSING_FLAG_MAP.put(field.getInt(null), field.getName());
                }
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}
