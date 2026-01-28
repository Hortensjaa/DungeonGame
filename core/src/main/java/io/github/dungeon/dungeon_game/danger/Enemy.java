package io.github.dungeon.dungeon_game.danger;

import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.game_objects.Character;
import io.github.dungeon.dungeon_game.Player;
import io.github.dungeon.dungeon_game.game_objects.Interactable;
import lombok.Getter;
import lombok.Setter;

public class Enemy extends Character implements Interactable {
    @Setter @Getter private Action action;

    public Enemy(DangerType type, Coord position) {
        super(type.getPath(), position, 0.05f);
        if (type.getMovingDir() != null) {
            this.action = Math.random() < 0.5 ? Action.LEFT : Action.UP;
        } else {
            this.action = Action.STAY;
        }
    }

    @Override
    public Coord move(Action action) {
        if (action == Action.STAY) {
            return getPosition();
        }
        return super.move(action);
    }

    public void onInteraction(Player player) {
        player.decreaseScore(); // todo: in standalone game it will be health decrease
    }

    @Override
    public int getDrawLayer() {
        return 3;
    }
}
