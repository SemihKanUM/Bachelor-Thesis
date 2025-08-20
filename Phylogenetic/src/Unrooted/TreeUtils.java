package Unrooted;

import java.util.*;

public class TreeUtils {

    // Helper: DFS path finder
    public static List<TreeNode> findPath(TreeNode current, TreeNode target, List<TreeNode> path, List<TreeNode> visited) { 

        path.add(current);
        visited.add(current);
        if (current == target) {
            return new ArrayList<>(path);
        }
        for (TreeNode neighbor : current.neighbors) {
            if (!visited.contains(neighbor)) {
                List<TreeNode> result = findPath(neighbor, target, path, visited);
                if (result != null) {
                    return result;
                }
            }
        } 
        path.remove(path.size() - 1); // backtrack
        return null;
    }

    // Remove singleton trees from the forest and also remove the corresponding taxon from T_local. And Return the String label of the deleted taxon
    public static ArrayList<String> removeSingletons(Forest forest, PhylogeneticTree T_local) {
        ArrayList<String> deletedLabels = new ArrayList<>();

        List<PhylogeneticTree> toRemove = new ArrayList<>();
        for (PhylogeneticTree tree : forest.components) {

            if(tree.nodes.size() == 2){
                if((tree.nodes.get(0).isLeaf && !tree.nodes.get(1).isLeaf) || (tree.nodes.get(1).isLeaf && !tree.nodes.get(0).isLeaf)){
                    if(tree.nodes.get(0).isLeaf){
                        deletedLabels.add(tree.nodes.get(0).label);
                        removeLeafFromTree(T_local, tree.nodes.get(0).label);
                        
                    }
                    else{
                        deletedLabels.add(tree.nodes.get(1).label);
                        removeLeafFromTree(T_local, tree.nodes.get(1).label);
                    }
                    toRemove.add(tree);
                    //System.out.println(tree.nodes.get(0).isLeaf + " " + tree.nodes.get(1).isLeaf);
                }
            }

            if (tree.nodes.size() == 1) {
                TreeNode single = tree.nodes.get(0);
                if (single.isLeaf) {
                    deletedLabels.add(single.label);
                    //System.out.println(single.label);
                    removeLeafFromTree(T_local, single.label);
                    toRemove.add(tree);
                }
            }

            if(tree.nodes.size() >= 3){
                for (int i = 0; i < tree.nodes.size(); i++) {
                    if(tree.nodes.get(i).neighbors.isEmpty()){
                        tree.nodes.remove(i);
                    }
                }
            }
        }
        forest.components.removeAll(toRemove);

        boolean allInternal = true;
        for (int i = 0; i < T_local.nodes.size(); i++) {
            if(T_local.nodes.get(i).isLeaf){
                allInternal = false;
            }
        }
        if(allInternal){
            T_local.nodes.clear();
        }

        return deletedLabels;
    }

    // Remove a given leaf (by label) from the provided tree.
    public static void removeLeafFromTree(PhylogeneticTree tree, String label) {
        TreeNode target = null;
        for (TreeNode node : tree.nodes) {
            if (node.isLeaf && node.label.equals(label)) {
                target = node;
                break;
            }
        }
        if (target != null) {
            for (TreeNode neighbor : new ArrayList<>(target.neighbors)) {
                tree.cutEdge(target, neighbor);
            }
            tree.removeNode(target);
        }
    }


    // Check whether forest F is an agreement forest for T_local.
    public static boolean isAgreementForest(PhylogeneticTree T_local, Forest F) {

        Forest Fcopy = new Forest();
        Fcopy = F.copyForest();

        PhylogeneticTree Tcopy = new PhylogeneticTree();
        Tcopy = T_local.copyTree();
        
        // For every component, restrict T_local to that component's leaves and check homeomorphism.
        for (PhylogeneticTree component : Fcopy.components) {
            List<String> leaves = new ArrayList<>();
            for (TreeNode node : component.nodes) {
                if (node.isLeaf) {
                    leaves.add(node.label);
                }
            }
            PhylogeneticTree restrictedT = Tcopy.getEmbedding(leaves);

            if (!TreeUtils.areHomeomorphic(restrictedT, component)) {
                return false;
            }
        }
        // T_local.printAdjacencyList();
        // F.printForest();
        return true;
    }
    

