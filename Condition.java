import java.util.*;
import java.util.regex.Pattern;

public class Condition {

    static class Clause {
        String attr, op, value;
    }

    List<Clause> clauses = new ArrayList<>();
    List<String> ops = new ArrayList<>();

    public Condition(String where) {
        if (where == null || where.trim().isEmpty()) {
            throw new RuntimeException("Invalid WHERE clause");
        }

        String normalized = where.trim();
        String[] parts = normalized.split("(?i)\\s+(AND|OR)\\s+");

        for (String part : parts) {
            Clause c = new Clause();
            String trimmed = part.trim();

            if (trimmed.contains(">=")) c.op = ">=";
            else if (trimmed.contains("<=")) c.op = "<=";
            else if (trimmed.contains("!=")) c.op = "!=";
            else if (trimmed.contains("=")) c.op = "=";
            else if (trimmed.contains(">")) c.op = ">";
            else if (trimmed.contains("<")) c.op = "<";
            else throw new RuntimeException("Invalid condition: " + trimmed);

            String[] sides = trimmed.split(Pattern.quote(c.op), 2);
            if (sides.length != 2) {
                throw new RuntimeException("Invalid condition: " + trimmed);
            }

            c.attr = sides[0].trim();
            c.value = sides[1].trim().replace("\"", "");
            clauses.add(c);
        }

        java.util.regex.Matcher matcher = Pattern.compile("(?i)\\s+(AND|OR)\\s+").matcher(normalized);
        while (matcher.find()) {
            ops.add(matcher.group(1).toUpperCase());
        }

        if (clauses.isEmpty()) {
            throw new RuntimeException("Invalid WHERE clause");
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
        int idx = cols.indexOf(c.attr);
        if (idx < 0) {
            throw new RuntimeException("Unknown column in WHERE: " + c.attr);
        }

        String left = r.values[idx].trim();
        String right = c.value.trim();

        boolean numeric = isNumeric(left) && isNumeric(right);
        if (numeric) {
            double a = Double.parseDouble(left);
            double b = Double.parseDouble(right);
            switch (c.op) {
                case "=": return a == b;
                case "!=": return a != b;
                case ">": return a > b;
                case "<": return a < b;
                case ">=": return a >= b;
                case "<=": return a <= b;
                default: return false;
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
            default: return false;
        }
    }

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}