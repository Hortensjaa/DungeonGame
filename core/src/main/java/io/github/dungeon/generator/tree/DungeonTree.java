package io.github.dungeon.generator.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.*;


@Getter
public class DungeonTree {

    // data
    @Setter
    private NodeTypes.Base type;
    //children
    private DungeonTree firstChild = null;
    private DungeonTree secondChild = null;
    private DungeonTree thirdChild = null;
    // parent
    private DungeonTree parent = null;

    // --------------- constructors ---------------
    public DungeonTree(NodeTypes.Base type) {
        this.type = type;
    }

    public DungeonTree(DungeonTree parent) {
        this.type = NodeTypes.getRandomRoom();
        this.parent = parent;
    }

    public DungeonTree() {
        this.type = NodeTypes.getRandomRoom();
    }

    // --------------- generation ---------------
    public void generateRandomTree(
            int maxDepth,
            float branchingFactor,
            float branchingFactorMultiplier
    ) {
        generateSubtree(0, maxDepth, branchingFactor, branchingFactorMultiplier);

        // Find the two most distant leaves for Start and Exit
        DungeonTree[] mostDistantLeaves = findMostDistantLeaves();
        if (mostDistantLeaves[0] != null) {
            mostDistantLeaves[0].type = new NodeTypes.Start();
        }
        if (mostDistantLeaves[1] != null) {
            mostDistantLeaves[1].type = new NodeTypes.Exit();
        }
    }


    private void generateSubtree(
            int currentDepth,
            int maxDepth,
            float branchingFactor,
            float branchingFactorMultiplier
    ) {
        type = NodeTypes.getRandomRoom();

        if (currentDepth >= maxDepth) {
            return;
        }

        double randomValue = Math.random();
        firstChild = (randomValue < branchingFactor ? new DungeonTree(this) : null);
        secondChild = (randomValue < branchingFactor * 0.75 ? new DungeonTree(this) : null);
        thirdChild = (randomValue < branchingFactor * 0.5 ? new DungeonTree(this) : null);
        float newBranchingFactor = branchingFactor * branchingFactorMultiplier;
        if (firstChild != null) {
            firstChild.generateSubtree(currentDepth + 1, maxDepth, newBranchingFactor, branchingFactorMultiplier);
        }
        if (secondChild != null) {
            secondChild.generateSubtree(currentDepth + 1, maxDepth, newBranchingFactor, branchingFactorMultiplier);
        }
        if (thirdChild != null) {
            thirdChild.generateSubtree(currentDepth + 1, maxDepth, newBranchingFactor, branchingFactorMultiplier);
        }
    }

    private DungeonTree[] findMostDistantLeaves() {
        List<DungeonTree> leaves = new ArrayList<>();
        collectLeaves(leaves);

        if (leaves.size() < 2) {
            // If only one leaf or none, return it as both start as root
            DungeonTree leaf = leaves.isEmpty() ? this : leaves.get(0);
            return new DungeonTree[]{this, leaf};
        }

        DungeonTree leaf1 = null;
        DungeonTree leaf2 = null;
        int maxDistance = -1;

        for (int i = 0; i < leaves.size(); i++) {
            for (int j = i + 1; j < leaves.size(); j++) {
                int distance = getTreeDistance(leaves.get(i), leaves.get(j));
                if (distance > maxDistance) {
                    maxDistance = distance;
                    leaf1 = leaves.get(i);
                    leaf2 = leaves.get(j);
                }
            }
        }

        return new DungeonTree[]{leaf1, leaf2};
    }

    public void collectLeaves(List<DungeonTree> leaves) {
        if (isLeaf()) {
            leaves.add(this);
            return;
        }
        if (firstChild != null) firstChild.collectLeaves(leaves);
        if (secondChild != null) secondChild.collectLeaves(leaves);
        if (thirdChild != null) thirdChild.collectLeaves(leaves);
    }

    public int getTreeDistance(DungeonTree a, DungeonTree b) {
        if (a == b) return 0;

        Map<DungeonTree, Integer> distFromA = new HashMap<>();
        int dist = 0;
        DungeonTree cur = a;

        while (cur != null) {
            distFromA.put(cur, dist++);
            cur = cur.getParent();
        }

        cur = b;
        dist = 0;

        while (cur != null) {
            if (distFromA.containsKey(cur)) {
                return dist + distFromA.get(cur);
            }
            dist++;
            cur = cur.getParent();
        }

        return Integer.MAX_VALUE;
    }

    private boolean findPath(DungeonTree current, DungeonTree target, List<DungeonTree> path) {
        if (current == null) return false;

        path.add(current);

        if (current == target) return true;

        if (findPath(current.firstChild, target, path)) return true;
        if (findPath(current.secondChild, target, path)) return true;
        if (findPath(current.thirdChild, target, path)) return true;

        path.remove(path.size() - 1);
        return false;
    }

    // --------------- API ---------------
    public boolean isLeaf() {
        // checking firstChild should be enough, but better safe than sorry
        return firstChild == null && secondChild == null && thirdChild == null;
    }

