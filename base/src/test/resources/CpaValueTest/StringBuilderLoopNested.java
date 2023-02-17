public class StringBuilderLoopNested
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder("HEllo");
        int t = args.length;
        while (t  > 0){
            sb.append("y");
            while (t > 5) {
                sb.append("x");
            }
        }
        System.out.println(sb.toString());
    }
}
