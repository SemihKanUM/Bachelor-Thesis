package Unrooted;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChenAlgorithm {
    public  PhylogeneticTree T; // Tree T
    public  Forest F; // Forest F' 
    private int solutionCount = 0;
    private static final int MAX_SOLUTIONS = 10;

    public  int recursionCounter;

    public ChenAlgorithm(PhylogeneticTree T, PhylogeneticTree Tprime) {
        this.T = T;
        this.F = new Forest();
        this.F.addComponent(Tprime);
    }
    
    // Public method: start the algorithm by calling solve with deep-copied T.
    public boolean solve(int k) {
        return solve(this.T.copyTree(), this.F.copyForest(), k , new ArrayList<>());
    }
    
    // The main recursive method; now explicitly passes a local tree T_local.
    public boolean solve(PhylogeneticTree T_local, Forest forest, int k, ArrayList<String> deletedLabels) {
        if (solutionCount >= MAX_SOLUTIONS) return false;
        recursionCounter++;
        while(true){
            // Tidy-up operations on the local tree and on each forest component.
            boolean supres1 = T_local.suppressDegree2Nodes();

            boolean supres2 = false;
            for (PhylogeneticTree tree : forest.components) {
                if(tree.suppressDegree2Nodes()){
                    supres2 = true;
                }
            }

            boolean singleton = false;
            ArrayList<String> deletedSingletons = TreeUtils.removeSingletons(forest, T_local);
            if(deletedSingletons.size() != 0){
                singleton = true;
                deletedLabels.addAll(deletedSingletons);
            }
            
            boolean reduction = TreeUtils.findAndReduceCommonCherries(forest, T_local);
            //boolean reduction  = false;
            if(!(supres1 || supres2|| singleton || reduction)){
                break;
            }
        }
    
        if(k<0){
            return false;
        }        

        
        // Try to pick a cherry from the local tree.
        List<List<TreeNode>> cherries = TreeUtils.findCherries(T_local);
        if(cherries.size() > 0){
            List<TreeNode> cherry = cherries.get(0);
            TreeNode a = cherry.get(0);
            TreeNode b = cherry.get(1);
    
            // Check using labels whether a and b lie in different components.
            if (inDifferentComponents(forest, a.label, b.label , T_local)) {
                // DIFFERENT COMPONENTS: Try cutting off a OR cutting off b.

                //System.out.println(a.label + " " + b.label);

                Forest copy1 = forest.copyForest();
                copy1.cutOff(a.label);

                Forest copy2 = forest.copyForest();
                copy2.cutOff(b.label);
    
                return solve(T_local.copyTree(), copy1, k - 1 , new ArrayList<>(deletedLabels) ) 
                    || solve(T_local.copyTree(), copy2, k - 1 , new ArrayList<>(deletedLabels));
            } else {
                // System.out.println(a.label + " " + b.label + " " + k);
                // SAME COMPONENT: Use Chen's branching rule (three branches)
                return applyChenBranching(T_local.copyTree(), forest.copyForest(), a.label, b.label, k , new ArrayList<>(deletedLabels));
            }
        }

        //If no cherries remain, check whether the forest is an agreement forest for T_local.
        if (TreeUtils.isAgreementForest(T_local, forest)) {
            
            if(T_local.nodes.size() != 0){
                List<String> labels = new ArrayList<>();
                for (TreeNode node : T_local.nodes) {
                    if (node.isLeaf) {
                        labels.add(node.label);
                    }
                }
                // Sort for consistency and concatenate
                Collections.sort(labels);
                String combined = String.join("_", labels);
                deletedLabels.add(combined);
            }

            //System.out.println(deletedLabels);
            
            solutionCount++;
            return true;
        }

        return false; // No applicable cherries and not an agreement forest.
    }

    // Chen branching: branch on cutting off a, cutting off b, or splitting the path between a and b.
    private boolean applyChenBranching(PhylogeneticTree T_local, Forest forest, String a, String b, int k , ArrayList<String> deletedLabels) {

        //Branch 1: Cut off a.
        Forest copy1 = forest.copyForest(); 
        copy1.cutOff(a);
        boolean branch1 = solve(T_local.copyTree(), copy1, k - 1 , new ArrayList<>(deletedLabels));

        // Branch 2: Cut off b.
        Forest copy2 = forest.copyForest();
        copy2.cutOff(b);
        boolean branch2 = solve(T_local.copyTree(), copy2, k - 1 , new ArrayList<>(deletedLabels));

        //Branch 3: Split the path between a and b.
        Forest copy3 = forest.copyForest();
        boolean branch3 = splitPath(T_local.copyTree(), copy3, a, b, k , new ArrayList<>(deletedLabels));
        
        return branch1 || branch2 || branch3 ;
        //return branch3;
    }

    // Check whether nodes with labels a and b are in different forest components.
    private boolean inDifferentComponents(Forest forest, String a, String b , PhylogeneticTree T_local) {
        PhylogeneticTree compA = null;
        PhylogeneticTree compB = null;

        for (PhylogeneticTree tree : forest.components) { 
            if (TreeUtils.findLeafByLabel(tree, a) != null) {
                compA = tree;
            }
            if (TreeUtils.findLeafByLabel(tree, b) != null) {
                compB = tree;
            }
        }

        if (compA == null || compB == null) {
            forest.printForest();
            //T_local.printAdjacencyList();
            //return true;
            System.out.println(a + " " + b);
            throw new RuntimeException("One of the nodes was not found in any component.");
        }
        return (compA != compB);
    }

    // Split the path between nodes with labels aLabel and bLabel.
    // This branch tries all choices of keeping one side edge and cutting the others.
    private boolean splitPath(PhylogeneticTree T_local, Forest forest, String aLabel, String bLabel, int k , ArrayList<String> deletedLabels)  {
        TreeNode a = null;
        TreeNode b = null;
        PhylogeneticTree component = null;

        for (PhylogeneticTree tree : forest.components) {
            a = TreeUtils.findLeafByLabel(tree, aLabel);
            b = TreeUtils.findLeafByLabel(tree, bLabel);
            if(a != null && b!= null) {
                component = tree;
                break;
            }
        }

        if (a == null || b == null) {
            throw new RuntimeException("Nodes with label a or b not found in any component!");
        }
        if (component == null) {
            throw new RuntimeException("Nodes a and b are not in the same component!");
        }

        // Find the unique path between a and b.
        List<TreeNode> path = TreeUtils.findPath(a, b, new ArrayList<>(), new ArrayList<>()); 
        if (path == null) {
            throw new RuntimeException("No path found between a and b!");
        }
        

        // Identify side edges: edges incident to the path.
        List<List<TreeNode>> sideEdges = new ArrayList<>();
        for (TreeNode node : path) {
            for (TreeNode neighbor : node.neighbors) {
                if (!path.contains(neighbor)) {
                    sideEdges.add(Arrays.asList(node, neighbor));
                }
            }
        }

        if (sideEdges.size() <= 1) return false;  // trivial or degenerate

        for (List<TreeNode> edge : sideEdges) {
            TreeNode u = edge.get(0);
            TreeNode v = edge.get(1);
            forest.cutEdgeInComponent(u, v);
        }
        
        boolean result = false;

        // For each side edge
        for (List<TreeNode> edge : sideEdges) {
            TreeNode u = edge.get(0);
            TreeNode v = edge.get(1);

            forest.mergeEdge(u, v); // Reconnect

            Forest forestCopy = forest.copyForest(); // Copy with merged edge

            result |= solve(T_local.copyTree(), forestCopy, k - (sideEdges.size() - 1), new ArrayList<>(deletedLabels));

            // Disconnect again
            forest.cutEdgeInComponent(u, v);
                
        }
        return result;
    }
}    