package Unrooted;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SplitOrDecompose {

    private PhylogeneticTree Tfirst; // The main tree
    private Forest F;           // The forest (current decomposition)
    private int solutionCount = 0;
    private static final int MAX_SOLUTIONS = 1;

    public int splitFunctioned = 0;
    public int splitCounter = 0;
    public int recursionCounter = 0;
    public int decompositionCounter = 0;
    public int decompositionFunctioned = 0;

    public SplitOrDecompose(PhylogeneticTree Tfirst, PhylogeneticTree Tprime) {
        this.Tfirst = Tfirst;
        this.F = new Forest();
        F.addComponent(Tprime);
    }

    public boolean solve(int k){
        return solve(Tfirst.copyTree(), F.copyForest(), k, true);
    }

    public boolean solve(PhylogeneticTree T_local, Forest forest, int k , boolean allowDecompose) {
        recursionCounter++;
    
        // === REDUCTION PHASE ===
        while (true) {
            boolean changed = false;
    
            // Suppress degree-2 nodes in T_local and all forest components
            if (T_local.suppressDegree2Nodes()) changed = true;
            for (PhylogeneticTree tree : forest.components) {
                if (tree.suppressDegree2Nodes()) changed = true;
            }
    
            // Remove singleton components from the forest and corresponding leaves from T
            List<String> deleted = TreeUtils.removeSingletons(forest, T_local);
            if (!deleted.isEmpty()) changed = true;
    
            // Reduce common cherries between T and F'
            if (TreeUtils.findAndReduceCommonCherries(forest, T_local)) changed = true;
    
            if (!changed) break; // Exit when no more reductions apply
        }
    
        // === BASE CASE ===
        if (k < 0) return false; // too many cuts used
        

        // === SPLIT PHASE ===
        List<PhylogeneticTree> components = forest.components;
        for (int i = 0; i < components.size(); i++) {
            for (int j = i + 1; j < components.size(); j++) {
                PhylogeneticTree comp1 = components.get(i);
                PhylogeneticTree comp2 = components.get(j);
    
                List<String> leaves1 = comp1.getLeafLabels();
                List<String> leaves2 = comp2.getLeafLabels();
    
                List<TreeNode> overlappingEdge = getFirstOverlappingEdge(T_local, leaves1, leaves2);

                if (overlappingEdge != null && k != 0) {

                    // Split phase: create two branching scenarios
                    TreeNode u = overlappingEdge.get(0);
                    TreeNode v = overlappingEdge.get(1);
    
                    Set<String> Y = getLeavesFrom(u, v);
                    Set<String> Z = getLeavesFrom(v, u);
    
                    if (Y.isEmpty() || Z.isEmpty() || !Collections.disjoint(Y, Z)) {
                        System.err.println("Invalid bipartition in split: skipping.");
                        continue;
                    }
                    
                    splitCounter++;

                    // Branch A: Cut comp1
                    Forest f1 = new Forest();
                    Map<PhylogeneticTree, PhylogeneticTree> mapA = forest.copyForestWithMapping(f1);
                    PhylogeneticTree compF1 = mapA.get(comp1);
                    f1.removeComponent(compF1);
                    boolean branchA = recursivelySplitting(compF1, f1, Y, Z, k, T_local);

                    
    
                    // Branch B: Cut comp2
                    Forest f2 = new Forest();
                    Map<PhylogeneticTree, PhylogeneticTree> mapB = forest.copyForestWithMapping(f2);
                    PhylogeneticTree compF2 = mapB.get(comp2);
                    f2.removeComponent(compF2);
                    boolean branchB = recursivelySplitting(compF2, f2, Z, Y, k, T_local);

                    if(branchA || branchB){
                        splitFunctioned++;
                    }
    
                    return branchA || branchB;
                }
            }
        }
    
        // === DECOMPOSITION PHASE ===
        if (allowDecompose && hasOverlap(T_local.copyTree(), forest.copyForest())) {
            decompositionCounter++;
            if (tryDecompositionOnDisjointForest(T_local.copyTree(), forest.copyForest(), k)) {
                decompositionFunctioned++;
                return true;
            }
        }
    
        // === CHERRY BRANCHING PHASE (Chen style) ===
        List<List<TreeNode>> cherries = TreeUtils.findCherries(T_local);
        if (!cherries.isEmpty()) {
            TreeNode a = cherries.get(0).get(0);
            TreeNode b = cherries.get(0).get(1);
    
            if (inDifferentComponents(forest, a.label, b.label)) {
                // Cut either a or b
                Forest f1 = forest.copyForest();
                f1.cutOff(a.label);
                Forest f2 = forest.copyForest();
                f2.cutOff(b.label);
    
                return solve(T_local.copyTree(), f1, k - 1,true)
                    || solve(T_local.copyTree(), f2, k - 1,true);
            } else {
                return applyChenBranching(T_local.copyTree(), forest.copyForest(), a.label, b.label, k);
            }
        }
    
        // === FINAL CHECK ===
        if (TreeUtils.isAgreementForest(T_local, forest)) {
            solutionCount++;
            return true;
        }
        return false;
    }

    private boolean hasOverlap(PhylogeneticTree T_local, Forest forest){

        List<PhylogeneticTree> components = forest.components;
        for (int i = 0; i < components.size(); i++) {
            for (int j = i + 1; j < components.size(); j++) {
                PhylogeneticTree comp1 = components.get(i);
                PhylogeneticTree comp2 = components.get(j);
    
                List<String> leaves1 = comp1.getLeafLabels();
                List<String> leaves2 = comp2.getLeafLabels();
    
                List<TreeNode> overlappingEdge = getFirstOverlappingEdge(T_local, leaves1, leaves2);

                if (overlappingEdge != null) {
                    return true;
                }
            }
        }

        return false;
    }
    
    private boolean tryDecompositionOnDisjointForest(PhylogeneticTree T_local, Forest forest, int k) {
        // Base cases
        if (k < 0) return false;
        if (forest.components.isEmpty()) return true;
    
        int m = forest.components.size();
        int a = k / m; // first-pass budget per component
        
        if(a > 3){
            a = 3;
        }
        int spent = 0;
        Forest unsolved = new Forest(); // components that said "NO" with budget a
    
        for (PhylogeneticTree comp : forest.components) {
            // Build the embedding for just this component
            List<String> leaves = comp.getLeafLabels();
            PhylogeneticTree embedding = T_local.getEmbedding(leaves).getRestriction().copyTree();
    
            // Single-component forest to query the solver
            Forest single = new Forest();
            single.addComponent(comp);
    
            boolean ok = false;
            int used = -1;
    
            // IMPORTANT: try up to and including 'a'
            for (int j = 0; j <= a; j++) {
                
                if (solve(embedding.copyTree(),comp.copyTree() ,j)) {
                    ok = true;
                    used = j;   // minimal j we found
                    break;
                }
            }
    
            if (ok) {
                spent += used; // pay only what we actually used
            } else {
                unsolved.addComponent(comp); // save for the recursive call
            }
        }
    
        // If nobody fit into 'a' cuts, we must return NO
        if (unsolved.components.size() == m) return false;
    
        // If everyone fit, we're done
        if (unsolved.components.isEmpty()) return true;
    
        // Recurse on the NO components with the remaining budget
        return tryDecompositionOnDisjointForest(T_local, unsolved, k - spent);
        
    }
    
    public boolean recursivelySplitting(PhylogeneticTree component, Forest fullForest, Set<String> Y, Set<String> Z, int k , PhylogeneticTree T_local) {
       
        if (k == 0) {
            return false;
        }

        component.suppressDegree2Nodes();

        Forest newForest1 = fullForest.copyForest();
        PhylogeneticTree copyComp1 = new PhylogeneticTree();
        copyComp1 = component.copyTree();
        newForest1.addComponent(copyComp1);
            
        if(TreeUtils.isAgreementForest(T_local.copyTree(), newForest1)){
            return true;
        }

        if(component.isCherry()){
            if(component.nodes.size() == 2){ 
                Forest newForest = fullForest.copyForest();
                PhylogeneticTree copyComp = new PhylogeneticTree();
                Map<TreeNode, TreeNode> nodeMap = component.copyTreeWithMapping(copyComp);
                TreeNode u = nodeMap.get(component.nodes.get(0));
                TreeNode v = nodeMap.get(component.nodes.get(1));
                List<PhylogeneticTree> split = copyComp.splitIntoTwo(u, v);
    
                if (split.size() != 2) {
                    System.err.println("Missing cut operation here !!!");
                    System.out.println(split.size());
                }

                newForest.addComponent(split.get(0));
                newForest.addComponent(split.get(1));

                return solve(T_local.copyTree(), newForest, k - 1,false);

            }
            if(component.nodes.size() == 3){
                Forest newForest = fullForest.copyForest();
                PhylogeneticTree copyComp = new PhylogeneticTree();
                Map<TreeNode, TreeNode> nodeMap = component.copyTreeWithMapping(copyComp);
                TreeNode u = nodeMap.get(component.nodes.get(0));
                TreeNode v = nodeMap.get(component.nodes.get(1));
                List<PhylogeneticTree> split = copyComp.splitIntoTwo(u, v);
    
                if (split.size() != 2) {
                    System.err.println("Missing cut operation here !!!");
                    System.out.println(split.size());
                }

                newForest.addComponent(split.get(0));
                newForest.addComponent(split.get(1));

                return solve(T_local.copyTree(), newForest, k - 1,false);
            }
        }
    
        for (TreeNode center : component.nodes) {
            if (center.isLeaf) continue;

            Map<TreeNode, Set<String>> neighborToLabels = new HashMap<>();
            for (TreeNode neighbor : center.neighbors) {
                Set<String> reachable = new HashSet<>();
                Set<TreeNode> visited = new HashSet<>();
                dfsExclude(neighbor, center, visited, reachable);
                neighborToLabels.put(neighbor, reachable);
            }
    
            List<TreeNode> yOnly = new ArrayList<>();
            List<TreeNode> zOnly = new ArrayList<>();
            List<TreeNode> mixed = new ArrayList<>();
    
            for (Map.Entry<TreeNode, Set<String>> entry : neighborToLabels.entrySet()) {
                Set<String> labels = entry.getValue();
                boolean allY = labels.stream().allMatch(Y::contains);
                boolean allZ = labels.stream().allMatch(Z::contains);
    
                if (allY) yOnly.add(entry.getKey());
                else if (allZ) zOnly.add(entry.getKey());
                else mixed.add(entry.getKey());
            }
    
            // Case 1: Y-Z-Mixed structure  branching
            if (!yOnly.isEmpty() && !zOnly.isEmpty() && !mixed.isEmpty()) {

                for (TreeNode toCut : yOnly) {
                    Forest newForest = fullForest.copyForest();
                    PhylogeneticTree copyComp = new PhylogeneticTree();
                    Map<TreeNode, TreeNode> nodeMap = component.copyTreeWithMapping(copyComp);
                    TreeNode u = nodeMap.get(toCut);
                    TreeNode v = nodeMap.get(center);

                    if (!toCut.neighbors.contains(center)) continue;
                    List<PhylogeneticTree> split = copyComp.splitIntoTwo(u, v);
                
                    if (split.size() != 2) {
                        System.err.println("Missing cut operation here !!!");
                        System.out.println(split.size());
                    }

                    newForest.addComponent(split.get(0));
                    
                    if (recursivelySplitting(split.get(1), newForest, Y, Z, k - 1, T_local.copyTree())) {
                        return true;
                    }
                                        
                }
                
                for (TreeNode toCut : zOnly) {
                    Forest newForest = fullForest.copyForest();
                    PhylogeneticTree copyComp = new PhylogeneticTree();
                    Map<TreeNode, TreeNode> nodeMap = component.copyTreeWithMapping(copyComp);
                    TreeNode u = nodeMap.get(toCut);
                    TreeNode v = nodeMap.get(center);
                    
                    if (!toCut.neighbors.contains(center)) continue;
                    List<PhylogeneticTree> split = copyComp.splitIntoTwo(u, v);
                    
                    if (split.size() != 2) {
                        System.err.println("Missing cut operation here !!!");
                        System.out.println(split.size());
                    }
                    
                    newForest.addComponent(split.get(0));

                    if (recursivelySplitting(split.get(1), newForest, Y, Z, k - 1, T_local.copyTree())) {
                        return true;
                    }
                }

                break;
            }
    
            //  Case 2: Y-only and Z-only with no mixed — clean split
            else if (!yOnly.isEmpty() && !zOnly.isEmpty() && mixed.isEmpty()) {
                // Arbitrarily split on one yOnly neighbor
                TreeNode toCut;
    
                if (yOnly.size() == 1 && zOnly.size() == 2) {
                    toCut = yOnly.get(0);  // Y is minority
                } else if (zOnly.size() == 1 && yOnly.size() == 2) {
                    toCut = zOnly.get(0);  // Z is minority
                } 
                else {
                    // Should not happen in strict binary trees, but fallback
                    toCut = null;
                    System.out.println(yOnly.size() + " " + zOnly.size());
                    System.err.println("somethings are wrong!");
                }

                Forest newForest = fullForest.copyForest();
                PhylogeneticTree copyComp = new PhylogeneticTree();
                Map<TreeNode, TreeNode> nodeMap = component.copyTreeWithMapping(copyComp);
                TreeNode u = nodeMap.get(toCut);
                TreeNode v = nodeMap.get(center);

                if (!toCut.neighbors.contains(center)) continue;
                List<PhylogeneticTree> split = copyComp.splitIntoTwo(u, v);
    
                if (split.size() != 2) {
                    System.err.println("Missing cut operation here !!!");
                    System.out.println(split.size());
                }
                newForest.addComponent(split.get(0));
                newForest.addComponent(split.get(1));

                if (solve(T_local.copyTree(), newForest, k - 1,false)) {
                    return true;
                } 
            }
        }
        return false;
    }
    
    public static List<List<Integer>> generateDistributions(int k, int n) {
        List<List<Integer>> result = new ArrayList<>(); 
        generateDistributionsHelper(k, n, new ArrayList<>(), result);
        return result;
    }
    
    private static void generateDistributionsHelper(int remaining, int n, List<Integer> current, List<List<Integer>> result) {
        if (n == 1) {
            current.add(remaining);
            result.add(new ArrayList<>(current));
            current.remove(current.size() - 1);
            return;
        }
    
        for (int i = 0; i <= remaining; i++) {
            current.add(i);
            generateDistributionsHelper(remaining - i, n - 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    public static List<TreeNode> getFirstOverlappingEdge(PhylogeneticTree T, List<String> leaves1, List<String> leaves2) {
        List<List<TreeNode>> edges1 = new ArrayList<>(TreeUtils.getOriginalEdgesInEmbedding(T, leaves1));
        List<List<TreeNode>> edges2 = new ArrayList<>(TreeUtils.getOriginalEdgesInEmbedding(T, leaves2));
    
        for (List<TreeNode> e1 : edges1) {
            TreeNode e1a = e1.get(0);
            TreeNode e1b = e1.get(1);
    
            for (List<TreeNode> e2 : edges2) {
                TreeNode e2a = e2.get(0);
                TreeNode e2b = e2.get(1);
    
                boolean match =
                    (e1a == e2a && e1b == e2b) ||
                    (e1a == e2b && e1b == e2a);
    
                if (match) {
                    return e1;
                }
            }
        }
    
        return null;
    }

    //     This method will give us:
    // Y = all leaf labels reachable from u without going through v
    // Z = all leaf labels reachable from v without going through u
    // That’s our bipartition.
    public Set<String> getLeavesFrom(TreeNode start, TreeNode block) {
        Set<String> result = new HashSet<>();
        Set<TreeNode> visited = new HashSet<>();
        dfsExclude(start, block, visited, result);
        return result;
    }
    
    private void dfsExclude(TreeNode current, TreeNode block, Set<TreeNode> visited, Set<String> leaves) {
        visited.add(current);
        if (current.isLeaf && current.label != null) {
            leaves.add(current.label);
        }
        for (TreeNode neighbor : current.neighbors) {
            if (!visited.contains(neighbor) && neighbor != block) {
                dfsExclude(neighbor, block, visited, leaves);
            }
        }
    }

    // Chen branching: branch on cutting off a, cutting off b, or splitting the path between a and b.
    private boolean applyChenBranching(PhylogeneticTree T_local, Forest forest, String a, String b, int k) {

        //Branch 1: Cut off a.
        Forest copy1 = forest.copyForest(); 
        copy1.cutOff(a);
        boolean branch1 = solve(T_local.copyTree(), copy1, k - 1,true);

        // Branch 2: Cut off b.
        Forest copy2 = forest.copyForest();
        copy2.cutOff(b);
        boolean branch2 = solve(T_local.copyTree(), copy2, k - 1,true);

        //Branch 3: Split the path between a and b.
        Forest copy3 = forest.copyForest();
        boolean branch3 = splitPath(T_local.copyTree(), copy3, a, b, k);
        
        //return branch2 || branch1;
        return branch1 || branch2 || branch3 ;
        //return branch3;
    }

    // Check whether nodes with labels a and b are in different forest components.
    private boolean inDifferentComponents(Forest forest, String a, String b) {
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
            // System.out.println(a + " " + b);
            System.out.println(a + " " + b);
            throw new RuntimeException("One of the nodes was not found in any component.");
        }
        return (compA != compB);
    }

    // Split the path between nodes with labels aLabel and bLabel.
    // This branch tries all choices of keeping one side edge and cutting the others.
    // Split the path between nodes with labels aLabel and bLabel.
    // This branch tries all choices of keeping one side edge and cutting the others.
    private boolean splitPath(PhylogeneticTree T_local, Forest forest, String aLabel, String bLabel, int k )  {
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

            result |= solve(T_local.copyTree(), forestCopy, k - (sideEdges.size() - 1),true);

            // Disconnect again
            forest.cutEdgeInComponent(u, v);
                
        }
        
        return result;
    }

    public boolean solve(PhylogeneticTree T_local, PhylogeneticTree comp , int k){
        ChenAlgorithm subAlg = new ChenAlgorithm(T_local.copyTree(), comp.copyTree());
        return subAlg.solve(k);
    }
}