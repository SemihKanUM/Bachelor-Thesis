package Unrooted;

public class TreeBuilder {
    private PhylogeneticTree tree;

    public TreeBuilder() {
        tree = new PhylogeneticTree();
    }

    public PhylogeneticTree buildTree1() {
        cleanTree();

        // Leaves
        TreeNode zero = addLeaf("0");
        TreeNode one = addLeaf("1");
        TreeNode two = addLeaf("2");
        TreeNode three = addLeaf("3");
        TreeNode four = addLeaf("4");
        TreeNode five = addLeaf("5");
        TreeNode six = addLeaf("6");
        TreeNode seven = addLeaf("7");
        TreeNode eight = addLeaf("8");
        TreeNode nine = addLeaf("9");

        // Internal nodes
        TreeNode int1 = addInternalNode();
        TreeNode int2 = addInternalNode();
        TreeNode int3 = addInternalNode();
        TreeNode int4 = addInternalNode();
        TreeNode int5 = addInternalNode();
        TreeNode int6 = addInternalNode();
        TreeNode int7 = addInternalNode();
        TreeNode int8 = addInternalNode();

        // Build tree
        connect(four, int1);
        connect(eight, int1);

        connect(int1, int2);
        connect(one, int2);

        connect(int2, int3);
        connect(int3, int4);

        connect(two, int4);
        connect(int5, int4);

        connect(seven, int5);
        connect(five,int5);

        connect(int3, int6);
        connect(int6,int7);
        connect(int6, int8);

        connect(int7, three);
        connect(int7, nine);

        connect(int8, zero);
        connect(int8, six);

        return tree;
    }

    public PhylogeneticTree buildTree2() {
        cleanTree();

        // Leaves
        TreeNode zero = addLeaf("0");
        TreeNode one = addLeaf("1");
        TreeNode two = addLeaf("2");
        TreeNode three = addLeaf("3");
        TreeNode four = addLeaf("4");
        TreeNode five = addLeaf("5");
        TreeNode six = addLeaf("6");
        TreeNode seven = addLeaf("7");
        TreeNode eight = addLeaf("8");
        TreeNode nine = addLeaf("9");

        // Internal nodes
        TreeNode int1 = addInternalNode();
        TreeNode int2 = addInternalNode();
        TreeNode int3 = addInternalNode();
        TreeNode int4 = addInternalNode();
        TreeNode int5 = addInternalNode();
        TreeNode int6 = addInternalNode();
        TreeNode int7 = addInternalNode();
        TreeNode int8 = addInternalNode();

        // Build tree
        connect(seven,int1);
        connect(three, int1);

        connect(int1, int2);
        connect(five, int2);
        connect(int2, int3);

        connect(six,int3);

        connect(int3, int4);
        connect(int4, int5);

        connect(int5,nine);
        connect(int5, two);

        connect(int4,int6);
        connect(int6,four);

        connect(int6,int7);
        connect(int7,eight);

        connect(int7, int8);

        connect(int8,zero);
        connect(int8,one);

        return tree;
    }

    public void cleanTree() {
        tree = new PhylogeneticTree();
    }

    public TreeNode addLeaf(String label) {
        TreeNode leaf = new TreeNode(label);
        leaf.isLeaf = true;
        tree.addNode(leaf);
        return leaf;
    }

    public TreeNode addInternalNode() {
        TreeNode node = new TreeNode();
        node.isLeaf = false;
        tree.addNode(node);
        return node;
    }

    public void connect(TreeNode n1, TreeNode n2) {
        tree.addEdge(n1, n2);
    }
}