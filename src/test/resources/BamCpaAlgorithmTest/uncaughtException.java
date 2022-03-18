class A {
    public static int a;
    public static void main(){
        B.foo();
    }
}

class B
{
    public static void foo()
    {
        throw new NullPointerException();
    }
}
