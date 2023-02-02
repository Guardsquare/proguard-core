public class SimpleStringBuffer
{
    public static void main(String[] args)
    {
        StringBuffer sb = new StringBuffer("Hello");
        sb.append(" ");
        sb.append("World");
        System.out.println(sb.toString());
    }
}
