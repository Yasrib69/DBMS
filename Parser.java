import java.io.*;
import java.util.*;

public class Parser {

    private List<Token> tokens;
    private int current = 0;

    Database db = new Database();

    public Parser() {
        File folder = new File(".");
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".tbl")) {
                    Table t = FileManager.loadTable(file.getName());
                    if (t != null) db.addTable(t.name, t);
                }
            }
        }
    }

    private Token peek() {
        if (tokens == null || tokens.isEmpty()) {
            throw new RuntimeException("No tokens available");
        }
        if (current >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(current);
    }

    private Token advance() {
        if (tokens == null || tokens.isEmpty()) {
            throw new RuntimeException("No tokens available");
        }
        if (current < tokens.size()) current++;
        return tokens.get(Math.max(0, current - 1));
    }

    private boolean match(String val) {
        if (peek().value.equalsIgnoreCase(val)) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(String val) {
        if (!match(val)) {
            throw new RuntimeException("Expected " + val + " but found " + peek().value);
        }
    }

    private String expectIdentifier() {
        Token t = peek();
        if (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD) {
            advance();
            return t.value;
        }
        throw new RuntimeException("Expected identifier but found " + t.value);
    }

    public void parse(String input) {
        Tokenizer tokenizer = new Tokenizer(input);
        tokens = tokenizer.getTokens();
        current = 0;
        String cmd = input.trim().toUpperCase();

        try {
            if (cmd.startsWith("CREATE TABLE")) createTable(input);
            else if (cmd.startsWith("INSERT")) insert(input);
            else if (cmd.startsWith("SELECT")) select(input);
            else if (cmd.startsWith("UPDATE")) update(input);
            else if (cmd.startsWith("DELETE")) {
                match("DELETE");
                parseDelete();
            }
            else if (cmd.startsWith("DESCRIBE")) describe(input);
            else if (cmd.startsWith("INPUT")) inputFile(input);
            else if (cmd.startsWith("LET")) let(input);
            else System.out.println("Invalid command");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createTable(String input) {
        int tableIndex = input.toUpperCase().indexOf("TABLE");
        int openParen = input.indexOf("(");
        int closeParen = input.lastIndexOf(")");

        if (tableIndex < 0 || openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            throw new RuntimeException("Invalid CREATE TABLE syntax");
        }

        String name = input.substring(tableIndex + 5, openParen).trim();
        if (name.isEmpty()) {
            throw new RuntimeException("Missing table name");
        }
        if (db.hasTable(name)) {
            throw new RuntimeException("Table '" + name + "' already exists");
        }

        String inside = input.substring(openParen + 1, closeParen).trim();
        if (inside.isEmpty()) {
            throw new RuntimeException("Table must have at least one column");
        }

        String[] attrs = inside.split(",");
        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        String pk = null;

        for (String a : attrs) {
            String[] p = a.trim().split("\\s+");
            if (p.length < 2) {
                throw new RuntimeException("Invalid column definition: " + a.trim());
            }

            String columnName = p[0].trim();
            String columnType = p[1].trim().toUpperCase();

            if (columns.contains(columnName)) {
                throw new RuntimeException("Duplicate column: " + columnName);
            }

            if (!columnType.equals("INTEGER") && !columnType.equals("FLOAT") && !columnType.equals("TEXT")) {
                throw new RuntimeException("Invalid type: " + columnType);
            }

            columns.add(columnName);
            types.add(columnType);

            if (a.toUpperCase().contains("PRIMARY KEY")) {
                if (pk != null) throw new RuntimeException("Multiple primary keys are not supported");
                pk = columnName;
            }
        }

        db.addTable(name, new Table(name, columns, types, pk));
        System.out.println("Table created");
    }

    private void insert(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 4) {
            throw new RuntimeException("Invalid INSERT syntax");
        }

        String name = parts[1].trim().replace(";", "");
        Table t = db.getTable(name);

        if (t == null) {
            System.out.println("Error: Table '" + name + "' does not exist");
            return;
        }

        int openParen = input.indexOf("(");
        int closeParen = input.lastIndexOf(")");
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            throw new RuntimeException("Invalid INSERT syntax");
        }

        String values = input.substring(openParen + 1, closeParen);
        t.insert(new Record(values.split(",")));

        System.out.println("Inserted into " + t.name);
    }

    private void select(String input) {
        String upper = input.toUpperCase();
        int fromIndex = upper.indexOf("FROM");
        if (fromIndex < 0) {
            throw new RuntimeException("Missing FROM clause");
        }

        boolean hasWhere = upper.contains("WHERE");
        String beforeFrom = input.substring(6, fromIndex).trim();
        String afterFrom = input.substring(fromIndex + 4).trim();

        String tableName;
        String whereClause = null;

        if (hasWhere) {
            int whereIndex = afterFrom.toUpperCase().indexOf("WHERE");
            tableName = afterFrom.substring(0, whereIndex).trim().replace(";", "");
            whereClause = afterFrom.substring(whereIndex + 5).replace(";", "").trim();
        } else {
            tableName = afterFrom.replace(";", "").trim();
        }

        if (tableName.contains(",")) {
            throw new RuntimeException("Multi-table SELECT is not supported");
        }

        Table t = db.getTable(tableName);
        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        Condition cond = (whereClause != null && !whereClause.isEmpty()) ? new Condition(whereClause) : null;
        List<Record> result = t.select(cond, null);

        if (beforeFrom.contains("(")) {
            String func = beforeFrom.substring(0, beforeFrom.indexOf("(")).trim().toUpperCase();
            String attr = beforeFrom.substring(beforeFrom.indexOf("(") + 1, beforeFrom.indexOf(")")).trim();

            if (func.equals("AVERAGE")) func = "AVG";
            t.aggregate(func, attr, result);
            return;
        }

        List<String> attrs;
        if (beforeFrom.equals("*")) attrs = t.columns;
        else attrs = Arrays.asList(beforeFrom.split(","));

        t.print(result, attrs);
    }

    private void update(String input) {
        System.out.println("UPDATE not fully implemented");
    }

    private void parseDelete() {
        String tableName = expectIdentifier();
        Table t = db.getTable(tableName);

        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        Condition cond = null;

        if (match("WHERE")) {
            StringBuilder where = new StringBuilder();
            while (!peek().value.equals(";") && !peek().value.equals("EOF")) {
                where.append(advance().value).append(" ");
            }
            String whereClause = where.toString().trim();
            if (whereClause.isEmpty()) {
                throw new RuntimeException("Missing WHERE condition");
            }
            cond = new Condition(whereClause);
        }

        expect(";");

        if (cond == null) {
            t.clear();
            System.out.println("All rows deleted from " + t.name);
        } else {
            t.delete(cond);
            System.out.println("Deleted matching rows from " + t.name);
        }
    }

    private void describe(String input) {
        String trimmed = input.trim();
        if (trimmed.equalsIgnoreCase("DESCRIBE ALL;") || trimmed.equalsIgnoreCase("DESCRIBE;")) {
            db.describeAll();
            return;
        }

        String[] parts = trimmed.replace(";", "").split("\\s+");
        if (parts.length >= 2) {
            Table t = db.getTable(parts[1]);
            if (t == null) {
                System.out.println("Error: Table '" + parts[1] + "' does not exist");
                return;
            }
            t.describe();
            return;
        }

        db.describeAll();
    }

    private void inputFile(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            throw new RuntimeException("Missing input file name");
        }
        String file = parts[1].replace(";", "");
        FileManager.executeFile(file, this);
    }

    private void let(String input) {
        String[] parts = input.split("(?i)SELECT", 2);
        if (parts.length < 2) {
            throw new RuntimeException("Invalid LET syntax");
        }

        String header = parts[0].trim();
        String selectPart = "SELECT" + parts[1];
        String[] headerTokens = header.split("\\s+");

        if (headerTokens.length < 2) {
            throw new RuntimeException("Invalid LET syntax");
        }

        String newTableName = headerTokens[1];
        String key = null;
        if (headerTokens.length >= 4 && headerTokens[2].equalsIgnoreCase("KEY")) {
            key = headerTokens[3];
        }

        String upperSelect = selectPart.toUpperCase();
        int fromIndex = upperSelect.indexOf("FROM");
        if (fromIndex < 0) {
            throw new RuntimeException("Invalid LET SELECT syntax");
        }

        String attrPart = selectPart.substring(6, fromIndex).trim();
        String afterFrom = selectPart.substring(fromIndex + 4).trim();

        String tableName;
        String whereClause = null;
        int whereIndex = afterFrom.toUpperCase().indexOf("WHERE");
        if (whereIndex >= 0) {
            tableName = afterFrom.substring(0, whereIndex).trim();
            whereClause = afterFrom.substring(whereIndex + 5).replace(";", "").trim();
        } else {
            tableName = afterFrom.replace(";", "").trim();
        }

        if (tableName.contains(",")) {
            throw new RuntimeException("LET supports only one source table");
        }

        Table oldTable = db.getTable(tableName);
        if (oldTable == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        List<String> selectedColumns;
        if (attrPart.equals("*")) selectedColumns = new ArrayList<>(oldTable.columns);
        else {
            selectedColumns = new ArrayList<>();
            for (String attr : attrPart.split(",")) {
                String clean = attr.trim();
                if (!oldTable.columns.contains(clean)) {
                    throw new RuntimeException("Unknown column: " + clean);
                }
                selectedColumns.add(clean);
            }
        }

        List<String> selectedTypes = new ArrayList<>();
        for (String col : selectedColumns) {
            int idx = oldTable.columns.indexOf(col);
            selectedTypes.add(oldTable.types.get(idx));
        }

        if (key != null && !selectedColumns.contains(key)) {
            throw new RuntimeException("LET key must be one of the selected columns");
        }

        Condition cond = (whereClause != null && !whereClause.isEmpty()) ? new Condition(whereClause) : null;
        List<Record> result = oldTable.select(cond, null);

        Table newTable = new Table(newTableName, selectedColumns, selectedTypes, key);
        for (Record r : result) {
            String[] projected = new String[selectedColumns.size()];
            for (int i = 0; i < selectedColumns.size(); i++) {
                int idx = oldTable.columns.indexOf(selectedColumns.get(i));
                projected[i] = r.values[idx];
            }
            newTable.insert(new Record(projected));
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