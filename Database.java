import java.util.*;

public class Database {
    Map<String, Table> tables = new HashMap<>();

    public void addTable(String name, Table table) {
        tables.put(name.toUpperCase(), table);
    }

    public Table getTable(String name) {
        if (name == null) return null;
        return tables.get(name.trim().toUpperCase());
    }

    public boolean hasTable(String name) {
        return getTable(name) != null;
    }

    public void describeAll() {
        for (Table t : tables.values()) {
            t.describe();
        }
    }
}