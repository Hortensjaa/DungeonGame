package io.github.dungeon.generator.grid;

import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.danger.DangerType;
import io.github.dungeon.dungeon_game.reward.RewardType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class RoomContents {
    private final Map<Coord, DangerType> enemies = new HashMap<>();
    private final Map<Coord, RewardType> rewards = new HashMap<>();

    public void addTrap(Coord position) {
        enemies.put(position, DangerType.FIRE);
    }

    public void addLizardX(Coord position) {
        enemies.put(position, DangerType.LIZARD_HORIZONTAL);
    }

    public void addLizardY(Coord position) {
        enemies.put(position, DangerType.LIZARD_VERTICAL);
    }

    public void addCoin(Coord position) {
        rewards.put(position, RewardType.COIN);
    }
}
