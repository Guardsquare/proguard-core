public class StringBuilderBranch
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder();
        if (System.currentTimeMillis() > 0) {
            sb.append("x");
        } else {
            sb.append("y");
        }
        System.out.println(sb.toString());
    }
}
