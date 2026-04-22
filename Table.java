import java.util.*;

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
            throw new RuntimeException("Expected " + columns.size() + " values but found " + r.values.length);
        }

        if (primaryKey != null) {
            int idx = columns.indexOf(primaryKey);
            if (idx < 0) {
                throw new RuntimeException("Invalid primary key definition");
            }

            for (Record existing : records) {
                if (existing.values[idx].trim().equals(r.values[idx].trim())) {
                    throw new RuntimeException("Duplicate primary key: " + r.values[idx].trim());
                }
            }
        }

        records.add(r);

        if (primaryKey != null) {
            int idx = columns.indexOf(primaryKey);
            int key = Integer.parseInt(r.values[idx].trim());
            index.insert(key, r);
        }
    }

    public List<Record> select(Condition cond, String attr) {
        List<Record> result = new ArrayList<>();

        if (primaryKey != null) {
            List<Record> ordered = index.getAllRecords();
            for (Record r : ordered) {
                if (cond == null || cond.evaluate(r, columns)) {
                    result.add(r);
                }
            }
        } else {
            for (Record r : records) {
                if (cond == null || cond.evaluate(r, columns)) {
                    result.add(r);
                }
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
            String a = attr.trim();
            if (!columns.contains(a)) {
                throw new RuntimeException("Unknown column: " + a);
            }
            cleanAttrs.add(a);
        }

        System.out.println(String.join(" | ", cleanAttrs));

        int count = 1;
        for (Record r : result) {
            List<String> row = new ArrayList<>();
            for (String attr : cleanAttrs) {
                int idx = columns.indexOf(attr);
                row.add(r.values[idx]);
            }
            System.out.println(count++ + ". " + String.join(" | ", row));
        }
    }

    public void aggregate(String func, String attr, List<Record> data) {
        String actualFunc = func.toUpperCase();

        if (actualFunc.equals("COUNT")) {
            System.out.println("COUNT = " + data.size());
            return;
        }

        if (actualFunc.equals("AVERAGE")) {
            actualFunc = "AVG";
        }

        int idx = columns.indexOf(attr);
        if (idx < 0) {
            throw new RuntimeException("Unknown column: " + attr);
        }

        if (data.isEmpty()) {
            System.out.println(actualFunc + " = 0");
            return;
        }

        List<Double> nums = new ArrayList<>();
        for (Record r : data) {
            nums.add(Double.parseDouble(r.values[idx]));
        }

        double result = 0;
        switch (actualFunc) {
            case "MIN": result = Collections.min(nums); break;
            case "MAX": result = Collections.max(nums); break;
            case "AVG": result = nums.stream().mapToDouble(d -> d).average().orElse(0); break;
            default: throw new RuntimeException("Unsupported aggregate: " + func);
        }

        System.out.println(actualFunc + " = " + result);
    }

    public void describe() {
        System.out.println("\n" + name);
        for (int i = 0; i < columns.size(); i++) {
            System.out.println(columns.get(i) + ": " + types.get(i));
        }
        if (primaryKey != null) {
            System.out.println("PRIMARY KEY: " + primaryKey);
        }
    }

    public void clear() {
        records.clear();
        index = new BST();
    }

    public void save() {
        StringBuilder sb = new StringBuilder();
        sb.append(columns.size()).append("\n");

        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i)).append(" ").append(types.get(i)).append("\n");
        }

        if (primaryKey != null) {
            sb.append("PRIMARY ").append(primaryKey).append("\n");
        }

        sb.append("DATA\n");

        for (Record r : records) {
            sb.append(String.join(",", r.values)).append("\n");
        }

        FileManager.save(name + ".tbl", sb.toString());
    }

    public void delete(Condition cond) {
        if (cond == null) {
            records.clear();
            index = new BST();
            return;
        }

        Iterator<Record> it = records.iterator();
        while (it.hasNext()) {
            Record r = it.next();
            if (cond.evaluate(r, columns)) {
                it.remove();
            }
        }

        if (primaryKey != null) {
            index = new BST();
            int pkIndex = columns.indexOf(primaryKey);
            for (Record r : records) {
                int key = Integer.parseInt(r.values[pkIndex]);
                index.insert(key, r);
            }
        }
    }
}