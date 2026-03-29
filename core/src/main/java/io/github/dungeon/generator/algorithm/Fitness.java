package io.github.dungeon.generator.algorithm;


import io.github.dungeon.common.Constants;
import io.github.dungeon.generator.grid.LayoutGenerator;
import io.github.dungeon.generator.tree.DungeonTree;
import io.github.dungeon.generator.tree.NodeTypes;

import java.util.*;
import java.util.function.ToDoubleFunction;

public final class Fitness {
    // ------------------ quality ------------------
    // more -> better (up to 75% of max nodes)
    private static float countNodes(DungeonTree tree) {
        int count = tree.countNodes();
        float value = (float) count / Constants.MAX_NODES;
        return 1.0f - Math.abs(value - 0.75f);
    }

    private static float nodesDiversity(DungeonTree tree) {
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
    private static float startToExitPathLen(DungeonTree tree) {
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

    public static float avgBranchingFactor(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);
        return average(
            nodes.stream().filter(n -> n.countChildren() > 0).toList(),
            DungeonTree::countChildren
        ) / 3.0f;
    }

    private static float averageRisk(DungeonTree tree) {
        return average(getAllNodes(tree), n -> n.getType().getRisk());
    }

    private static float averageReward(DungeonTree tree) {
        return average(getAllNodes(tree), n -> n.getType().getReward());
    }

    private static float averageRiskOnMainPath(DungeonTree tree) {
        return average(getMainPath(tree), n -> n.getType().getRisk());
    }

    private static float averageRewardOnMainPath(DungeonTree tree) {
        return average(getMainPath(tree), n -> n.getType().getReward());
    }

    public static float riskValue(DungeonTree tree) {
        return averageRisk(tree) * 0.75f + averageRiskOnMainPath(tree) * 0.25f;
    }

    static float rewardValue(DungeonTree tree) {
        return averageReward(tree) * 0.75f + averageRewardOnMainPath(tree) * 0.25f;
    }

    private static float balanceValue(DungeonTree tree) {
        float risk = riskValue(tree);
        float reward = rewardValue(tree);
        return 1 - Math.abs(risk - reward);
    }

    // ------------------ controls ------------------
    // should have start and exit
    static float hasStartAndExitOnce(DungeonTree tree) {
        if (tree.hasStartAndExitOnce()) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

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

    // ------------------ other ------------------
    private static float average(List<DungeonTree> nodes, ToDoubleFunction<DungeonTree> mapper) {
        if (nodes.isEmpty()) return 0;

        double total = 0;
        for (DungeonTree node : nodes) {
            total += mapper.applyAsDouble(node);
        }
        return (float) (total / nodes.size());
    }

    private static List<DungeonTree> getAllNodes(DungeonTree tree) {
        List<DungeonTree> nodes = new ArrayList<>();
        tree.collectNodes(nodes);
        return nodes;
    }

    private static List<DungeonTree> getMainPath(DungeonTree tree) {
        if (hasStartAndExitOnce(tree) == 0f) return Collections.emptyList();

        List<DungeonTree> path = new ArrayList<>();
        tree.collectNodesMainPath(path);
        return path;
    }

    // ------------- API -------------
    private static float quality(DungeonTree tree) {
        return (
                countNodes(tree)
                + startToExitPathLen(tree)
                + nodesDiversity(tree)
                + balanceValue(tree)
        ) / 4;
    }

    private static float control(DungeonTree tree) {
        return hasStartAndExitOnce(tree) * canGenerateLayout(tree);
    }

    public static float fitness(DungeonTree tree, boolean quality, boolean control) {
        float q = quality ? quality(tree): 1.0f;
        float c = control ? control(tree): 1.0f;
        return q * c;
    }
}
