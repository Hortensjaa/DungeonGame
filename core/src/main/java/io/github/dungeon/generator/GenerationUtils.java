package io.github.dungeon.generator;


import io.github.dungeon.generator.algorithm.MapElites;
import io.github.dungeon.generator.algorithm.MapElitesArchive;
import io.github.dungeon.generator.grid.GridDefinition;
import io.github.dungeon.generator.grid.GridGenerator;
import io.github.dungeon.generator.tree.DungeonTree;

public final class GenerationUtils {
    static final int MAX_RETRIES = 20;

    public static GridDefinition generateFromFile(String dirName, int x, int y) {
        return GridGenerator.generate(dirName, x, y, MAX_RETRIES);
    }

    public static GridDefinition runAndGenerate(int numGenerations) {
         MapElitesArchive res = MapElites.run(numGenerations);
         DungeonTree tree = res.getRandomGoodTree();
         return GridGenerator.generate(tree, MAX_RETRIES);
    }

    public static GridDefinition runAndGenerate(int numGenerations, int x, int y) {
         MapElitesArchive res = MapElites.run(numGenerations);
         DungeonTree tree = res.getTreeAt(x, y);
         return GridGenerator.generate(tree, MAX_RETRIES);
    }

    public static GridDefinition runSaveAndGenerate(int numGenerations) {
         MapElitesArchive res = MapElites.run(numGenerations);
         res.serializeArchive(numGenerations);
         DungeonTree tree = res.getRandomGoodTree();
         GridDefinition gridDefinition = GridGenerator.generate(tree, MAX_RETRIES);
         return gridDefinition;
    }

    public static GridDefinition runSaveAndGenerate(int numGenerations, int x, int y) {
        MapElitesArchive res = MapElites.run(numGenerations);
        res.serializeArchive(numGenerations);
        DungeonTree tree = res.getTreeAt(x, y);
        GridDefinition gridDefinition = GridGenerator.generate(tree, MAX_RETRIES);
        return gridDefinition;
    }

    public static GridDefinition generateFromTree(DungeonTree tree) {
        return GridGenerator.generate(tree, MAX_RETRIES);
    }

    public static GridDefinition generateRandomTree(int depth, float branchFact, float branchFactMult) {
        DungeonTree dungeonTree = new DungeonTree();
        dungeonTree.generateRandomTree(depth, branchFact, branchFactMult);
        return GridGenerator.generate(dungeonTree, 10);
    }

    public static GridDefinition debugSerialization(int numGenerations, int x, int y) {
        MapElitesArchive res = MapElites.run(numGenerations);
        String dirName = res.serializeArchive(numGenerations);
        // get tree directly from archive
        DungeonTree tree = res.getTreeAt(x, y);
        GridDefinition gridDefinition = GridGenerator.generate(tree, MAX_RETRIES);
        // get tree from saved files
//        GridDefinition gridDefinition2 = GeneratorFromLayout.generate(dirName, x, y);
        return gridDefinition;
    }
}
