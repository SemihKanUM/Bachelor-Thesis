package Unrooted;

import java.util.*;

public class Forest {
    List<PhylogeneticTree> components;

    public Forest() {
        components = new ArrayList<>();
    }

    public void addComponent(PhylogeneticTree tree) {
        components.add(tree);
    }

    public void removeComponent(PhylogeneticTree tree) {
        components.remove(tree);
    }
    
    public void mergeEdge(TreeNode u, TreeNode v) {
        PhylogeneticTree treeU = null;
        PhylogeneticTree treeV = null;
    
        for (PhylogeneticTree tree : components) {
            if (tree.nodes.contains(u)) treeU = tree;
            if (tree.nodes.contains(v)) treeV = tree;
        }
    
        if (treeU == null || treeV == null) {
            throw new RuntimeException("Cannot merge: node(s) not found in any component.");
        }
    
        // Step 1: Add edge
        u.neighbors.add(v);
        v.neighbors.add(u);
    
        // Step 2: If u and v are in different components, merge them
        if (treeU != treeV) {
            treeU.nodes.addAll(treeV.nodes);
            components.remove(treeV);
        }
    }

    public void cutEdgeInComponent(TreeNode n1, TreeNode n2) {
        PhylogeneticTree targetTree = null;
    
        // Step 1: Find the component containing both nodes
        for (PhylogeneticTree tree : components) {
            if (tree.nodes.contains(n1) && tree.nodes.contains(n2)) {
                targetTree = tree;
                break;
            }
        }
    
        if (targetTree == null) {
            throw new RuntimeException("Cannot cut edge: nodes not found in the same component.");
        }
    
        // // Step 2: Cut the edge in that component
        // targetTree.cutEdge(n1, n2);
    
        // Step 3: Split into two trees
        List<PhylogeneticTree> newTrees = targetTree.splitIntoTwo(n1, n2);
    
        // Step 4: Update the forest
        removeComponent(targetTree);
        components.addAll(newTrees);
    }
    
    

    //CUT OFF A LEAF
    public void cutOff(String label) {
        TreeNode leaf = null;
        PhylogeneticTree sourceTree = null;
    
        // Step 1: Locate the leaf node and the tree it belongs to
        for (PhylogeneticTree tree : components) {
            TreeNode candidate = TreeUtils.findLeafByLabel(tree, label);
            if (candidate != null) {
                leaf = candidate;
                sourceTree = tree;
                break;
            }
        }
    
        if (leaf == null || sourceTree == null) {
            printForest();
            System.err.println("❌ Leaf with label '" + label + "' not found.");
            return;
        }
    
        if (!leaf.isLeaf) {
            printForest();
            System.err.println("❌ Node '" + label + "' is not a leaf.");
            return;
        }
    
        if (leaf.neighbors.size() != 1) {
            printForest();
            System.err.println("❌ Leaf '" + label + "' does not have exactly one neighbor.");
            return;
        }
    
        // Step 2: Remove the edge to its neighbor
        TreeNode neighbor = leaf.neighbors.get(0);
        sourceTree.cutEdge(leaf, neighbor);
    
        // Step 3: Remove the leaf from the original tree
        sourceTree.removeNode(leaf);
    
        // Step 4: Add the leaf as its own new tree in the forest
        PhylogeneticTree singletonTree = new PhylogeneticTree();
        singletonTree.addNode(leaf);
        components.add(singletonTree);
    
        // // Optional: clean up the source tree (e.g., suppress degree-2 nodes)
        // sourceTree.suppressDegree2Nodes();
    }
    
    

    public void printForest() {
        System.out.println("Forest with " + components.size() + " trees:");
        for (PhylogeneticTree tree : components) {
            tree.printAdjacencyList();
            System.out.println("---");
        }
    }

    public static TreeNode findEquivalentNode(Forest copyForest, Forest originalForest, TreeNode nodeOriginal) {

        // First, find which tree (component) in the original forest contains the original node
        PhylogeneticTree originalComponent = null;
        int componentIndex = -1;
        for (int i = 0; i < originalForest.components.size(); i++) {
            PhylogeneticTree tree = originalForest.components.get(i);
            if (tree.nodes.contains(nodeOriginal)) {
                originalComponent = tree;
                componentIndex = i;
                break;
            }
        }

        if (originalComponent == null) {
            throw new RuntimeException("Node not found in original forest!");
        }

        // Find the index of the node inside the nodes list
        int nodeIndex = originalComponent.nodes.indexOf(nodeOriginal);
        if (nodeIndex == -1) {
            throw new RuntimeException("Node not found inside its component!");
        }

        // Now go to the corresponding component in the copy forest
        if (componentIndex >= copyForest.components.size()) {
            throw new RuntimeException("Component index out of bounds in copy forest!");
        }

        PhylogeneticTree copyComponent = copyForest.components.get(componentIndex);

        if (nodeIndex >= copyComponent.nodes.size()) {
            throw new RuntimeException("Node index out of bounds in copy component!");
        }

        // Return the node at the same index
        return copyComponent.nodes.get(nodeIndex);
    }

    // Deep-copy a forest by copying every PhylogeneticTree.
    public Forest copyForest() {
        Forest original = this;
        Forest copy = new Forest();
        for (PhylogeneticTree tree : original.components) {
            PhylogeneticTree treeCopy = tree.copyTree();
            copy.addComponent(treeCopy);
        }
        return copy;
    }

    public Map<PhylogeneticTree, PhylogeneticTree> copyForestWithMapping(Forest forestCopyOut) {
        Map<PhylogeneticTree, PhylogeneticTree> treeMap = new HashMap<>();
    
        for (PhylogeneticTree originalTree : this.components) {
            PhylogeneticTree copiedTree = originalTree.copyTree();
            forestCopyOut.addComponent(copiedTree);
            treeMap.put(originalTree, copiedTree);
        }
    
        return treeMap;
    }  
}