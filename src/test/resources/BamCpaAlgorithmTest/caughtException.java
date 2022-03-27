class A {
    public static int a;
    public static void main(boolean isThrown){
        int a = 0;
        try{
            B.foo(isThrown);
            a = 1;
        }
        catch(Exception e){
            a = 2;
        }
    }
}

class B
{
    public static void foo(boolean isThrown)
    {
        if (isThrown)
        {
            throw new NullPointerException();
        }
    }
}
