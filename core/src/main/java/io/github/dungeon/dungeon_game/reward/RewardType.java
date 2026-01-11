package io.github.dungeon.dungeon_game.reward;

import io.github.dungeon.dungeon_game.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@AllArgsConstructor
@Getter
public enum RewardType {
    COIN(
        "COIN",
        "rewards/coin/MonedaD.png",
        16, 16, 5,
        Player::addScore),

    HEALTH_POTION(
        "HEALTH POTION",
        "rewards/potions/health.png",
        176/8, 37, 8,
        p -> p.addHp(50));

    private final String name;
    private final String path;

    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final Function<Player, Void> effect;

    private static final float POTION_PROBABILITY = 0.1f;

    public static RewardType getRandom() {
        return Math.random() < POTION_PROBABILITY ? HEALTH_POTION : COIN;
    }
}

