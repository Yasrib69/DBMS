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

            input = input.trim();

            if (input.toUpperCase().startsWith("EXIT")) {
                parser.saveAll();
                System.out.println("Saved. Exiting...");
                break;
            }

            parser.parse(input);
        }

        scanner.close();
    }
}