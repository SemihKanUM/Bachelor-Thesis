
# Split-or-Decompose MAF (Unrooted) — Java Implementation

**YOU CAN FIND THE THESIS REPORT ABOVE**

This project implements two fixed-parameter algorithms for computing Maximum Agreement Forests (MAF) on **unrooted** phylogenetic trees:

- **Chen’s algorithm** (baseline)
- **Split-or-Decompose** (SoD), including splitting overlapping components and a budgeted decomposition routine for disjoint components

The code loads Newick tree pairs from `.tree` files, performs reductions (suppressing degree-2 nodes, removing singletons, reducing common cherries), and explores branching strategies. It includes counters to track recursion and strategy usage for experiments.

---

## Project layout

```
src/
  Unrooted/
    Main.java                 # entry point (reads CSV of tree-pair filenames and runs algorithms)
    Parser.java               # Newick parser
    TreeNode.java             # node representation
    PhylogeneticTree.java     # tree data structure and utilities (embedding, restriction, copy, etc.)
    Forest.java               # forest container (components, cutOff, cutEdgeInComponent, split, etc.)
    TreeUtils.java            # reductions, cherries, overlaps, paths, agreement-forest checks
    ChenAlgorithm.java        # baseline branching algorithm
    SplitOrDecompose.java     # split-or-decompose algorithm
    TreeBuilder.java          # small handcrafted toy trees for sanity tests
```

---

## Requirements

- Java 17+ (Java 11 should also work)
- macOS/Linux/Windows

---

## Build & Run

There are two ways to run:
- **(A) Quick run with current hardcoded paths** in `Main.java` (default behavior as in the source).
- **(B) Recommended**: make `Main` read the dataset and CSV paths from command-line arguments for portability.

### A) Quick run (uses current hardcoded paths)

```bash
# from the project root
find src -name "*.java" > sources.txt
javac -d out @sources.txt

# run the program
java -cp out Unrooted.Main
```

### B) Portable run (pass arguments)

1) Edit `Main.java` to accept arguments (dataset directory and CSV path). For example:

```java
public static void main(String[] args) throws Exception {
    String datasetDir = args.length > 0 ? args[0] : "/path/to/maindataset";
    String csvPath    = args.length > 1 ? args[1] : "/path/to/names.csv";
    ...
}
```

2) Compile and run:

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt

# dataset and csv are positional args
java -cp out Unrooted.Main /absolute/path/to/maindataset /absolute/path/to/names.csv
```

> The CSV is expected to have a **header** and a first column with the base name of each `.tree` file (without extension). Each `.tree` file must contain **exactly 2** Newick strings, **one per line**.


## Quick sanity tests (no dataset required)

You can run on toy trees using the `TreeBuilder` or inline Newick strings via `Parser`:

```java
// Inside Main.main for a quick experiment:
TreeBuilder builder = new TreeBuilder();
PhylogeneticTree t1 = builder.buildTree1();
PhylogeneticTree t2 = builder.buildTree2();

SplitOrDecompose sod = new SplitOrDecompose(t1.copyTree(), t2.copyTree());
ChenAlgorithm chen = new ChenAlgorithm(t1.copyTree(), t2.copyTree());

System.out.println("SoD(k=4)   = " + sod.solve(4));
System.out.println("Chen(k=4)  = " + chen.solve(4));
System.out.println("Recursions: SoD=" + sod.recursionCounter + ", Chen=" + chen.recursionCounter);
```

Or with Newick:

```java
Parser p = new Parser();
PhylogeneticTree t1 = p.parse("((((6,5),(1,2)),((4,8),10)),((3,7),9))");
PhylogeneticTree t2 = p.parse("((6,(10,(1,((4,8),2)))),(5,((3,7),9)))");
```

---

## Dataset format & experiment loop

- CSV (`names.csv`) header + first column contains base names without extension; e.g. `TREEPAIR_50_10_70_02`.
- Each `*.tree` file contains **two lines**, each a Newick tree.

The default `Main` looks for the smallest `k` (up to 30) where both algorithms return `true`. It also maintains counters for recursion depth and strategy triggers, which you can print for analysis.

---

## Testing checklist

- **Parsing**: Try a few `.tree` files manually; verify that `Parser.parseFileWithTwoTrees` returns 2 trees.
- **Reductions**: Log when degree-2 suppression, singleton removal, and cherry reduction trigger.
- **Agreement checks**: Validate `TreeUtils.isAgreementForest` on small hand-crafted cases.
- **Split**: Construct an overlap where two components induce the same edge in the embedding of `T` and verify the branching.
- **Decompose**: Feed a forest with disjoint components and confirm the budgeted per-component solving and recursion on the remainder.

---

## Notes & Tips

- Prefer **absolute paths** for datasets/CSV in experiments.
- For large batches, redirect stdout to a file to collect results:
  ```bash
  java -jar maf-sod.jar /data/maindataset /data/names.csv > results.txt
  ```
- If you observe timeouts, consider lowering `k` max, or add a recursion limit and abort early for that pair.

---

## License

Bachelor Thesis Assignment By Semih Kan 
Supervisor : David Mestel
Maastricht University 2025
Publicly Open
