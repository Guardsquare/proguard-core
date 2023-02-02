public class SimpleStringConcat
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder("Hello");
        sb.append(" ");
        System.out.println(sb.toString() + "World");
    }
}
