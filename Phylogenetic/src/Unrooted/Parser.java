package Unrooted;

import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class Parser {

    /**
     * Reads a .tree file that contains exactly two Newick trees (each on one line).
     */
    public List<PhylogeneticTree> parseFileWithTwoTrees(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));

       // System.out.println(lines.get(0));
        
        if (lines.size() != 2) {
            throw new IllegalArgumentException("Expected exactly 2 trees in the file: " + filePath);
        }

        List<PhylogeneticTree> trees = new ArrayList<>();
        for (String line : lines) {
            trees.add(parse(line.trim()));
        }
        return trees;
    }

    public PhylogeneticTree parseGivenString(String string){
        return parse(string);
    }
    
    public PhylogeneticTree parse(String newick) {
        Stack<TreeNode> stack = new Stack<>();
        PhylogeneticTree tree = new PhylogeneticTree();

        TreeNode current = null;
        StringBuilder label = new StringBuilder();

        for (char ch : newick.toCharArray()) {
            if (ch == '(') {
                stack.push(current);
                current = new TreeNode();  // create new internal node
                current.isLeaf = false;
                tree.addNode(current);
            } else if (ch == ',') {
                if (label.length() > 0) {
                    TreeNode leaf = createLeaf(label.toString(), tree);
                    tree.addEdge(current, leaf);
                    label.setLength(0);
                }
            } else if (ch == ')') {
                if (label.length() > 0) {
                    TreeNode leaf = createLeaf(label.toString(), tree);
                    tree.addEdge(current, leaf);
                    label.setLength(0);
                }

                TreeNode child = current;
                current = stack.pop();

                if (current != null) {
                    tree.addEdge(current, child);
                } else {
                    current = child;  // this was the root
                }
            } else if (ch == ';') {
                continue; // end of Newick
            } else if (!Character.isWhitespace(ch)) {
                label.append(ch);
            }
        }

        return tree;
    }

    private TreeNode createLeaf(String labelStr, PhylogeneticTree tree) {
        TreeNode leaf = new TreeNode(labelStr);
        leaf.isLeaf = true;
        tree.addNode(leaf);
        return leaf;
    }
}