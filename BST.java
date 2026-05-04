import java.util.ArrayList;
import java.util.List;

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

    public void inorder(Node node, List<Record> result) {
        if (node != null) {
            inorder(node.left, result);
            result.add(node.record);
            inorder(node.right, result);
        }
    }

    public void inorderKeys(Node node, List<Integer> result) {
        if (node != null) {
            inorderKeys(node.left, result);
            result.add(node.key);
            inorderKeys(node.right, result);
        }
    }

    public List<Record> getAllRecords() {
        List<Record> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    public List<Integer> getAllKeys() {
        List<Integer> result = new ArrayList<>();
        inorderKeys(root, result);
        return result;
    }
}