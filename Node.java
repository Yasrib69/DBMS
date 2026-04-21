public class Node {
    int key;
    Record record;
    Node left, right;

    public Node(int key, Record record) {
        this.key = key;
        this.record = record;
    }
}