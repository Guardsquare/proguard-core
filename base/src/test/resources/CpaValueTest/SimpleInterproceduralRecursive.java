public class SimpleInterproceduralRecursive
{
    public static void main(String[] args)
    {
        String s = "Hello World";
        s = foo(s);
    }

    public static String foo(String s) {
        if (s.length() > 100) {
            return s;
        }
        return foo(s + "x");
    }
}