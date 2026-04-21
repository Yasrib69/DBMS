public class Record {
    String[] values;

    public Record(String[] values) {
        this.values = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            this.values[i] = values[i].trim().replace("\"","");
        }
    }
}