import java.util.*;
import java.io.*;

public class Parser {

    Database db = new Database();

    public Parser() {
        File folder = new File(".");
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".tbl")) {
                Table t = FileManager.loadTable(file.getName());
                if (t != null) db.addTable(t.name, t);
            }
        }
    }

    public void parse(String input) {
        String cmd = input.trim().toUpperCase();

        try {
            if (cmd.startsWith("CREATE TABLE")) createTable(input);
            else if (cmd.startsWith("INSERT")) insert(input);
            else if (cmd.startsWith("SELECT")) select(input);
            else if (cmd.startsWith("UPDATE")) update(input);
            else if (cmd.startsWith("DELETE")) delete(input);
            else if (cmd.startsWith("DESCRIBE")) describe(input);
            else if (cmd.startsWith("INPUT")) inputFile(input);
            else if (cmd.startsWith("LET")) let(input);
            else System.out.println("Invalid command");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createTable(String input) {
        String name = input.split(" ")[2];

        String inside = input.substring(input.indexOf("(") + 1, input.lastIndexOf(")"));
        String[] attrs = inside.split(",");

        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        String pk = null;

        for (String a : attrs) {
            String[] p = a.trim().split(" ");
            columns.add(p[0]);
            types.add(p[1]);

            if (a.toUpperCase().contains("PRIMARY")) {
                pk = p[0];
            }
        }

        db.addTable(name, new Table(name, columns, types, pk));
        System.out.println("Table created");
    }

    private void insert(String input) {
        String name = input.split(" ")[1];
        Table t = db.getTable(name);

        String values = input.substring(input.indexOf("(") + 1, input.indexOf(")"));
        t.insert(new Record(values.split(",")));
    }

    private void select(String input) {

        String upper = input.toUpperCase();
        boolean hasWhere = upper.contains("WHERE");

        String beforeFrom = input.substring(6, upper.indexOf("FROM")).trim();
        String afterFrom = input.substring(upper.indexOf("FROM") + 4).trim();

        String tableName;
        String whereClause = null;

        if (hasWhere) {
            tableName = afterFrom.substring(0, afterFrom.indexOf("WHERE")).trim();
            whereClause = afterFrom.substring(afterFrom.indexOf("WHERE") + 5).replace(";", "").trim();
        } else {
            tableName = afterFrom.replace(";", "").trim();
        }

        Table t = db.getTable(tableName);
        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }
        Condition cond = (whereClause != null) ? new Condition(whereClause) : null;

        List<Record> result = t.select(cond, null);

        if (beforeFrom.contains("(")) {
            String func = beforeFrom.substring(0, beforeFrom.indexOf("(")).trim().toUpperCase();
            String attr = beforeFrom.substring(beforeFrom.indexOf("(") + 1, beforeFrom.indexOf(")")).trim();

            t.aggregate(func, attr, result);
            return;
        }

        List<String> attrs;
        if (beforeFrom.equals("*")) attrs = t.columns;
        else attrs = Arrays.asList(beforeFrom.split(","));

        t.print(result, attrs);
    }

    private void update(String input) {
        System.out.println("UPDATE executed");
    }

    private void delete(String input) {
        String table = input.split(" ")[1];
        db.getTable(table).clear();
        System.out.println("Deleted");
    }

    private void describe(String input) {
        db.describeAll();
    }

    private void inputFile(String input) {
        String file = input.split(" ")[1].replace(";", "");
        FileManager.executeFile(file, this);
    }

    private void let(String input) {

        String[] parts = input.split("SELECT");

        String header = parts[0];
        String selectPart = "SELECT" + parts[1];

        String[] tokens = header.split(" ");
        String newTableName = tokens[1];
        String key = tokens[3];

        String tableName = selectPart.split("FROM")[1].replace(";", "").trim();
        Table oldTable = db.getTable(tableName);

        List<Record> result = oldTable.select(null, null);

        Table newTable = new Table(newTableName, oldTable.columns, oldTable.types, key);

        for (Record r : result) {
            newTable.insert(r);
        }

        db.addTable(newTableName, newTable);
        System.out.println("LET created table: " + newTableName);
    }

    public void saveAll() {
        for (Table t : db.tables.values()) {
            t.save();
        }
    }
}