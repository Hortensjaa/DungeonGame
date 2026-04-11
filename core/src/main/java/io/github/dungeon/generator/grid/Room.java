package io.github.dungeon.generator.grid;

import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class Room {
    private int left; // with offset applied
    private int top; // with offset applied
    int partitionWidth;
    int partitionHeight;
    float difficulty;
    float reward;
    private final Coord entrance = null;
    private final Set<Coord> exits = new HashSet<>();

    public Room(
        int startX, int startY,
        int partitionWidth, int partitionHeight,
        float difficulty, float reward
    ) {
        left = startX + Constants.WALL_OFFSET;
        top = startY + Constants.WALL_OFFSET;
        this.partitionWidth = partitionWidth;
        this.partitionHeight = partitionHeight;
        this.difficulty = difficulty;
        this.reward = reward;
    }

    public Coord getEnd() {
        float right = left + partitionWidth - 2 * Constants.WALL_OFFSET;
        float bottom = top + partitionHeight - 2 * Constants.WALL_OFFSET;
        return new Coord(right, bottom);
    }

    public Coord getCenter() {
        Coord end = getEnd();
        float centerX = (left + end.getX()) / 2;
        float centerY = (top + end.getY()) / 2;
        return new Coord((int) centerX, (int) centerY);
    }

    private boolean isCorridor(int x, int y, int[][] grid) {
        return grid[y][x] == Constants.CORRIDOR;
    }

    private void findEntrances(int[][] grid) {
        Coord end = getEnd();
        for (int y = top; y < end.getY(); y++) {
            for (int x = left; x < end.getX(); x++) {
                if (isCorridor(x + 1, y, grid)
                    || isCorridor(x - 1, y, grid)
                    || isCorridor(x, y + 1, grid)
                    || isCorridor(x, y - 1, grid)) {
                    exits.add(new Coord(x, y));
                }
            }
        }
    }

    public RoomContents getRoomContents(int[][] grid) {
        findEntrances(grid);
        return RoomPopulator.populate(this);
    }
}
