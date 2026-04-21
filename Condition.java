import java.util.*;

public class Condition {

    static class Clause {
        String attr, op, value;
    }

    List<Clause> clauses = new ArrayList<>();
    List<String> ops = new ArrayList<>();

    public Condition(String where) {

        String[] parts = where.split("(?i) AND | OR ");

        for (String part : parts) {
            Clause c = new Clause();

            if (part.contains(">=")) c.op=">=";
            else if (part.contains("<=")) c.op="<=";
            else if (part.contains("!=")) c.op="!=";
            else if (part.contains("=")) c.op="=";
            else if (part.contains(">")) c.op=">";
            else if (part.contains("<")) c.op="<";

            String[] sides = part.split(c.op);
            c.attr = sides[0].trim();
            c.value = sides[1].replace("\"","").trim();

            clauses.add(c);
        }

        if (where.toUpperCase().contains("AND")) ops.add("AND");
        if (where.toUpperCase().contains("OR")) ops.add("OR");
    }

    public boolean evaluate(Record r, List<String> cols) {
        boolean result = eval(r, cols, clauses.get(0));

        for (int i = 1; i < clauses.size(); i++) {
            boolean next = eval(r, cols, clauses.get(i));

            if (ops.get(i-1).equals("AND")) result &= next;
            else result |= next;
        }

        return result;
    }

    private boolean eval(Record r, List<String> cols, Clause c) {
        int idx = cols.indexOf(c.attr);
        String val = r.values[idx];

        int cmp = val.compareToIgnoreCase(c.value);

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
}