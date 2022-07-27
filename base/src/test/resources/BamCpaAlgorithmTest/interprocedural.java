class A {
    public static int a;
    public static void main(){
        a = 4;
        int d = B.foo(2); // offset 5 (next instruction offset 8)

        double y = 42.0 + new B().fee(d, 2.0); // offset 23 (next instruction offset 26)
    }
}

class B {
    public static int foo(int b){
        int c = A.a + b;
        return c * 42;
    }

    public double fee(int a, double b)
    {
        return a + b;
    }
}
