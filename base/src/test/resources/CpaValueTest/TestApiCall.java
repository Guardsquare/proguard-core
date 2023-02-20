public class TestApiCall
{
   public static void main(String[] args)
   {
       secret();
   }

   private static void secret()
   {
       String key = "yeKIPAyM";
       callAPI(key);
   }

   public static void callAPI(String key)
   {
       StringBuilder reverser = new StringBuilder(key);
       SomeAPI.call(reverser.reverse().toString());
   }
}

class SomeAPI
{
    public static void call(String key)
    {

    }
}