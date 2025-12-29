package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.generator.tree.DungeonTree;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LayoutGenerator {

    private static final Random RANDOM = new Random();

    static boolean generateLayout(DungeonTree node, LayoutField[][] grid, int x, int y, Direction directionFromParent) {
        // if spot is not free, fail
        if (grid[y][x] != null) {
            return false;
        }
        // Place the current node if the spot is free
        grid[y][x] = new LayoutField(node.getType(), directionFromParent);

        List<DungeonTree> children = new ArrayList<>(node.getChildren());
        Collections.shuffle(children, RANDOM);  // Shuffle for diversity
        return tryPlaceChildren(grid, x, y, children, 0);
    }

    /**
     * Exhaustive backtracking with shuffled directions: Try all directions for each child in shuffled order.
     * childIndex: Current child to place (0 to children.size()-1).
     */
    private static boolean tryPlaceChildren(LayoutField[][] grid, int parentX, int parentY, List<DungeonTree> children, int childIndex) {
        int width = grid[0].length;
        int height = grid.length;

        if (childIndex == children.size()) {
            return true;  // All children placed
        }

        DungeonTree child = children.get(childIndex);
        List<Direction> directions = Direction.shuffledDirections();

        for (Direction dir : directions) {
            int nx = parentX + dir.getDx();
            int ny = parentY + dir.getDy();

            if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[ny][nx] == null) {
                // Try placing this child and recurse to next child
                if (generateLayout(child, grid, nx, ny, dir.opposite())) {
                    // Record position for backtracking
                    grid[parentY][parentX].placedChildrenPositions.add(new Coord(nx, ny));
                    // Recurse to next child
                    if (tryPlaceChildren(grid, parentX, parentY, children, childIndex + 1)) {
                        return true;
                    }
                    // Backtrack this child if subtree failed
                    backtrackChildPlacement(grid, nx, ny);
                    grid[parentY][parentX].placedChildrenPositions.remove(new Coord(nx, ny));
                }
            }
        }

        return false;  // No direction worked for this child
    }

    /**
     * Recursively removes the given node's placement and all its descendants' placements.
     */
    private static void backtrackChildPlacement(LayoutField[][] grid, int x, int y) {
        LayoutField field = grid[y][x];
        if (field == null) {
            return;
        }

        for (Coord pos : new ArrayList<>(field.placedChildrenPositions)) {
            backtrackChildPlacement(grid, (int) pos.getX(), (int) pos.getY());
        }

        grid[y][x] = null;
    }

    private static LayoutField[][] trim(LayoutField[][] layout) {
        int h = layout.length;
        int w = layout[0].length;

        int minX = w, minY = h;
        int maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (layout[y][x] != null) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX == -1) {
            return new LayoutField[0][0];
        }

        int newW = maxX - minX + 1;
        int newH = maxY - minY + 1;

        LayoutField[][] trimmed = new LayoutField[newH][newW];

        for (int y = 0; y < newH; y++) {
            System.arraycopy(layout[y + minY], minX, trimmed[y], 0, newW);
        }

        return trimmed;
    }

    public static LayoutField[][] generateLayout(DungeonTree root, int max_retries) throws IllegalArgumentException {
        int centerX = Constants.MAX_LAYOUT_WIDTH / 2;
        int centerY = Constants.MAX_LAYOUT_HEIGHT / 2;

        for (int attempt = 0; attempt < max_retries; attempt++) {
            LayoutField[][] grid = new LayoutField[Constants.MAX_LAYOUT_HEIGHT][Constants.MAX_LAYOUT_WIDTH];
            if (generateLayout(root, grid, centerX, centerY, null)) {
                return trim(grid);  // Success with this shuffle and root position
            }
            // If all root positions failed, retry with new shuffles (next attempt)
        }

        throw new IllegalArgumentException("Failed to generate layout after " + max_retries + " retries with shuffling.");
    }

    //    ------------------ printer -----------------------
    public static void printLayout(LayoutField[][] layout) {
        for (LayoutField[] layoutFields : layout) {
            for (int x = 0; x < layout[0].length; x++) {
                if (layoutFields[x] != null) {
                    System.out.print(layoutFields[x].toString());
                } else {
                    System.out.print("[ ] ");
                }
            }
            System.out.println();
        }
    }
}
