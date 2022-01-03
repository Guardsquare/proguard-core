public class Car extends Vehicle
{
    private String modelName = "Mustang";
    @Override
    public void honk()
    {
        System.out.println("Tuut, Car!");
    }
}