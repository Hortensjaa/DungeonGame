package io.github.dungeon.dungeon_game.danger;

import io.github.dungeon.common.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DangerType {
    // ---------------------- enum constants ----------------------
    FIRE(
        "FIRE",
        "enemies/fire/sheet.png",
        160/8, 24, 8, null),

    LIZARD_HORIZONTAL("LIZARD", "enemies/lizard/sheet.png", 8, Direction.RIGHT),
    LIZARD_VERTICAL("LIZARD", "enemies/lizard/sheet.png", 8, Direction.DOWN);

    // ---------------------- class properties ----------------------
    private final String name;
    private final String path;

    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;

    private final Direction movingDir;

    private static final float LIZARD_PROBABILITY = 0.2f;

    public static DangerType getRandom() {
        return Math.random() < LIZARD_PROBABILITY ? LIZARD_HORIZONTAL : FIRE;
    }

    DangerType(String name, String path, int frameCount,  Direction movingDir) {
        this.name = name;
        this.path = path;
        this.frameWidth = 64;
        this.frameHeight = 64;
        this.frameCount = frameCount;
        this.movingDir = movingDir;
    }
}
