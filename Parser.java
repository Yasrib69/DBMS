import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private static class QueryResult {
        List<String> columns;
        List<String> types;
        List<Record> rows;

        QueryResult(List<String> columns, List<String> types, List<Record> rows) {
            this.columns = columns;
            this.types = types;
            this.rows = rows;
        }
    }

    private List<Token> tokens;
    private int current = 0;

    Database db = new Database();
    String currentDB = "";

    public Parser() {
        loadTablesFrom(".");
    }

    private void loadTablesFrom(String dir) {
        File folder = new File(dir);
        File[] files = folder.listFiles((d, n) -> n.endsWith(".tbl"));
        if (files == null) return;
        for (File f : files) {
            Table t = FileManager.loadTable(f.getPath());
            if (t != null) db.addTable(t.name, t);
        }
    }

    private Token peek() {
        if (tokens == null || tokens.isEmpty()) throw new RuntimeException("No tokens available");
        if (current >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(current);
    }

    private Token advance() {
        if (tokens == null || tokens.isEmpty()) throw new RuntimeException("No tokens available");
        if (current < tokens.size()) current++;
        return tokens.get(Math.max(0, current - 1));
    }

    private boolean match(String value) {
        if (peek().value.equalsIgnoreCase(value)) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(String value) {
        if (!match(value)) throw new RuntimeException("Expected '" + value + "' but got '" + peek().value + "'");
    }

    private String expectIdentifier() {
        Token t = peek();
        if (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD) {
            advance();
            return t.value;
        }
        throw new RuntimeException("Expected identifier but got '" + t.value + "'");
    }

    public void parse(String input) {
        Tokenizer tokenizer = new Tokenizer(input);
        tokens = tokenizer.getTokens();
        current = 0;

        String cmd = input.trim().toUpperCase();
        try {
            if (cmd.startsWith("CREATE DATABASE")) createDatabase(input);
            else if (cmd.startsWith("CREATE TABLE")) createTable(input);
            else if (cmd.startsWith("CREATE")) System.out.println("Error: Unknown CREATE variant");
            else if (cmd.startsWith("USE")) useDatabase(input);
            else if (cmd.startsWith("INSERT")) { match("INSERT"); parseInsert(); }
            else if (cmd.startsWith("SELECT")) select(input);
            else if (cmd.startsWith("UPDATE")) { match("UPDATE"); parseUpdate(); }
            else if (cmd.startsWith("DELETE")) { match("DELETE"); parseDelete(); }
            else if (cmd.startsWith("DESCRIBE")) describe(input);
            else if (cmd.startsWith("RENAME")) { match("RENAME"); parseRename(); }
            else if (cmd.startsWith("LET")) let(input);
            else if (cmd.startsWith("INPUT")) inputFile(input);
            else System.out.println("Error: Unknown command");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createDatabase(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 3) throw new RuntimeException("Usage: CREATE DATABASE <name>;");
        String name = parts[2].replace(";", "").trim();
        if (name.isEmpty()) throw new RuntimeException("Missing database name");

        File dir = new File(name);
        if (dir.exists()) System.out.println("Database '" + name + "' already exists");
        else {
            dir.mkdir();
            System.out.println("Database created: " + name);
        }
    }

    private void useDatabase(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) throw new RuntimeException("Usage: USE <name>;");
        String name = parts[1].replace(";", "").trim();
        if (name.isEmpty()) throw new RuntimeException("Usage: USE <name>;");

        File dir = new File(name);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Error: Database '" + name + "' does not exist");
            return;
        }

        currentDB = name;
        db = new Database();
        db.currentDB = name;
        loadTablesFrom(name);
        System.out.println("Using database: " + name);
    }

    private void createTable(String input) {
        int tableIdx = input.toUpperCase().indexOf("TABLE");
        int openParen = input.indexOf('(');
        int closeParen = input.lastIndexOf(')');
        if (tableIdx < 0 || openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            throw new RuntimeException("Invalid CREATE TABLE syntax");
        }

        String name = input.substring(tableIdx + 5, openParen).trim();
        if (name.isEmpty()) throw new RuntimeException("Missing table name");
        if (db.hasTable(name)) throw new RuntimeException("Table '" + name + "' already exists");

        String inside = input.substring(openParen + 1, closeParen).trim();
        if (inside.isEmpty()) throw new RuntimeException("Table must have at least one column");

        List<String> columns = new ArrayList<>();
        List<String> types = new ArrayList<>();
        String pk = null;

        for (String attr : inside.split(",")) {
            String[] p = attr.trim().split("\\s+");
            if (p.length < 2) throw new RuntimeException("Invalid column definition: " + attr.trim());
            String colName = p[0].trim();
            String colType = p[1].trim().toUpperCase();

            for (String existing : columns) {
                if (existing.equalsIgnoreCase(colName)) {
                    throw new RuntimeException("Duplicate column: " + colName);
                }
            }
            if (!colType.equals("INTEGER") && !colType.equals("FLOAT") && !colType.equals("TEXT")) {
                throw new RuntimeException("Invalid type '" + colType + "' for column " + colName);
            }

            columns.add(colName);
            types.add(colType);
            if (attr.toUpperCase().contains("PRIMARY") && attr.toUpperCase().contains("KEY")) {
                if (pk != null) throw new RuntimeException("Only one PRIMARY KEY allowed");
                pk = colName;
            }
        }

        db.addTable(name, new Table(name, columns, types, pk));
        System.out.println("Table created: " + name);
    }

    private void parseInsert() {
        String tableName = expectIdentifier();
        Table t = db.getTable(tableName);
        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        expect("VALUES");
        expect("(");
        List<String> vals = new ArrayList<>();
        if (!peek().value.equals(")")) {
            do {
                Token tok = peek();
                switch (tok.type) {
                    case STRING:
                    case INTEGER:
                    case FLOAT:
                    case IDENTIFIER:
                    case KEYWORD:
                        vals.add(advance().value);
                        break;
                    default:
                        throw new RuntimeException("Unexpected token in VALUES: " + tok.value);
                }
            } while (match(","));
        }
        expect(")");
        expect(";");

        t.insert(new Record(vals.toArray(new String[0])));
        System.out.println("Inserted into " + t.name);
    }

    private void select(String input) {
        QueryResult result = executeSelect(input);

        String upper = input.toUpperCase();
        int fromIdx = upper.indexOf("FROM");
        String beforeFrom = input.substring(6, fromIdx).trim();

        if (beforeFrom.contains("(")) {
            int lp = beforeFrom.indexOf('(');
            int rp = beforeFrom.lastIndexOf(')');
            String func = beforeFrom.substring(0, lp).trim().toUpperCase();
            String attr = rp > lp ? beforeFrom.substring(lp + 1, rp).trim() : "";
            if (func.equals("AVERAGE")) func = "AVG";
            Table temp = new Table("_temp", result.columns, result.types, null);
            temp.records.addAll(result.rows);
            temp.aggregate(func, attr, result.rows);
            return;
        }

        if (result.rows.isEmpty()) {
            System.out.println("Nothing found");
            return;
        }

        System.out.println(String.join(" | ", result.columns));
        int count = 1;
        for (Record r : result.rows) {
            System.out.println(count++ + ". " + String.join(" | ", r.values));
        }
    }

    private QueryResult executeSelect(String input) {
        String upper = input.toUpperCase();
        int fromIdx = upper.indexOf("FROM");
        if (fromIdx < 0) throw new RuntimeException("Missing FROM clause");

        String attrPart = input.substring(6, fromIdx).trim();
        if (attrPart.isEmpty()) throw new RuntimeException("No columns specified in SELECT");
        String afterFrom = input.substring(fromIdx + 4).trim();

        String tableList;
        String whereClause = null;
        int whereIdx = afterFrom.toUpperCase().indexOf("WHERE");
        if (whereIdx >= 0) {
            tableList = afterFrom.substring(0, whereIdx).trim();
            whereClause = afterFrom.substring(whereIdx + 5).replace(";", "").trim();
        } else {
            tableList = afterFrom.replace(";", "").trim();
        }

        List<Table> sourceTables = new ArrayList<>();
        for (String name : tableList.split(",")) {
            String clean = name.trim();
            Table t = db.getTable(clean);
            if (t == null) {
                System.out.println("Error: Table '" + clean + "' does not exist");
                return new QueryResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }
            sourceTables.add(t);
        }

        QueryResult joined = buildCartesianProduct(tableList, sourceTables);
        Condition cond = (whereClause != null && !whereClause.isEmpty()) ? new Condition(whereClause) : null;

        List<Record> filtered = new ArrayList<>();
        for (Record r : joined.rows) {
            if (cond == null || cond.evaluate(r, joined.columns)) {
                filtered.add(r);
            }
        }

        if (attrPart.contains("(")) {
            return new QueryResult(joined.columns, joined.types, filtered);
        }

        List<String> projectedColumns = new ArrayList<>();
        List<String> projectedTypes = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();

        if (attrPart.equals("*")) {
            projectedColumns.addAll(joined.columns);
            projectedTypes.addAll(joined.types);
            for (int i = 0; i < joined.columns.size(); i++) indexes.add(i);
        } else {
            for (String attr : attrPart.split(",")) {
                String clean = attr.trim();
                int idx = findColumnIndex(joined.columns, clean);
                if (idx == -1) {
                                    for (int i = 0; i < joined.columns.size(); i++) {
                                        String col = joined.columns.get(i);
                                        if (col.contains(".")) {
                                            String shortName = col.substring(col.indexOf('.') + 1);
                                            if (shortName.equalsIgnoreCase(clean)) {
                                                idx = i;
                                                break;
                                            }
                                        }
                                    }
                                }
                if (idx < 0) throw new RuntimeException("Unknown column: " + clean);
                projectedColumns.add(clean);
                projectedTypes.add(joined.types.get(idx));
                indexes.add(idx);
            }
        }

        List<Record> projectedRows = new ArrayList<>();
        for (Record r : filtered) {
            String[] values = new String[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                values[i] = r.values[indexes.get(i)];
            }
            projectedRows.add(new Record(values));
        }

        return new QueryResult(projectedColumns, projectedTypes, projectedRows);
    }

    private QueryResult buildCartesianProduct(String tableList, List<Table> tables) {
        List<String> allColumns = new ArrayList<>();
        List<String> allTypes = new ArrayList<>();
        List<List<Record>> orderedRows = new ArrayList<>();

        String[] names = tableList.split(",");
        for (int i = 0; i < tables.size(); i++) {
            Table t = tables.get(i);
            String tableName = names[i].trim();
            for (int j = 0; j < t.columns.size(); j++) {
                allColumns.add(tableName + "." + t.columns.get(j));
                allTypes.add(t.types.get(j));
            }
            orderedRows.add(t.select(null, null));
        }

        List<Record> out = new ArrayList<>();
        buildProductRows(orderedRows, 0, new ArrayList<>(), out);
        return new QueryResult(allColumns, allTypes, out);
    }

    private void buildProductRows(List<List<Record>> sources, int depth, List<String> currentValues, List<Record> out) {
        if (depth == sources.size()) {
            out.add(new Record(currentValues.toArray(new String[0])));
            return;
        }
        for (Record r : sources.get(depth)) {
            List<String> next = new ArrayList<>(currentValues);
            next.addAll(Arrays.asList(r.values));
            buildProductRows(sources, depth + 1, next, out);
        }
    }

    private void parseUpdate() {
        String tableName = expectIdentifier();
        Table t = db.getTable(tableName);
        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        expect("SET");
        Map<String, String> assignments = new LinkedHashMap<>();
        do {
            String col = expectIdentifier();
            expect("=");
            Token val = peek();
            if (val.type != TokenType.STRING && val.type != TokenType.INTEGER && val.type != TokenType.FLOAT
                && val.type != TokenType.IDENTIFIER && val.type != TokenType.KEYWORD) {
                throw new RuntimeException("Invalid value in SET: " + val.value);
            }
            assignments.put(col.trim(), advance().value);
        } while (match(","));

        Condition cond = null;
        if (match("WHERE")) {
            String whereClause = collectUntilSemicolon();
            if (!whereClause.isEmpty()) cond = new Condition(whereClause);
        }
        expect(";");

        t.update(assignments, cond);
        System.out.println("Updated table: " + t.name);
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
            String whereClause = collectUntilSemicolon();
            if (whereClause.isEmpty()) throw new RuntimeException("Missing WHERE condition");
            cond = new Condition(whereClause);
        }
        expect(";");

        if (cond == null) {
            db.removeTable(tableName);
            FileManager.deleteTableFiles(currentDB, t.name);
            System.out.println("Table deleted: " + t.name);
        } else {
            t.delete(cond);
            System.out.println("Deleted matching rows from " + t.name);
        }
    }

    private void describe(String input) {
        String cleaned = input.trim().replace(";", "").trim();
        String[] parts = cleaned.split("\\s+");
        if (parts.length < 2 || parts[1].equalsIgnoreCase("ALL")) {
            db.describeAll();
            return;
        }

        Table t = db.getTable(parts[1]);
        if (t == null) {
            System.out.println("Error: Table '" + parts[1] + "' does not exist");
            return;
        }
        t.describe();
    }

    private void parseRename() {
        String tableName = expectIdentifier();
        Table t = db.getTable(tableName);
        if (t == null) {
            System.out.println("Error: Table '" + tableName + "' does not exist");
            return;
        }

        expect("(");
        List<String> newCols = new ArrayList<>();
        do {
            newCols.add(expectIdentifier());
        } while (match(","));
        expect(")");
        expect(";");

        t.rename(newCols);
        System.out.println("Renamed columns of table: " + t.name);
    }

    private void let(String input) {

        String[] parts = input.split("(?i)SELECT", 2);
        if (parts.length < 2) {
            throw new RuntimeException("Invalid LET syntax; missing SELECT");
        }

        String header = parts[0].trim();
        String selectPart = "SELECT" + parts[1];

        String[] headerTokens = header.split("\\s+");

        if (headerTokens.length < 2) {
            throw new RuntimeException("Invalid LET syntax; missing table name");
        }

        String newTableName = headerTokens[1];
        String key = null;

        if (headerTokens.length >= 4 && headerTokens[2].equalsIgnoreCase("KEY")) {
            key = headerTokens[3];
        }

        QueryResult result = executeSelect(selectPart);

        if (result == null) {
            throw new RuntimeException("LET failed: invalid SELECT");
        }

        if (key != null && findColumnIndex(result.columns, key) < 0) {
            throw new RuntimeException("LET key must be one of the selected columns");
        }

        List<String> tableColumns = new ArrayList<>();
        for (String col : result.columns) {
            int dot = col.indexOf('.');
            if (dot >= 0) tableColumns.add(col.substring(dot + 1));
            else tableColumns.add(col);
        }

        Table newTable = new Table(newTableName, tableColumns, result.types, key);

        for (Record r : result.rows) {
            newTable.insert(new Record(Arrays.copyOf(r.values, r.values.length)));
        }

        db.addTable(newTableName, newTable);

        System.out.println("LET created table: " + newTableName);
    }

    private void inputFile(String input) {
        String[] parts = input.trim().replace(";", "").split("\\s+");
        if (parts.length < 2) throw new RuntimeException("Usage: INPUT <file> [OUTPUT <file>];");
        String inputFile = parts[1];
        String outputFile = null;
        if (parts.length >= 4 && parts[2].equalsIgnoreCase("OUTPUT")) {
            outputFile = parts[3];
        }
        if (outputFile == null) FileManager.executeFile(inputFile, this);
        else FileManager.executeFile(inputFile, outputFile, this);
    }

    public void saveAll() {
        for (Table t : db.tables.values()) {
            t.save(currentDB);
        }
    }

    private String collectUntilSemicolon() {
        StringBuilder sb = new StringBuilder();
        while (!peek().value.equals(";") && peek().type != TokenType.EOF) {
            sb.append(advance().value).append(' ');
        }
        return sb.toString().trim();
    }

    private int findColumnIndex(List<String> columns, String target) {

        String t = target.trim();

        // 1. direct match (st.name)
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(t)) {
                return i;
            }
        }

        // 2. match unqualified name (name)
        int found = -1;

        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);

            if (col.contains(".")) {
                String shortName = col.substring(col.indexOf('.') + 1);

                if (shortName.equalsIgnoreCase(t)) {
                    // if multiple matches → ambiguous (optional handling)
                    if (found != -1) {
                        return found; // keep first match (simple approach)
                    }
                    found = i;
                }
            }
        }

        return found;
    }
}