    public void addChild(DungeonTree child) {
        if (firstChild == null) {
            setFirstChild(child);
        } else if (secondChild == null) {
            setSecondChild(child);
        } else if (thirdChild == null) {
            setThirdChild(child);
        } else {
            System.out.println("Cannot add more than 3 children to a DungeonTree node.");
        }
    }

    public void setFirstChild(DungeonTree newChild) {
        this.firstChild = newChild;
        if (firstChild != null) {
            firstChild.parent = this;
        }
    }

    public void setSecondChild(DungeonTree newChild) {
        this.secondChild = newChild;
        if (secondChild != null) {
            secondChild.parent = this;
        }
    }

    public void setThirdChild(DungeonTree newChild) {
        this.thirdChild = newChild;
        if (thirdChild != null) {
            thirdChild.parent = this;
        }
    }

    public HashSet<DungeonTree> getChildren() {
        HashSet<DungeonTree> children = new HashSet<>();
        if (firstChild != null) children.add(firstChild);
        if (secondChild != null) children.add(secondChild);
        if (thirdChild != null) children.add(thirdChild);
        return children;
    }

    public HashSet<DungeonTree> getGrandchildren() {
        HashSet<DungeonTree> grandchildren = new HashSet<>();
        if (firstChild != null) {
            grandchildren.addAll(firstChild.getChildren());
        }
        if (secondChild != null) {
            grandchildren.addAll(secondChild.getChildren());
        }
        if (thirdChild != null) {
            grandchildren.addAll(thirdChild.getChildren());
        }
        return grandchildren;
    }

    public DungeonTree removeChild(int childIndex) {
        DungeonTree removed;
        if (childIndex == 0) {
            removed = firstChild;
            firstChild = null;
            if (this.thirdChild != null) {
                firstChild = thirdChild;
                thirdChild = null;
            }
            else if (this.secondChild != null) {
                firstChild = secondChild;
                secondChild = null;
            }
        } else if (childIndex == 1) {
            removed = secondChild;
            secondChild = null;
            if (this.thirdChild != null) {
                secondChild = thirdChild;
                thirdChild = null;
            }
        } else {
            removed = thirdChild;
            thirdChild = null;
        }
        if (removed == null) {
            System.out.println("Cannot remove - no child at index " + childIndex);
        } else {
            removed.parent = null;
        }
        return removed;
    }

    public DungeonTree removeRandomChild() {
        HashSet<DungeonTree> children = getChildren();
        if (children.isEmpty()) return null;

        int indexToRemove = (int) (Math.random() * children.size());
        DungeonTree childToRemove = new ArrayList<>(children).get(indexToRemove);

        if (childToRemove == firstChild) {
            return removeChild(0);
        } else if (childToRemove == secondChild) {
            return removeChild(1);
        } else {
            return removeChild(2);
        }
    }

    public DungeonTree getRandomChild() {
        HashSet<DungeonTree> children = getChildren();
        if (children.isEmpty()) return null;

        int indexToGet = (int) (Math.random() * children.size());
        return new ArrayList<>(children).get(indexToGet);
    }

    public void collectNodes(List<DungeonTree> out) {
        out.add(this);

        if (firstChild != null) firstChild.collectNodes(out);
        if (secondChild != null) secondChild.collectNodes(out);
        if (thirdChild != null) thirdChild.collectNodes(out);
    }

    public boolean isStartOrExit() {
        return type instanceof NodeTypes.Start || type instanceof NodeTypes.Exit;
    }

    public int countNodes() {
        List<DungeonTree> nodes = new ArrayList<>();
        collectNodes(nodes);
        return nodes.size();
    }

    public boolean hasStartAndExitOnce() {
        List<DungeonTree> nodes = new ArrayList<>();
        collectNodes(nodes);
        boolean hasStart = false;
        boolean hasExit = false;
        for (DungeonTree node : nodes) {
            if (node.type instanceof NodeTypes.Start) {
                if (hasStart) {
                    return false; // more than one start
                }
                hasStart = true;
            }
            if (node.type instanceof NodeTypes.Exit) {
                if (hasExit) {
                    return false; // more than one exit
                }
                hasExit = true;
            }
        }
        return hasStart && hasExit;
    }

    // --------------- copy ---------------
    private DungeonTree deepCopy(DungeonTree parent) {
        DungeonTree copy = new DungeonTree(this.type);
        copy.parent = parent;
        if (firstChild != null) {
            copy.firstChild = firstChild.deepCopy(copy);
        }
        if (secondChild != null) {
            copy.secondChild = secondChild.deepCopy(copy);
        }
        if (thirdChild != null) {
            copy.thirdChild = thirdChild.deepCopy(copy);
        }
        return copy;
    }

    public DungeonTree deepCopy() {
        return deepCopy(null);
    }

    // --------------- testing ---------------
    public void printBFS() {
        Queue<DungeonTree> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            DungeonTree current = queue.poll();
            System.out.print(current.type.getShortName() + " ");
            if (current.firstChild != null) queue.add(current.firstChild);
            if (current.secondChild != null) queue.add(current.secondChild);
            if (current.thirdChild != null) queue.add(current.thirdChild);
        }
        System.out.println();
    }
}
