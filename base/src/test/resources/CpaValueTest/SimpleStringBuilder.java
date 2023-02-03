public class SimpleStringBuilder
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder("Hello");
        sb.append(" ");
        sb.append("World");
        System.out.println(sb.toString());
    }
}
