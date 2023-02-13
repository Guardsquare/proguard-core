public class SimpleSwitch
{
    public static void main(String[] args)
    {
        String s;
        switch (args.length) {
            case 0: s = "Hello World"; break;
            case 1: s = "Foo"; break;
            case 2: s = "Bar"; break;
            default: s = "Baz"; break;
        }
        System.out.println(s);
    }
}
