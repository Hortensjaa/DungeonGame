package io.github.dungeon.generator.algorithm;


import io.github.dungeon.common.Constants;
import io.github.dungeon.generator.grid.LayoutGenerator;
import io.github.dungeon.generator.tree.DungeonTree;
import io.github.dungeon.generator.tree.NodeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Fitness {
    // ------------------ quality ------------------
    // more -> better (up to 75% of max nodes)
    static float countNodes(DungeonTree tree) {
        int count = tree.countNodes();  // todo: rethink
        float value = (float) count / Constants.MAX_NODES;
        return 1.0f - Math.abs(value - 0.75f);
    }

    // ideal is ~4 grandchildren per node, to avoid deep backtracking
    static float countGrandchildren(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);

        int penaltyCount = 0;
        for (DungeonTree node : nodes) {
            int grandchildCount = node.getGrandchildren().size();
            penaltyCount += Math.abs(grandchildCount - 4);
        }

        float penalty = (float) penaltyCount / (float) (tree.countNodes() * 7);
        return 1.0f - penalty;
    }

    static float nodesDiversity(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);

        Map<String, Integer> types = new HashMap<>();
        for (DungeonTree node : nodes) {
            types.put(node.getType().getName(), types.getOrDefault(node.getType().getName(), 0) + 1);
        }

        int sumOfSquares = types.values().stream()
                .mapToInt(count -> count * count)
                .sum();

        int totalNodes = nodes.size();
        float maxSum = (float) (totalNodes * totalNodes);
        float minSum = (float) totalNodes;

        return 1.0f - (sumOfSquares - minSum) / (maxSum - minSum);
    }

    // which part of the dungeon is on the main path from start to exit; should be ~50%
    static float startToExitPath(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);

        DungeonTree start = null;
        DungeonTree exit = null;

        for (DungeonTree node : nodes) {
            if (node.getType() instanceof NodeTypes.Start) {
                start = node;
            } else if (node.getType() instanceof NodeTypes.Exit) {
                exit = node;
            }
        }

        if (start == null || exit == null) {
            return 0;
        }

        int distance = tree.getTreeDistance(start, exit);
        float percent = distance / (float)(tree.countNodes());
        return 1.0f - Math.abs(percent - 0.5f);
    }

    // ------------------ controls ------------------
    static float countNodesControl(DungeonTree tree) {
        int count = tree.countNodes();
        return (float) count / Constants.MAX_NODES;
    }

    // should have start and exit
    static float hasStartAndExitOnce(DungeonTree tree) {
        if (tree.hasStartAndExitOnce()) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    // todo: maybe it should be more strict in later generations (eg. 80%+)
    static float canGenerateLayout(DungeonTree tree) {
        float successRate = 0f;
        for (int i = 0; i < 5; i++) {
            try {
                LayoutGenerator.generateLayout(tree, 1);
                successRate += 0.2f;
            } catch (IllegalArgumentException e) {
                // try again
            }
        }
        if (successRate > 0.5f) {
            return successRate;
        } else {
            return 0f;
        }
    }

    // if there are more than 7 children, collisions are unavoidable
    static float checkGrandchildren(DungeonTree tree) {
        return checkGrandchildrenRec(tree) ? 1f : 0f;
    }

    private static boolean checkGrandchildrenRec(DungeonTree tree) {
        if (tree.getGrandchildren().size() > 7) {
            return false;
        }

        for (DungeonTree child : tree.getChildren()) {
            if (!checkGrandchildrenRec(child)) {
                return false;
            }
        }

        return true;
    }

    // ------------------ other ------------------
    static float averageDifficulty(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);

        float totalDifficulty = 0;
        for (DungeonTree node : nodes) {
            totalDifficulty += node.getType().getDifficulty();
        }
        return totalDifficulty / nodes.size();
    }

    static float averageReward(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);

        float totalReward = 0;
        for (DungeonTree node : nodes) {
            totalReward += node.getType().getReward();
        }
        return totalReward / nodes.size();
    }

    // ------------- API -------------
    private static float min(float... values) {
        float min = Float.MAX_VALUE;
        for (float v : values) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    private static float quality(DungeonTree tree) {
        return (
                countNodes(tree)
                + startToExitPath(tree)
                + countGrandchildren(tree)
                + nodesDiversity(tree)
        ) / 4;
    }

    private static float control(DungeonTree tree) {
        return min(hasStartAndExitOnce(tree), checkGrandchildren(tree), countNodesControl(tree), canGenerateLayout(tree));
    }

    public static float fitness(DungeonTree tree, boolean quality, boolean control) {
        float q = quality ? quality(tree): 1.0f;
        float c = control ? control(tree): 1.0f;
        return q * c;
    }
}
