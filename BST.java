import java.util.*;

public class BST {
    Node root;

    public void insert(int key, Record r) {
        root = insertRec(root, key, r);
    }

    private Node insertRec(Node root, int key, Record r) {
        if (root == null) return new Node(key, r);

        if (key < root.key) root.left = insertRec(root.left, key, r);
        else root.right = insertRec(root.right, key, r);

        return root;
    }

    public void inorder(Node root, List<Record> result) {
        if (root != null) {
            inorder(root.left, result);
            result.add(root.record);
            inorder(root.right, result);
        }
    }

    public List<Record> getAllRecords() {
        List<Record> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }
}