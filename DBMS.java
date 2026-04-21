import java.util.Scanner;

public class DBMS {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Parser parser = new Parser();

        System.out.println("Mini DBMS Started");

        while (true) {
            System.out.print("> ");
            String input = "";

            while (!input.trim().endsWith(";")) {
                input += scanner.nextLine() + " ";
            }

            if (input.equalsIgnoreCase("EXIT;")) {
                parser.saveAll();
                System.out.println("Saved. Exiting...");
                break;
            }

            parser.parse(input.trim());
        }

        scanner.close();
    }
}