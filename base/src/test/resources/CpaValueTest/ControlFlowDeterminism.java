import java.util.Objects;

public class ControlFlowDeterminism {

  public static void main(String[] args) {
    String input1 = args[1];
    String input2 = args[2];
    try {
      for (int j = 0; j < 2; j++) {
        if (Objects.nonNull(input1) && !input1.isEmpty()) {
          switch (input2) {
            case "Option1":
              int a = 24;
              break;
            case "Option2":
              throw new NullPointerException("NPE");
            default:
              break;
          }
        } else {
          System.out.println("Input2 is empty");
        }
        if ("throwException".equals(input1)) {
          throw new IllegalArgumentException("Exception thrown intentionally");
        }
      }
    } catch (NullPointerException e) {
      System.out.println("NullPointerException: " + e.getMessage());
    } finally {
      System.out.println("Finally block");
    }
  }
}