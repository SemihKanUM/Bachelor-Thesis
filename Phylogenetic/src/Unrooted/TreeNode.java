package Unrooted;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    String label; // unique label 
    boolean isLeaf; // true if leaf (species), false if internal node
    List<TreeNode> neighbors;

    TreeNode() {
        this.isLeaf = false;
        this.neighbors = new ArrayList<>();
    }

    TreeNode(String label) {
        this.label = label;
        this.isLeaf = true;
        this.neighbors = new ArrayList<>();
    }

    public void addNeighbours(TreeNode node){
        neighbors.add(node);
    }

    public void removeNeighbours(TreeNode node){
        neighbors.remove(node);
    }

    public void printNeighbours(){
        for (int i = 0; i < neighbors.size(); i++) {
            if(neighbors.get(i).isLeaf){
                System.out.print(neighbors.get(i).label + " ");
            }
            else{
                System.out.print("*" + " ");
            }
        }
    }
}