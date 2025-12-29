package io.github.dungeon.dungeon_game.game_objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EnemyType {
    FIRE("FIRE", "enemies/fire/cut/", 8, 1, false),
    GOLEM("GOLEM", "enemies/golem/", 8, 2, true);

    private final String name;
    private final String filePrefix;
    private final int frames;
    private final int severity;
    private final boolean moving;

    private static final float GOLEM_PROBABILITY = 0.2f;

    public static EnemyType getRandom() {
        return Math.random() < GOLEM_PROBABILITY ? GOLEM : FIRE;
    }
}
