package io.github.dungeon.dungeon_game.reward;


import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.Player;
import io.github.dungeon.dungeon_game.game_objects.Interactable;
import io.github.dungeon.dungeon_game.game_objects.Thing;
import lombok.Getter;

import java.util.function.Function;

@Getter
public class Reward extends Thing implements Interactable {
    private final Function<Player, Void> effect;

    public Reward(RewardType type, Coord position) {
        super(type.getPath(), position, type.getFrameWidth(), type.getFrameHeight(), type.getFrameCount());
        this.effect = type.getEffect();
    }

    @Override
    public void onInteraction(Player player) {
        effect.apply(player);
    }

    @Override
    public float getScale() {
        return 0.5f;
    }
}
