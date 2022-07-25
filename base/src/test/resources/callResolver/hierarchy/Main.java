import java.util.Arrays;

public class Main
{

    String flag = "";

    public void ambiguous()
    {
        Super s = null;
        switch (flag)
        {
            case "A":
                s = new A();
                break;
            case "B":
                s = new B();
                break;
            default:
                s = null;
        }

        // Target: A#test or B#test, may possibly throw NPE
        s.test();

        // Target: Super#staticTest
        s.staticTest();
    }

    public void a()
    {
        Super s = new A();
        // Target: A#test
        s.test();
    }

    public void b()
    {
        Super s = new B();
        // Target: B#test
        s.test();
    }

    public void s()
    {
        Super s = new Super();
        // Target: Super#test
        s.test();
    }

    public void notOverridden()
    {
        Super s = new NotOverridden();
        // Target: Super#test
        s.test();
    }

    public void external()
    {
        // Target: java.io.PrintStream#println
        System.out.println();
    }

    public void alwaysNull()
    {
        Super s = null;
        // Target: Super#test, will always throw NPE
        s.test();
    }

    public void dynamic()
    {
        Arrays.asList("Red", "Green", "Blue")
              .stream()
              .filter(c -> c.length() > 3) // Target: Main#lambda$dynamic$0
              .count();
    }

    public void invokeInterface()
    {
        NormalImplementor s = new NormalImplementor();

        // Target: SuperInterface#defaultTest
        s.defaultTest();

        // Target: NormalImplementor#abstractTest
        s.abstractTest();
    }

    public void mostSpecificDefaultSub()
    {
        // Target: SubInterface#defaultTest
        new SubImplementor().defaultTest();
    }

    public void mostSpecificDefaultSuper()
    {
        // Target: SubInterface#defaultTest (!)
        new SuperImplementor().defaultTest();
    }

    public static void makeNoise(Vehicle v){
        // Target: Vehicle#honk, Car#honk or Bike#honk
        v.honk();
    }

    public static void makeString(Object v){
        // Target: java.lang.Object#toString (!)
        v.toString();
    }
}
