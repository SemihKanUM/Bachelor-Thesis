package Unrooted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PhylogeneticTree {
    List<TreeNode> nodes;

    PhylogeneticTree() {
        nodes = new ArrayList<>();
    }

    public void addNode(TreeNode node) { 
        nodes.add(node);
    }

    public void removeNode(TreeNode node){
        nodes.remove(node);
    }

    void addEdge(TreeNode node1, TreeNode node2) {
        node1.neighbors.add(node2);
        node2.neighbors.add(node1);
    }

    void cutEdge(TreeNode node1, TreeNode node2) {
        node1.neighbors.remove(node2);
        node2.neighbors.remove(node1);
    }

    public void printAdjacencyList() {
        System.out.println("Unrooted Tree (Adjacency List):");
        for (TreeNode node : nodes) {
            String label = node.isLeaf ? node.label : "*";
            System.out.print(label + " → ");
            List<String> neighborLabels = new ArrayList<>();
            for (TreeNode neighbor : node.neighbors) {
                neighborLabels.add(neighbor.isLeaf ? neighbor.label : "*");
            }
            System.out.println(String.join(", ", neighborLabels));
        }
    }

    //Implementation of Embedding with given set of leaves
    //For each pair of leaf labels, find the path between them
    //Collect all unique nodes and edges that are used in any of those paths
    //Build and return a new PhylogeneticTree using those nodes and edges
    public PhylogeneticTree getEmbedding(List<String> leafLabels) {
        List<TreeNode> leafNodes = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf && leafLabels.contains(node.label)) {
                leafNodes.add(node);
            }
        }
    
        List<TreeNode> collectedNodes = new ArrayList<>();
        List<List<TreeNode>> collectedEdges = new ArrayList<>();
    
        for (int i = 0; i < leafNodes.size(); i++) {
            for (int j = i + 1; j < leafNodes.size(); j++) {
                TreeNode start = leafNodes.get(i);
                TreeNode end = leafNodes.get(j);
                List<TreeNode> path = TreeUtils.findPath(start, end, new ArrayList<>(), new ArrayList<>());
                if (path != null) {
                    for (TreeNode node : path) {
                        if (!collectedNodes.contains(node)) {
                            collectedNodes.add(node);
                        }
                    }
                    for (int k = 0; k < path.size() - 1; k++) {
                        List<TreeNode> edge = new ArrayList<>();
                        edge.add(path.get(k));
                        edge.add(path.get(k + 1));
                        if (!edgeExists(edge, collectedEdges)) {
                            collectedEdges.add(edge);
                        }
                    }
                }
            }
        }
    
        // Create a new tree with copies of the collected nodes
        PhylogeneticTree embedded = new PhylogeneticTree();
        List<TreeNode> newNodes = new ArrayList<>();
    
        for (TreeNode original : collectedNodes) {
            TreeNode copy = original.isLeaf ? new TreeNode(original.label) : new TreeNode();
            copy.isLeaf = original.isLeaf;
            embedded.addNode(copy);
            newNodes.add(copy);
        }
    
        // Connect edges using position/index in collectedNodes
        for (List<TreeNode> edge : collectedEdges) {
            int idx1 = collectedNodes.indexOf(edge.get(0));
            int idx2 = collectedNodes.indexOf(edge.get(1));
            embedded.addEdge(newNodes.get(idx1), newNodes.get(idx2));
        }
    
        return embedded;
    }
    
    // Helper: check if edge already exists
    private boolean edgeExists(List<TreeNode> edge, List<List<TreeNode>> edgeList) {
        for (List<TreeNode> e : edgeList) {
            if ((e.get(0) == edge.get(0) && e.get(1) == edge.get(1)) ||
                (e.get(0) == edge.get(1) && e.get(1) == edge.get(0))) {
                return true;
            }
        }
        return false;
    }

    //Logic for restrcition:
    //Traverse the subtree
    //While any internal node has degree = 2:
    //Remove it
    //Reconnect its two neighbors directly
    //Return the new tree
    public PhylogeneticTree getRestriction() {
        // Step 1: Get the embedding
        PhylogeneticTree embedded = this;
    
        boolean changed;
        do {
            changed = false;
            List<TreeNode> toRemove = new ArrayList<>();
    
            for (TreeNode node : new ArrayList<>(embedded.nodes)) {
                if (!node.isLeaf && node.neighbors.size() == 2) {
                    // Suppress this node by reconnecting its neighbors
                    TreeNode n1 = node.neighbors.get(0);
                    TreeNode n2 = node.neighbors.get(1);
    
                    // Cut original edges
                    embedded.cutEdge(node, n1);
                    embedded.cutEdge(node, n2);
    
                    // Add direct edge between n1 and n2
                    embedded.addEdge(n1, n2);
    
                    // Remove the node from the tree
                    embedded.removeNode(node);
                    toRemove.add(node);
                    changed = true;
                }
            }
    
            // Clean up suppressed nodes
            embedded.nodes.removeAll(toRemove);
    
        } while (changed); // Repeat until no more degree-2 internal nodes
    
        return embedded;
    }

    public List<PhylogeneticTree> splitIntoTwo(TreeNode n1, TreeNode n2) {
        
        cutEdge(n1, n2);
        // Step 1: Collect nodes reachable from n1
        Set<TreeNode> visited1 = new HashSet<>();
        dfs(n1, visited1);
    
        // Step 2: Collect nodes reachable from n2
        Set<TreeNode> visited2 = new HashSet<>();
        dfs(n2, visited2);
    
        // Step 3: Build two new trees
        PhylogeneticTree tree1 = new PhylogeneticTree();
        tree1.nodes.addAll(visited1);
    
        PhylogeneticTree tree2 = new PhylogeneticTree();
        tree2.nodes.addAll(visited2);
    
        // Step 4: Clean up neighbor lists
        cleanNeighbors(tree1, visited1);
        cleanNeighbors(tree2, visited2);
    
        List<PhylogeneticTree> result = new ArrayList<>();
        result.add(tree1);
        result.add(tree2);
    
        return result;
    }
    
    
    // Reuse your DFS
    private void dfs(TreeNode node, Set<TreeNode> visited) {
        if (visited.contains(node)) return;
        visited.add(node);
        for (TreeNode neighbor : node.neighbors) {
            dfs(neighbor, visited);
        }
    }
    
    // New helper: clean neighbors properly
    private void cleanNeighbors(PhylogeneticTree tree, Set<TreeNode> allowed) {
        for (TreeNode node : tree.nodes) {
            List<TreeNode> newNeighbors = new ArrayList<>();
            for (TreeNode neighbor : node.neighbors) {
                if (allowed.contains(neighbor)) {
                    newNeighbors.add(neighbor);
                }
            }
            node.neighbors = newNeighbors; // Update the full neighbor list
        }
    }   

    // Revised deep-copy method for a phylogenetic tree.
    public PhylogeneticTree copyTree() {
        PhylogeneticTree original = this;
        // First, snapshot the list of nodes from the original tree.
        List<TreeNode> originalNodes = new ArrayList<>(original.nodes);
        PhylogeneticTree newTree = new PhylogeneticTree();
        List<TreeNode> copiedNodes = new ArrayList<>();
        
        // Create new copies of every node.
        for (TreeNode node : originalNodes) {
            TreeNode copy = node.isLeaf ? new TreeNode(node.label) : new TreeNode();
            copy.isLeaf = node.isLeaf;
            copiedNodes.add(copy);
            newTree.addNode(copy);
        }
        
        // Re-establish neighbor relationships based on the index in the snapshot.
        for (int i = 0; i < originalNodes.size(); i++) {
            TreeNode orig = originalNodes.get(i);
            TreeNode copy = copiedNodes.get(i);
            for (TreeNode neighbor : orig.neighbors) {
                int idx = originalNodes.indexOf(neighbor);
                if (idx != -1) {
                    TreeNode copyNeighbor = copiedNodes.get(idx);
                    // Add the neighbor if it is not already present. 
                    if (!copy.neighbors.contains(copyNeighbor)) {
                        copy.neighbors.add(copyNeighbor);
                    }
                }
            }
        }
        return newTree;
    }

    public Map<TreeNode, TreeNode> copyTreeWithMapping(PhylogeneticTree outputCopy) {
        Map<TreeNode, TreeNode> nodeMap = new HashMap<>();
        List<TreeNode> originalNodes = new ArrayList<>(this.nodes);
    
        // Step 1: Copy nodes
        for (TreeNode original : originalNodes) {
            TreeNode copy = original.isLeaf ? new TreeNode(original.label) : new TreeNode();
            copy.isLeaf = original.isLeaf;
            nodeMap.put(original, copy);
            outputCopy.addNode(copy);
        }
    
        // Step 2: Copy neighbor relationships
        for (TreeNode original : originalNodes) {
            TreeNode copy = nodeMap.get(original);
            for (TreeNode neighbor : original.neighbors) {
                TreeNode copyNeighbor = nodeMap.get(neighbor);
                if (copyNeighbor != null && !copy.neighbors.contains(copyNeighbor)) {
                    copy.neighbors.add(copyNeighbor);
                }
            }
        }
    
        return nodeMap;
    }
    
    // Suppress degree‑2 internal nodes in the given tree. If both neighbours are internal then remove. Also, if one neighbour is leaf and the other is internal also remove. 
    public boolean suppressDegree2Nodes() {
        boolean changeMade = false;

        PhylogeneticTree tree = this;
        boolean changed;
        do {
            changed = false;
            List<TreeNode> toRemove = new ArrayList<>();
            for (TreeNode node : new ArrayList<>(tree.nodes)) {
                if (!node.isLeaf && node.neighbors.size() == 2) {
                    if(!(node.neighbors.get(0).isLeaf && node.neighbors.get(1).isLeaf)){ // Both the neighbours will not be leaf
                        TreeNode n1 = node.neighbors.get(0);
                        TreeNode n2 = node.neighbors.get(1);
                        tree.cutEdge(node, n1);
                        tree.cutEdge(node, n2);
                        tree.addEdge(n1, n2);
                        toRemove.add(node);
                        changed = true;
                        changeMade = true;
                    }
                }
            }
            tree.nodes.removeAll(toRemove);
        } while (changed);
        return changeMade;
    }

    public boolean isCherry(){
        int counter = 0;
        if(nodes.size() == 3){
            for (int i = 0; i < nodes.size(); i++) {
                if(nodes.get(i).isLeaf){
                    counter++;
                }
            }
            if(counter == 2){
                return true;
            }
        }
        if(nodes.size() ==2){
            if(nodes.get(0).isLeaf && nodes.get(1).isLeaf){
                return true;
            }
        }
        return false;
    }

    public List<String> getLeafLabels() {
        List<String> labels = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (node.isLeaf) {
                
                labels.add(new String(node.label));
            }
        }
        return labels;
    }   
}