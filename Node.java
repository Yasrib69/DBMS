public class Node {
    int key;
    Record record;
    Node left;
    Node right;

    public Node(int key, Record record) {
        this.key = key;
        this.record = record;
    }
}