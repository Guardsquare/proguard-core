class A {
    public static int a;

    public static void main(){
        int c = A.sum(4, 2); // offset 2 (next instruction offset 5)
    }

    public static int sum(int a, int b)
    {
        if (a == 0)
        {
            return b;
        }
        else
        {
            return sum(a - 1, b + 1);
        }
    }
}
