import java.util.Scanner;

public class DBMS {
    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {

            Parser parser = new Parser();

            System.out.println("Mini DBMS Started. Type commands ending with ';'. Type EXIT; to quit.");

            while (true) {
                System.out.print("> ");

                StringBuilder input = new StringBuilder();
                while (!input.toString().trim().endsWith(";")) {
                    if (!scanner.hasNextLine()) {
                        parser.saveAll();
                        return;
                    }
                    input.append(scanner.nextLine()).append(" ");
                }

                String command = input.toString().trim();

                if (command.equalsIgnoreCase("EXIT;")) {
                    parser.saveAll();
                    System.out.println("Saved. Exiting...");
                    break;
                }

                parser.parse(command);
            }
        }
    }
}