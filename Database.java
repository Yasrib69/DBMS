import java.util.*;

public class Database {
    Map<String, Table> tables = new HashMap<>();

    public void addTable(String name, Table table) {
        tables.put(name.toUpperCase(), table);
    }

    public Table getTable(String name) {
        return tables.get(name.toUpperCase());
    }

    public void describeAll() {
        for (Table t : tables.values()) {
            t.describe();
        }
    }
}