package io.github.dungeon.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Action implements Move {

    UP(0, -1, "UP"),
    DOWN(0, 1, "DOWN"),
    LEFT(-1, 0, "LEFT"),
    RIGHT(1, 0, "RIGHT"),
    STAY(0, 0, "STAY");

    private final int dx;
    private final int dy;
    private final String name;

    public Action opposite() {
        if (this == UP) return DOWN;
        if (this == DOWN) return UP;
        if (this == LEFT) return RIGHT;
        if (this == RIGHT) return LEFT;
        return this;
    }
}

