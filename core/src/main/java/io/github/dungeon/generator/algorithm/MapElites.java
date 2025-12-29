package io.github.dungeon.generator.algorithm;


import io.github.dungeon.generator.tree.DungeonTree;
import io.github.dungeon.generator.tree.NodeTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MapElites {
    final static int GENERATIONS_DEFAULT = 10_000_000;
    final static int BASE_POPULATION = 100;

    private static DungeonTree getRandomNode(DungeonTree root) {
        List<DungeonTree> nodes = new ArrayList<>();
        root.collectNodes(nodes);
        return nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
    }

// ------------------------- mutations -------------------------
    private static void changeRoomType(DungeonTree tree) {
        tree.setType(NodeTypes.getRandomRoom());
    }

    private static void swapRoomTypes(DungeonTree tree1, DungeonTree tree2) {
        NodeTypes.Base temp = tree1.getType();
        tree1.setType(tree2.getType());
        tree2.setType(temp);
    }

    private static void addChildNode(DungeonTree tree) {
        if (tree.getChildren().size() < 3) {
            tree.addChild(new DungeonTree(tree));
        }
    }

    private static void removeChildNode(DungeonTree tree) {
        HashSet<DungeonTree> children = tree.getChildren();

        if (children.isEmpty()) return; // shouldn't happen, but just in case

        DungeonTree removed = tree.removeRandomChild();

        if (removed != null && removed.getType() instanceof NodeTypes.Start) {
            tree.setType(new NodeTypes.Start());
        }
        if (removed != null && removed.getType() instanceof NodeTypes.Exit) {
            tree.setType(new NodeTypes.Exit());
        }
    }

    private static DungeonTree mutate(DungeonTree tree) {
        DungeonTree treeCopy = tree.deepCopy();
        DungeonTree randomChild = getRandomNode(treeCopy);
        if (randomChild == null) return treeCopy;
        float rand = (float)Math.random();
        if (randomChild.isLeaf()) {
            if (rand < 0.5f) {
                changeRoomType(randomChild);
            } else {
                addChildNode(randomChild);
            }
        } else if (randomChild.isStartOrExit()) {
             if (rand < 0.33f) {
                 DungeonTree randomChild2 = getRandomNode(treeCopy);
                 swapRoomTypes(randomChild, randomChild2);
            } else if (rand < 0.66f) {
                addChildNode(randomChild);
            } else {
                removeChildNode(randomChild);
            }
        } else {
            if (rand < 0.35f) {
                changeRoomType(randomChild);
            } else if (rand < 0.7f) {
                addChildNode(randomChild);
            } else {
                removeChildNode(randomChild);
            }
        }
        return treeCopy;
    }

// ------------------------- crossover -------------------------
    private static DungeonTree crossover(DungeonTree parent1, DungeonTree parent2) {
        DungeonTree child = parent1.deepCopy();

        List<DungeonTree> children1 = new ArrayList<>(child.getChildren());
        List<DungeonTree> children2 = new ArrayList<>(parent2.getChildren());

        if (children1.isEmpty() || children2.isEmpty()) return child;

        child.removeRandomChild();

        DungeonTree toAdd = parent2.getRandomChild();
        child.addChild(toAdd.deepCopy());

        return child;
    }

// ------------------------- algorithm -------------------------
    public static MapElitesArchive run() {
        return MapElites.run(GENERATIONS_DEFAULT);
    }

    public static MapElitesArchive run(int generations_num) {
        // -- initialize
        MapElitesArchive archive = new MapElitesArchive(
                Fitness::averageDifficulty,
                Fitness::averageReward,
                tree -> Fitness.fitness(tree, true, true),
                0.1f, 0.7f,
                0.2f, 0.8f
        );
        archive.populateArchive(BASE_POPULATION);
        // -- main loop
        for (int generation = 0; generation < generations_num; generation++) {
            float rand = (float)Math.random();
            if (rand < 0.4f) {
                // mutation
                DungeonTree parent = archive.getRandomTree();
                if (parent != null) {
                    DungeonTree child = mutate(parent);
                    archive.addToArchive(child);
                }
            } else {
                // crossover
                DungeonTree parent1 = archive.getRandomTree();
                DungeonTree parent2 = archive.getRandomTree();
                if (parent1 != null && parent2 != null) {
                    DungeonTree child = crossover(parent1, parent2);
                    archive.addToArchive(child);
                }
            }
        }
        return archive;
    }

    public static void main(String[] args) {
        int numGenerations = 1_000_000;
        MapElitesArchive res = MapElites.run(numGenerations);
        res.print();
        res.serializeArchive(numGenerations);
    }
}
