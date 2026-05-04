import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    Map<String, Table> tables = new LinkedHashMap<>();
    String currentDB = "";

    public void addTable(String name, Table table) {
        tables.put(normalize(name), table);
    }

    public Table getTable(String name) {
        return tables.get(normalize(name));
    }

    public boolean hasTable(String name) {
        return getTable(name) != null;
    }

    public void removeTable(String name) {
        tables.remove(normalize(name));
    }

    public String normalize(String name) {
        return name == null ? "" : name.trim().toUpperCase();
    }

    public void describeAll() {
        for (Table t : tables.values()) {
            t.describe();
        }
    }
}