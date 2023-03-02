public class StringBuilderLoopReassign
{
    public static void main(String[] args)
    {
        StringBuilder sb = new StringBuilder();
        int t = args.length;
        while (t  > 0){
            sb = new StringBuilder();
        }
        System.out.println(sb.toString());
    }
}
