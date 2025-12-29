package io.github.dungeon.dungeon_game.game_objects;


import com.badlogic.gdx.graphics.g2d.Sprite;
import io.github.dungeon.common.Coord;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
public class Reward extends GameObject {
    private final Function<Player, Void> effect;

    public Reward(RewardType type, Coord position) {
        super(type.getFrames(), type.getFilePrefix(), position, false);
        this.effect = type.getEffect();
    }

    public void applyEffect(Player player) {
        effect.apply(player);
    }
}
