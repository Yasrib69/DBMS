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
        this.columns = cols;
        this.types = types;
        this.primaryKey = pk;
    }

    public void insert(Record r) {
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

        System.out.println(String.join(" | ", attrs));

        int count = 1;

        for (Record r : result) {
            List<String> row = new ArrayList<>();

            for (String attr : attrs) {
                int idx = columns.indexOf(attr.trim());
                row.add(r.values[idx]);
            }

            System.out.println(count++ + ". " + String.join(" | ", row));
        }
    }

    public void aggregate(String func, String attr, List<Record> data) {

        if (func.equals("COUNT")) {
            System.out.println("COUNT = " + data.size());
            return;
        }

        int idx = columns.indexOf(attr);
        List<Double> nums = new ArrayList<>();

        for (Record r : data) {
            nums.add(Double.parseDouble(r.values[idx]));
        }

        double result = 0;

        switch (func) {
            case "MIN": result = Collections.min(nums); break;
            case "MAX": result = Collections.max(nums); break;
            case "AVG": result = nums.stream().mapToDouble(d -> d).average().orElse(0); break;
        }

        System.out.println(func + " = " + result);
    }

    public void describe() {
        System.out.println("\n" + name);
        for (int i = 0; i < columns.size(); i++) {
            System.out.println(columns.get(i) + ": " + types.get(i));
        }
        if (primaryKey != null)
            System.out.println("PRIMARY KEY: " + primaryKey);
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
}