    // Reduce common cherries between T_local and every component in forest.
    // (Uses TreeUtils.collapseCherry, which now works by label so that both trees are updated properly.)
    public static boolean findAndReduceCommonCherries(Forest forest, PhylogeneticTree T_local) {
        boolean changeMade = false;
        for (int i = 0; i < forest.components.size(); i++) {

            while (true) {
                PhylogeneticTree t1 = T_local;
                PhylogeneticTree t2 = forest.components.get(i);
                List<List<TreeNode>> commonCherries = TreeUtils.findCommonCherries(t1, t2);
        
                if (commonCherries.isEmpty()) {
                    break; // no more common cherries
                }
                
                changeMade = true;

                // System.out.println("Reducing cherries in component " + i);
                // System.out.println("Common cherries: " + commonCherries.size());

                List<TreeNode> cherry = commonCherries.get(0); // pick one
                //System.out.println("Reducing cherry: " + cherry.get(0).label + " + " + cherry.get(1).label);

                 // Skip if either label is a merged label
                
                TreeUtils.collapseCherry(t1, cherry.get(0).label,cherry.get(1).label);
                TreeUtils.collapseCherry(t2, cherry.get(0).label,cherry.get(1).label);
    
                t1.getRestriction();
                t2.getRestriction(); 
            }
        }
        return changeMade;
    }

    

    public static boolean areHomeomorphic(PhylogeneticTree t1, PhylogeneticTree t2) {
        // Restrict both trees first
        t1 = t1.getRestriction();
        t2 = t2.getRestriction();

        while (true) {
            List<List<TreeNode>> commonCherries = findCommonCherries(t1, t2);
    
            if (commonCherries.isEmpty()) {
                break; // no more common cherries
            }
    
            List<TreeNode> cherry = commonCherries.get(0); // pick one
            collapseCherry(t1, cherry.get(0).label,cherry.get(1).label);
            collapseCherry(t2, cherry.get(0).label,cherry.get(1).label);

            t1.getRestriction();
            t2.getRestriction();

        }

        return areIdentical(t1, t2);
    }

    public static List<List<TreeNode>> findCommonCherries(PhylogeneticTree t1, PhylogeneticTree t2) {
        List<List<TreeNode>> cherries1 = findCherries(t1);
        List<List<TreeNode>> cherries2 = findCherries(t2);
        
        List<List<TreeNode>> common = new ArrayList<>();
        for (List<TreeNode> c1 : cherries1) {
            Set<String> set1 = new HashSet<>();
            for (TreeNode n : c1) set1.add(n.label);
    
            for (List<TreeNode> c2 : cherries2) {
                Set<String> set2 = new HashSet<>();
                for (TreeNode n : c2) set2.add(n.label);
    
                // If c1 ⊆ c2 or c2 ⊆ c1
                if (set1.containsAll(set2) || set2.containsAll(set1)) {
                    common.add(c1); // or add whichever one you prefer
                    break; // Optional: if one match is enough
                }
            }
        }
        return common;
    }
    
    
    public static boolean isSameCherry(List<TreeNode> c1, List<TreeNode> c2) {
        String l1 = c1.get(0).label, l2 = c1.get(1).label;
        String m1 = c2.get(0).label, m2 = c2.get(1).label;
        return (l1.equals(m1) && l2.equals(m2)) || (l1.equals(m2) && l2.equals(m1));
    }
    
