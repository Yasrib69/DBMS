import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    public static void saveRaw(String filePath, String content) {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveTable(String currentDB, String tableName, String content) {
        String dir = (currentDB == null || currentDB.isEmpty()) ? "." : currentDB;
        new File(dir).mkdirs();
        saveRaw(dir + File.separator + tableName + ".tbl", content);
    }

    public static void saveIndex(String currentDB, String tableName, List<Integer> keys) {
        String dir = (currentDB == null || currentDB.isEmpty()) ? "." : currentDB;
        new File(dir).mkdirs();
        StringBuilder sb = new StringBuilder();
        for (Integer key : keys) {
            sb.append(key).append("\n");
        }
        saveRaw(dir + File.separator + tableName + ".idx", sb.toString());
    }

    public static void deleteTableFiles(String currentDB, String tableName) {
        String dir = (currentDB == null || currentDB.isEmpty()) ? "." : currentDB;
        new File(dir + File.separator + tableName + ".tbl").delete();
        new File(dir + File.separator + tableName + ".idx").delete();
    }

    public static Table loadTable(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            int numCols = Integer.parseInt(br.readLine());
            List<String> cols = new ArrayList<>();
            List<String> types = new ArrayList<>();
            String pk = null;

            for (int i = 0; i < numCols; i++) {
                String[] parts = br.readLine().split(" ", 2);
                cols.add(parts[0]);
                types.add(parts[1]);
            }

            String line = br.readLine();
            if (line != null && line.startsWith("PRIMARY ")) {
                pk = line.substring(8).trim();
                line = br.readLine();
            }

            String fileName = new File(path).getName();
            String tableName = fileName.endsWith(".tbl") ? fileName.substring(0, fileName.length() - 4) : fileName;
            Table t = new Table(tableName, cols, types, pk);

            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    t.insert(new Record(line.split(",", -1)));
                }
            }
            return t;
        } catch (Exception e) {
            return null;
        }
    }

    public static void executeFile(String file, Parser parser) {
        executeFile(file, null, parser);
    }

    public static void executeFile(String file, String outputFile, Parser parser) {
        PrintStream original = System.out;
        PrintStream redirected = null;
        try {
            if (outputFile != null && !outputFile.isEmpty()) {
                redirected = new PrintStream(outputFile);
                System.setOut(redirected);
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                StringBuilder cmd = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    cmd.append(line).append(" ");
                    if (line.contains(";")) {
                        parser.parse(cmd.toString().trim());
                        cmd.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error: File not found: " + file);
        } finally {
            if (redirected != null) redirected.close();
            System.setOut(original);
        }
    }
}