import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Condition {
    static class Clause {
        String attr;
        String op;
        String value;
        boolean quoted;
    }

    List<Clause> clauses = new ArrayList<>();
    List<String> ops = new ArrayList<>();

    public Condition(String where) {
        if (where == null || where.trim().isEmpty()) {
            throw new RuntimeException("Empty WHERE clause");
        }

        String normalized = where.trim();
        String[] parts = normalized.split("(?i)\\s+(AND|OR)\\s+");

        for (String part : parts) {
            String trimmed = part.trim();
            Clause c = new Clause();

            if (trimmed.contains(">=")) c.op = ">=";
            else if (trimmed.contains("<=")) c.op = "<=";
            else if (trimmed.contains("!=")) c.op = "!=";
            else if (trimmed.contains("=")) c.op = "=";
            else if (trimmed.contains(">")) c.op = ">";
            else if (trimmed.contains("<")) c.op = "<";
            else throw new RuntimeException("Invalid condition: " + trimmed);

            String[] sides = trimmed.split(Pattern.quote(c.op), 2);
            if (sides.length != 2) {
                throw new RuntimeException("Malformed condition: " + trimmed);
            }

            c.attr = sides[0].trim();
            String right = sides[1].trim();
            c.quoted = right.startsWith("\"") && right.endsWith("\"") && right.length() >= 2;
            c.value = c.quoted ? right.substring(1, right.length() - 1) : right;
            clauses.add(c);
        }

        Matcher m = Pattern.compile("(?i)\\s+(AND|OR)\\s+").matcher(normalized);
        while (m.find()) {
            ops.add(m.group(1).toUpperCase());
        }
    }

    public boolean evaluate(Record r, List<String> cols) {
        boolean result = eval(r, cols, clauses.get(0));
        for (int i = 1; i < clauses.size(); i++) {
            boolean next = eval(r, cols, clauses.get(i));
            if (ops.get(i - 1).equals("AND")) result = result && next;
            else result = result || next;
        }
        return result;
    }

    private boolean eval(Record r, List<String> cols, Clause c) {

        int leftIdx = findColumnIndex(cols, c.attr);

        if (leftIdx == -1) {
            for (int i = 0; i < cols.size(); i++) {
                String col = cols.get(i);
                if (col.contains(".")) {
                    String shortName = col.substring(col.indexOf('.') + 1);
                    if (shortName.equalsIgnoreCase(c.attr.trim())) {
                        leftIdx = i;
                        break;
                    }
                }
            }
        }

        if (leftIdx == -1) {
            throw new RuntimeException("Unknown column in WHERE: " + c.attr);
        }

        String left = r.values[leftIdx].trim();
        String right = c.value.trim();

        if (!c.quoted) {
            int rightIdx = findColumnIndex(cols, right);

            if (rightIdx == -1) {
                for (int i = 0; i < cols.size(); i++) {
                    String col = cols.get(i);
                    if (col.contains(".")) {
                        String shortName = col.substring(col.indexOf('.') + 1);
                        if (shortName.equalsIgnoreCase(right.trim())) {
                            rightIdx = i;
                            break;
                        }
                    }
                }
            }

            if (rightIdx >= 0) {
                right = r.values[rightIdx].trim();
            }
        }

        if (isNumeric(left) && isNumeric(right)) {
            double a = Double.parseDouble(left);
            double b = Double.parseDouble(right);

            switch (c.op) {
                case "=": return a == b;
                case "!=": return a != b;
                case ">": return a > b;
                case "<": return a < b;
                case ">=": return a >= b;
                case "<=": return a <= b;
            }
        }

        int cmp = left.compareToIgnoreCase(right);

        switch (c.op) {
            case "=": return cmp == 0;
            case "!=": return cmp != 0;
            case ">": return cmp > 0;
            case "<": return cmp < 0;
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
        }

        return false;
    }

    private int findColumnIndex(List<String> cols, String target) {

        String t = target.trim();

        // direct match (table.column or column)
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).equalsIgnoreCase(t)) {
                return i;
            }
        }

        // unqualified match (column only)
        for (int i = 0; i < cols.size(); i++) {
            String col = cols.get(i);

            if (col.contains(".")) {
                String shortName = col.substring(col.indexOf('.') + 1);

                if (shortName.equalsIgnoreCase(t)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void validate(List<String> columns) {
        try {
            // Create a dummy record just to trigger column lookup
            String[] dummyValues = new String[columns.size()];
            Record dummy = new Record(dummyValues);

            // This will fail if column does not exist
            evaluate(dummy, columns);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("Column")) {
                throw e;
            }
        }
    }
}