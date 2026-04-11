package io.github.dungeon.generator.tree;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.dungeon.generator.layout.LayoutField;
import io.github.dungeon.generator.layout.LayoutGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.*;

@AllArgsConstructor
@Getter
@NoArgsConstructor
class EdgeDTO{
    int from;
    int to;
}


@AllArgsConstructor
@Getter
@NoArgsConstructor
class NodeDTO{
        int id;
        String type;
        float difficulty;
        float reward;
}


@AllArgsConstructor
@Getter
@NoArgsConstructor
class DungeonTreeDTO {
    float fitness;
    List<NodeDTO> nodes;
    List<EdgeDTO> edges;
}


public class DungeonTreeSerializer {

    private static DungeonTree findStartNode(DungeonTree root) {
        Queue<DungeonTree> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            DungeonTree cur = queue.poll();
            if (cur.getType() instanceof NodeTypes.Start) return cur;
            queue.addAll(cur.getChildren());
        }
        return root; // fallback
    }

    private static DungeonTree rerootAtStart(DungeonTree originalRoot) {
        DungeonTree copy = originalRoot.deepCopy();
        DungeonTree start = findStartNode(copy);

        // Collect path from start to root
        List<DungeonTree> path = new ArrayList<>();
        DungeonTree cur = start;
        while (cur != null) {
            path.add(cur);
            cur = cur.getParent();
        }

        // Flip edges along the path
        for (int i = 0; i < path.size() - 1; i++) {
            DungeonTree child = path.get(i);
            DungeonTree parent = path.get(i + 1);
            parent.removeChild(getChildIndex(parent, child));
            child.addChild(parent);
        }

        return start;
    }

    private static int getChildIndex(DungeonTree parent, DungeonTree child) {
        if (parent.getFirstChild() == child) return 0;
        if (parent.getSecondChild() == child) return 1;
        if (parent.getThirdChild() == child) return 2;
        throw new IllegalArgumentException("Node is not a child of the given parent");
    }

    private static DungeonTreeDTO serialize(DungeonTree root, float fitness) {
        Map<DungeonTree, Integer> ids = new IdentityHashMap<>();
        List<NodeDTO> nodes = new ArrayList<>();
        List<EdgeDTO> edges = new ArrayList<>();

        assignIds(root, ids);

        for (Map.Entry<DungeonTree, Integer> entry : ids.entrySet()) {
            DungeonTree node = entry.getKey();
            int id = entry.getValue();

            nodes.add(new NodeDTO(
                    id,
                    node.getType().getName(),
                    node.getType().getRisk(),
                    node.getType().getReward()
            ));

            if (node.getFirstChild() != null) addEdge(edges, ids, node, node.getFirstChild());
            if (node.getSecondChild() != null) addEdge(edges, ids, node, node.getSecondChild());
            if (node.getThirdChild() != null) addEdge(edges, ids, node, node.getThirdChild());
        }

        return new DungeonTreeDTO(fitness, nodes, edges);
    }

    private static void assignIds(DungeonTree root, Map<DungeonTree, Integer> ids) {
        Queue<DungeonTree> queue = new ArrayDeque<>();
        queue.add(root);
        ids.put(root, 0);

        int nextId = 1;

        while (!queue.isEmpty()) {
            DungeonTree cur = queue.poll();

            for (DungeonTree child : Arrays.asList(
                    cur.getFirstChild(),
                    cur.getSecondChild(),
                    cur.getThirdChild()
            )) {
                if (child != null && !ids.containsKey(child)) {
                    ids.put(child, nextId++);
                    queue.add(child);
                }
            }
        }
    }

    private static void addEdge(
            List<EdgeDTO> edges,
            Map<DungeonTree, Integer> ids,
            DungeonTree from,
            DungeonTree to
    ) {
        if (to != null) {
            edges.add(new EdgeDTO(
                    ids.get(from),
                    ids.get(to)
            ));
        }
    }

    public static DungeonTree deserialize(DungeonTreeDTO dto) {

        Map<Integer, DungeonTree> nodes = new HashMap<>();

        for (NodeDTO n : dto.getNodes()) {
            DungeonTree treeNode = new DungeonTree();
            treeNode.setType(NodeTypes.fromString(n.getType(), n.getDifficulty(), n.getReward()));
            nodes.put(n.getId(), treeNode);
        }

        Set<Integer> childrenIds = new HashSet<>();

        for (EdgeDTO e : dto.getEdges()) {
            DungeonTree from = nodes.get(e.getFrom());
            DungeonTree to = nodes.get(e.getTo());
            childrenIds.add(e.getTo());
            from.addChild(to);
        }

        DungeonTree root = null;

        for (Integer id : nodes.keySet()) {
            if (!childrenIds.contains(id)) {
                root = nodes.get(id);
                break;
            }
        }

        if (root == null) {
            throw new IllegalStateException("No root node found in DungeonTreeDTO");
        }

        return root;
    }

    // ------------------ API ------------------
    public static void writeToFile(DungeonTree tree, File file) throws IOException {
        writeToFile(tree, -1.0f, file);
    }

    public static void writeToFile(DungeonTree tree, float fitness, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
        DungeonTree rerooted = rerootAtStart(tree);
        mapper.writeValue(file, serialize(rerooted, fitness));
    }

    public static DungeonTree readFromFile(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DungeonTreeDTO dto = mapper.readValue(file, DungeonTreeDTO.class);

        return deserialize(dto);
    }

//    ------------------- test ------------------
public static void main(String[] args) throws Exception {
    DungeonTree original = new DungeonTree();
    original.generateRandomTree(4, 0.6f, 0.9f);

    File file = new File("levels/dungeon_tree.json");

    DungeonTreeSerializer.writeToFile(original, file);

    DungeonTree loaded = readFromFile(file);

    System.out.println("Original nodes: " + original.countNodes());
    System.out.println("Loaded nodes:   " + loaded.countNodes());

    System.out.println("Original BFS:");
    original.printBFS();
    System.out.println("Loaded BFS:");
    loaded.printBFS();

    LayoutField[][] l1 = LayoutGenerator.generateLayout(original, 3);
    LayoutField[][] l2 = LayoutGenerator.generateLayout(loaded, 3);

    System.out.println();
    LayoutGenerator.printLayout(l1);
    System.out.println();
    LayoutGenerator.printLayout(l2);
}

}

