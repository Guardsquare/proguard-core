public class SimpleStringBuilder
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder("Hello");
        sb.append(" ");
        sb.append("World");
        foo(sb.toString());
    }

    public static void foo(String s) {
        callApi(s);
    }

    public static void callApi(String s) {
        System.out.println(s);
    }
}
