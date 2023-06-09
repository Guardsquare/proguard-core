package proguard.examples.instrument;

/**
 * Example for {@link DebugStackTop}.
 */
public class DebugStackTopExample
{
    public static void main(String[] args)
    {
        foo(args[0]);
        foo(args[1]);
    }

    public static void foo(String s)
    { }
}
