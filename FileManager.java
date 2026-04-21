import java.io.*;
import java.util.*;

public class FileManager {

    public static void save(String file, String content) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Table loadTable(String filename) {

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {

            int numCols = Integer.parseInt(br.readLine());

            List<String> cols = new ArrayList<>();
            List<String> types = new ArrayList<>();
            String pk = null;

            for (int i = 0; i < numCols; i++) {
                String[] parts = br.readLine().split(" ");
                cols.add(parts[0]);
                types.add(parts[1]);
            }

            String line = br.readLine();

            if (line.startsWith("PRIMARY")) {
                pk = line.split(" ")[1];
                br.readLine();
            }

            Table t = new Table(filename.replace(".tbl",""), cols, types, pk);

            while ((line = br.readLine()) != null) {
                t.insert(new Record(line.split(",")));
            }

            return t;

        } catch (Exception e) {
            return null;
        }
    }

    public static void executeFile(String file, Parser parser) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line, cmd = "";

            while ((line = br.readLine()) != null) {
                cmd += line + " ";
                if (line.contains(";")) {
                    parser.parse(cmd.trim());
                    cmd = "";
                }
            }
        } catch (Exception e) {
            System.out.println("File error");
        }
    }
}