package io.github.dungeon.generator.room;

import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.danger.DangerType;
import io.github.dungeon.dungeon_game.reward.RewardType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Getter
@NoArgsConstructor
public class RoomContents {
    private final Map<Coord, DangerType> enemies = new HashMap<>();
    private final Map<Coord, RewardType> rewards = new HashMap<>();

    public void addTrap(Coord position) {
        enemies.put(position, DangerType.FIRE);
    }

    public void addEnemyX(Coord position) {
        enemies.put(position, DangerType.LIZARD_HORIZONTAL);
    }

    public void addEnemyY(Coord position) {
        enemies.put(position, DangerType.LIZARD_VERTICAL);
    }

    public void addCoin(Coord position) {
        rewards.put(position, RewardType.COIN);
    }

    public void addPotion(Coord position) {
        rewards.put(position, RewardType.HEALTH_POTION);
    }

    public void addRandomHazard(Coord position) {
        Random random = new Random();
        double roll = random.nextDouble();
        if (roll < 0.5) addTrap(position);
        else if (roll < 0.75) addEnemyX(position);
        else addEnemyY(position);
    }

    public void addRandomReward(Coord position) {
        Random random = new Random();
        double roll = random.nextDouble();
        if (roll < 0.9) addCoin(position);
        else addPotion(position);
    }

    public RoomContents deepcopy() {
        RoomContents copy = new RoomContents();
        getEnemies().forEach((coord, type) -> copy.getEnemies().put(coord, type));
        getRewards().forEach((coord, type) -> copy.getRewards().put(coord, type));
        return copy;
    }
}
