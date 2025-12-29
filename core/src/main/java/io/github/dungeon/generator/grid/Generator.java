package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;

public abstract class Generator {
    protected static int[][] initialGridWalls(int rows, int columns) {
        int[][] grid = new int[rows][columns];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                grid[y][x] = Constants.WALL;
            }
        }
        return grid;
    }

    protected static int randomRow(int rows) {
        return (int) (Math.random() * rows);
    }

    protected static int randomColumn(int columns) {
        return (int) (Math.random() * columns);
    }

    protected static Coord randomCoord(int rows, int columns) {
        return new Coord(randomColumn(columns), randomRow(rows));
    }

    protected static Coord randomCoordInRoom(int min_x, int max_x, int min_y, int max_y) {
        int x = min_x + (int) (Math.random() * (max_x - min_x));
        int y = min_y + (int) (Math.random() * (max_y - min_y));
        return new Coord(x, y);
    }
}
