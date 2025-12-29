package io.github.dungeon.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum Direction implements Move {
    UP(0, -1, "UP"),
    DOWN(0, 1, "DOWN"),
    LEFT(-1, 0, "LEFT"),
    RIGHT(1, 0, "RIGHT");

    private final int dx;
    private final int dy;
    private final String name;

    public Direction opposite() {
        if (this == UP) return DOWN;
        if (this == DOWN) return UP;
        if (this == LEFT) return RIGHT;
        if (this == RIGHT) return LEFT;
        return this;
    }

    public Direction perpendicular() {
        if (this == UP) return RIGHT;
        if (this == DOWN) return RIGHT;
        if (this == LEFT) return UP;
        if (this == RIGHT) return UP;
        return this;
    }

    public static Direction fromString(String s) {
        for (Direction d : values()) {
            if (d.name.equalsIgnoreCase(s)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown direction: " + s);
    }

    public static Direction randomDirection() {
        Direction[] directions = values();
        int index = (int) (Math.random() * directions.length);
        return directions[index];
    }

    public static List<Direction> shuffledDirections() {
        List<Direction> directions = new ArrayList<>(Arrays.asList(values()));
        java.util.Collections.shuffle(directions);
        return directions;
    }

    public static boolean isVertical(Direction dir) {
        return dir == UP || dir == DOWN;
    }

    public static boolean isHorizontal(Direction dir) {
        return dir == LEFT || dir == RIGHT;
    }
}