    public static List<List<TreeNode>> findCherries(PhylogeneticTree tree) {

        
        List<List<TreeNode>> cherries = new ArrayList<>();
        for (TreeNode node : tree.nodes) {
            if (!node.isLeaf) {
                List<TreeNode> leafNeighbors = new ArrayList<>();
                for (TreeNode neighbor : node.neighbors) {
                    if (neighbor.isLeaf) {
                        leafNeighbors.add(neighbor);
                    }
                }
                // Add all unordered leaf pairs as cherries
                for (int i = 0; i < leafNeighbors.size(); i++) {
                    for (int j = i + 1; j < leafNeighbors.size(); j++) {
                        List<TreeNode> cherry = Arrays.asList(leafNeighbors.get(i), leafNeighbors.get(j));
                        cherries.add(cherry);
                    }
                }
            }
        }

        if(tree.nodes.size() == 2){
            if(tree.nodes.get(0).isLeaf && tree.nodes.get(1).isLeaf){
                List<TreeNode> cherry = Arrays.asList(tree.nodes.get(0), tree.nodes.get(1));
                cherries.add(cherry);
            }
        }

        return cherries;
    }
    
    
    public static void collapseCherry(PhylogeneticTree tree, String label1, String label2) {
        TreeNode a = findLeafByLabel(tree, label1);
        TreeNode b = findLeafByLabel(tree, label2);
        
        if (a == null || b == null) {
            throw new RuntimeException("Cherry nodes not found!");
        }

        if(tree.nodes.size() == 2){
            if(a.isLeaf && b.isLeaf){
                String mergedLabel = mergeLabels(label1, label2);
                TreeNode merged = new TreeNode(mergedLabel);
                merged.isLeaf = true;
                tree.addNode(merged);

                tree.removeNode(a);
                tree.removeNode(b);
            }
        }
        else{
            TreeNode parent = a.neighbors.get(0);
            if (!b.neighbors.contains(parent)) {
                throw new RuntimeException("Cherry collapse error: leaves do not share parent");
            }
        
            String mergedLabel = mergeLabels(label1, label2);
            TreeNode merged = new TreeNode(mergedLabel);
            merged.isLeaf = true;
            tree.addNode(merged);
        
            parent.neighbors.remove(a);
            parent.neighbors.remove(b);
            parent.neighbors.add(merged);
            merged.neighbors.add(parent);
        
            tree.removeNode(a);
            tree.removeNode(b);
        }
        
    }

    public static TreeNode findLeafByLabel(PhylogeneticTree tree, String label) {
        for (TreeNode node : tree.nodes) {
            if (node.isLeaf && node.label.equals(label)) {
                return node;
            }
        }
        return null;
    }
        

    public static String mergeLabels(String label1, String label2) {
        Set<Integer> values = new TreeSet<>(); // TreeSet keeps things sorted
    
        for (String part : label1.split("_")) {
            values.add(Integer.parseInt(part));
        }
        for (String part : label2.split("_")) {
            values.add(Integer.parseInt(part));
        }
    
        List<String> sorted = new ArrayList<>();
        for (int val : values) {
            sorted.add(String.valueOf(val));
        }
    
        return String.join("_", sorted);
    }
    
    public static boolean areIdentical(PhylogeneticTree t1, PhylogeneticTree t2) {
        if (t1.nodes.size() != 2 || t2.nodes.size() != 2) {
            return false;
        }
    
        // Find the two nodes in each tree
        TreeNode t1n1 = t1.nodes.get(0);
        TreeNode t1n2 = t1.nodes.get(1);
        TreeNode t2n1 = t2.nodes.get(0);
        TreeNode t2n2 = t2.nodes.get(1);
    
        // Check degrees (both nodes must have degree 1)
        if (t1n1.neighbors.size() != 1 || t1n2.neighbors.size() != 1 ||
            t2n1.neighbors.size() != 1 || t2n2.neighbors.size() != 1) {
            return false;
        }
    
        // Check labels: sets must match
        Set<String> t1Labels = new HashSet<>(Arrays.asList(t1n1.label, t1n2.label));
        Set<String> t2Labels = new HashSet<>(Arrays.asList(t2n1.label, t2n2.label));
    
        return t1Labels.equals(t2Labels);
    }

    public static Set<List<TreeNode>> getOriginalEdgesInEmbedding(PhylogeneticTree tree, List<String> leafLabels) {
        List<TreeNode> leafNodes = new ArrayList<>();
        for (TreeNode node : tree.nodes) {
            if (node.isLeaf && leafLabels.contains(node.label)) {
                leafNodes.add(node);
            }
        }
    
        Set<List<TreeNode>> edgeSet = new HashSet<>();
    
        for (int i = 0; i < leafNodes.size(); i++) {
            for (int j = i + 1; j < leafNodes.size(); j++) {
                TreeNode start = leafNodes.get(i);
                TreeNode end = leafNodes.get(j);
    
                List<TreeNode> path = TreeUtils.findPath(start, end, new ArrayList<>(), new ArrayList<>());
                if (path != null) {
                    for (int k = 0; k < path.size() - 1; k++) {
                        TreeNode u = path.get(k);
                        TreeNode v = path.get(k + 1);
    
                        List<TreeNode> edge = (System.identityHashCode(u) < System.identityHashCode(v)) ?
                                Arrays.asList(u, v) : Arrays.asList(v, u);
    
                        edgeSet.add(edge);
                    }
                }
            }
        }
        return edgeSet;
    }
}