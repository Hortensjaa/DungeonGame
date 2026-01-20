package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.danger.DangerType;
import io.github.dungeon.dungeon_game.reward.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class GridDefinition {

    @Builder.Default
    private final int[][] grid = new int[Constants.ROWS][Constants.COLUMNS];

    @Builder.Default
    private final Coord playerStart = new Coord(0, 0);

    @Builder.Default
    private final Coord exit = new Coord(Constants.COLUMNS - 1, Constants.ROWS - 1);

    @Builder.Default
    private final Map<Coord, DangerType> dangers = new HashMap<>();

    @Builder.Default
    private final Map<Coord, RewardType> rewards = new HashMap<>();

    // methods
    public int rows() {
        return grid.length;
    }
    public int columns() {
        return grid[0].length;
    }

    public int getCoordValue(Coord coord) {
        return grid[(int) coord.getY()][(int) coord.getX()];
    }

    public void setCoordValue(Coord coord, int value) {
        grid[(int) coord.getY()][(int) coord.getX()] = value;
    }

    public void prettyPrint() {
        System.out.print("   ");
        for (int x = 0; x < columns(); x++) {
            System.out.printf("%2d ", x);
        }
        System.out.println();
        for (int y = 0; y < rows(); y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < columns(); x++) {
                System.out.printf("%2d ", grid[y][x]);
            }
            System.out.println();
        }
    }

}

