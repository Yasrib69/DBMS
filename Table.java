import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table {
    String name;
    List<String> columns;
    List<String> types;
    String primaryKey;

    List<Record> records = new ArrayList<>();
    BST index = new BST();

    public Table(String name, List<String> cols, List<String> types, String pk) {
        this.name = name;
        this.columns = new ArrayList<>(cols);
        this.types = new ArrayList<>(types);
        this.primaryKey = pk;
    }

    public void insert(Record r) {
        if (r.values.length != columns.size()) {
            throw new RuntimeException("Column count mismatch: expected " + columns.size() + " values but got " + r.values.length);
        }

        for (int i = 0; i < r.values.length; i++) {
            validateDomain(i, r.values[i]);
        }

        if (primaryKey != null) {
            int idx = findColumnIndex(primaryKey);
            if (idx < 0) throw new RuntimeException("Invalid primary key: " + primaryKey);
            if (r.values[idx].trim().isEmpty()) throw new RuntimeException("Primary key cannot be empty");
            for (Record existing : records) {
                if (existing.values[idx].trim().equalsIgnoreCase(r.values[idx].trim())) {
                    throw new RuntimeException("Duplicate primary key: " + r.values[idx].trim());
                }
            }
        }

        records.add(r);
        rebuildIndex();
    }

    public List<Record> select(Condition cond, String attr) {
        List<Record> result = new ArrayList<>();
        List<Record> source = primaryKey != null ? index.getAllRecords() : records;

        for (Record r : source) {
            if (cond == null || cond.evaluate(r, columns)) {
                result.add(r);
            }
        }
        return result;
    }

    public void print(List<Record> result, List<String> attrs) {
        if (result.isEmpty()) {
            System.out.println("Nothing found");
            return;
        }

        List<String> cleanAttrs = new ArrayList<>();
        for (String attr : attrs) {
            String clean = attr.trim();
            if (findColumnIndex(clean) < 0) throw new RuntimeException("Unknown column: " + clean);
            cleanAttrs.add(clean);
        }

        System.out.println(String.join(" | ", cleanAttrs));
        int count = 1;
        for (Record r : result) {
            List<String> row = new ArrayList<>();
            for (String attr : cleanAttrs) {
                row.add(r.values[findColumnIndex(attr)]);
            }
            System.out.println(count++ + ". " + String.join(" | ", row));
        }
    }

    public void aggregate(String func, String attr, List<Record> data) {
        String actualFunc = func.toUpperCase();
        if (actualFunc.equals("AVERAGE")) actualFunc = "AVG";

        if (actualFunc.equals("COUNT")) {
            System.out.println("COUNT = " + data.size());
            return;
        }

        int idx = findColumnIndex(attr);
        if (idx < 0) throw new RuntimeException("Unknown column: " + attr);
        if (data.isEmpty()) {
            System.out.println(actualFunc + " = 0");
            return;
        }

        List<Double> nums = new ArrayList<>();
        for (Record r : data) {
            nums.add(Double.parseDouble(r.values[idx].trim()));
        }

        double result;
        switch (actualFunc) {
            case "MIN": result = Collections.min(nums); break;
            case "MAX": result = Collections.max(nums); break;
            case "AVG": result = nums.stream().mapToDouble(d -> d).average().orElse(0); break;
            default: throw new RuntimeException("Unsupported aggregate: " + func);
        }

        System.out.println(actualFunc + " = " + result);
    }

    public void describe() {
        System.out.println();
        System.out.println(name);
        for (int i = 0; i < columns.size(); i++) {
            String type = types.get(i);
            if (primaryKey != null && columns.get(i).equalsIgnoreCase(primaryKey)) {
                type += " PRIMARY KEY";
            }
            System.out.println(columns.get(i) + ": " + type);
        }
    }

    public void rename(List<String> newCols) {
        if (newCols.size() != columns.size()) {
            throw new RuntimeException("Column count mismatch: table has " + columns.size() + " columns but got " + newCols.size() + " new names");
        }

        int pkIndex = primaryKey == null ? -1 : findColumnIndex(primaryKey);
        for (int i = 0; i < newCols.size(); i++) {
            columns.set(i, newCols.get(i).trim());
        }
        if (pkIndex >= 0) primaryKey = columns.get(pkIndex);
    }

    public void update(Map<String, String> assignments, Condition cond) {
        

        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            int idx = findColumnIndexLoose(entry.getKey());

            if (idx == -1) {
                for (int i = 0; i < columns.size(); i++) {
                    if (columns.get(i).equalsIgnoreCase(entry.getKey().trim())) {
                        idx = i;
                        break;
                    }
                }
            }

            if (idx == -1) {
                throw new RuntimeException("Unknown column in SET: " + entry.getKey());
            }
        }

        for (Record r : records) {
            if (cond == null || cond.evaluate(r, columns)) {

                for (Map.Entry<String, String> entry : assignments.entrySet()) {

                    int idx = findColumnIndex(entry.getKey());

                    if (idx == -1) {
                        for (int i = 0; i < columns.size(); i++) {
                            if (columns.get(i).equalsIgnoreCase(entry.getKey().trim())) {
                                idx = i;
                                break;
                            }
                        }
                    }

                    String val = entry.getValue().trim().replace("\"", "");
                    validateDomain(idx, val);
                    r.values[idx] = val;
                }
            }
        }

        ensurePrimaryKeyUnique();
        rebuildIndex();
    }

    public void clear() {
        records.clear();
        index = new BST();
    }

    public void delete(Condition cond) {
        if (cond == null) {
            clear();
            return;
        }

        Iterator<Record> it = records.iterator();
        while (it.hasNext()) {
            Record r = it.next();
            if (cond.evaluate(r, columns)) {
                it.remove();
            }
        }
        rebuildIndex();
    }

    public void save(String currentDB) {
        StringBuilder sb = new StringBuilder();
        sb.append(columns.size()).append("\n");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i)).append(" ").append(types.get(i)).append("\n");
        }
        if (primaryKey != null) sb.append("PRIMARY ").append(primaryKey).append("\n");
        sb.append("DATA\n");
        for (Record r : records) {
            sb.append(String.join(",", r.values)).append("\n");
        }
        FileManager.saveTable(currentDB, name, sb.toString());
        FileManager.saveIndex(currentDB, name, index.getAllKeys());
    }

    public int findColumnIndex(String col) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(col.trim())) return i;
        }
        return -1;
    }

    private void validateDomain(int idx, String rawValue) {
        String type = types.get(idx).toUpperCase();
        String value = rawValue.trim().replace("\"", "");
        if (type.equals("INTEGER")) {
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                throw new RuntimeException("Domain constraint violation on column " + columns.get(idx));
            }
        } else if (type.equals("FLOAT")) {
            try {
                Double.parseDouble(value);
            } catch (Exception e) {
                throw new RuntimeException("Domain constraint violation on column " + columns.get(idx));
            }
        }
    }

    private void ensurePrimaryKeyUnique() {
        if (primaryKey == null) return;
        int idx = findColumnIndex(primaryKey);
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (Record r : records) {
            String key = r.values[idx].trim().toUpperCase();
            if (seen.containsKey(key)) {
                throw new RuntimeException("Duplicate primary key: " + r.values[idx].trim());
            }
            seen.put(key, 1);
        }
    }

    private void rebuildIndex() {
        index = new BST();
        if (primaryKey == null) return;
        int pkIndex = findColumnIndex(primaryKey);
        for (Record r : records) {
            int key = (int) Double.parseDouble(r.values[pkIndex].trim());
            index.insert(key, r);
        }
    }

    private int findColumnIndexLoose(List<String> cols, String target) {
    String t = target.trim();
        for (int i = 0; i < cols.size(); i++) {
            String col = cols.get(i);
            if (col.equalsIgnoreCase(t)) return i;
            int dot = col.indexOf('.');
            if (dot >= 0) {
                String shortName = col.substring(dot + 1);
                if (shortName.equalsIgnoreCase(t)) return i;
            }
        }
        return -1;
    }

    private int findColumnIndexLoose(String target) {

    String t = target.trim();

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(t)) {
                return i;
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);

            if (col.contains(".")) {
                String shortName = col.substring(col.indexOf('.') + 1);

                if (shortName.equalsIgnoreCase(t)) {
                    return i;
                }
            }
        }

        return -1;
    }

}