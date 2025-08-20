package Unrooted;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    // public static void main(String[] args) throws Exception {

    //    TOY TREES FOR EXPERIMENT AND OBSERVATIONS

    //     TreeBuilder builder = new TreeBuilder();
    //     PhylogeneticTree tree1 = builder.buildTree1();
    //     PhylogeneticTree tree2 = builder.buildTree2();

    //   OR  HERE YOU CAN MANUALLY READ A TREE IN A NEWICK FORMAT WITH THE PARSE CLASS

    //     // Parser parser = new Parser();
    //     // List<PhylogeneticTree> trees = parser.parseFileWithTwoTrees("/Users/user/Desktop/Bachelor Thesis/DataCode_11_dec_2020/maindataset/TREEPAIR_50_10_70_02.tree");

    //     // PhylogeneticTree tree1 = trees.get(0);
    //     // PhylogeneticTree tree2 = trees.get(1);

    //     // Parser parser = new Parser();
    //     // PhylogeneticTree tree1 = parser.parse("((((6,5),(1,2)),((4,8),10)),((3,7),9))");
    //     // PhylogeneticTree tree2 = parser.parse("((6,(10,(1,((4,8),2)))),(5,((3,7),9)))");   
    
    //      YOU CAN TRY CHEN' ALGORITHM OR THE SPLIT OR DECOMPOSITION ALGORITHM

    //     ChenAlgorithm chenAlgorithm = new ChenAlgorithm(tree1, tree2);
    //     System.out.println(chenAlgorithm.solve(4));
    //     //System.out.println(chenAlgorithm.solve(3));

    //     // SplitOrDecompose splitOrDecompose = new SplitOrDecompose(tree1.copyTree(), tree2.copyTree());
    //     //System.out.println(splitOrDecompose.solve(2));
    // }

    public static void main(String[] args) throws Exception {
        String datasetDir = "DataCode_11_dec_2020/maindataset"; // <- Source of the tree datasets
        String csvPath = "DataCode_11_dec_2020/names.csv"; // <- Path for the names of the trees that you want to be read
    
        BufferedReader reader = new BufferedReader(new FileReader(csvPath));
        String line;
        boolean isFirstLine = true;
        
        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // skip header
            }
    
            String[] parts = line.split(",");
            String fileName = parts[0].trim() + ".tree";
            String fullPath = datasetDir + "/" + fileName;
            
            try {
                Parser parser = new Parser();
                List<PhylogeneticTree> trees = parser.parseFileWithTwoTrees(fullPath);
                PhylogeneticTree tree1 = trees.get(0).copyTree();
                PhylogeneticTree tree2 = trees.get(1).copyTree();

                int k = 1;
                while (true) {
                    SplitOrDecompose splitOrDecompose = new SplitOrDecompose(tree1.copyTree(), tree2.copyTree());
                    ChenAlgorithm chen = new ChenAlgorithm(tree1.copyTree(), tree2.copyTree());
                    boolean s = splitOrDecompose.solve(k);
                    boolean c = chen.solve(k);
                    if(splitOrDecompose.recursionCounter > 3000000){
                        break;
                    }
                    if (s && c) {
                        System.out.println(k + " " + fileName);
                        //Different print statements for differen experimental purposes
                        //System.out.println(k + " " + splitOrDecompose.recursionCounter + " " + chen.recursionCounter);
                        //System.out.println(k + " " + splitOrDecompose.recursionCounter +  " " + splitOrDecompose.splitCounter + "  " + splitOrDecompose.splitFunctioned + " " + splitOrDecompose.decompositionCounter + " " + splitOrDecompose.decompositionFunctioned + " " + chen.recursionCounter + " " +taxaCount);
                       // System.out.println(chen.recursionCounter + " " + chen.splitCounter + " " + k);
                        break;
                    }
                    else if((s && !c) || (!s && c)){
                    }
                    k++;
                    if (k > 30) {
                        System.out.println(fileName + " -> no solution up to k=30");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error processing file: " + fileName);
                e.printStackTrace();
            }
        }
        reader.close();
    }
}