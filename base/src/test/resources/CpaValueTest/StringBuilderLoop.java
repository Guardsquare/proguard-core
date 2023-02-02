public class StringBuilderLoop
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder();
        int t = args.length;
        while (t  > 0){
            sb.append("x");
        }
        System.out.println(sb.toString());
    }
}
