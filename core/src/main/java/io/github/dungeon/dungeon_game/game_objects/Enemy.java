package io.github.dungeon.dungeon_game.game_objects;

import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;
import lombok.Getter;
import lombok.Setter;

public class Enemy extends GameObject {
    @Setter @Getter private Action action;
    private int turnCounter = 0;

    public Enemy(EnemyType type, Coord position) {
        super(type.getFrames(), type.getFilePrefix(), position, type.isMoving(), 0.1f);
        if (type.isMoving()) {
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

    public void attack(Player player) {
        player.decreaseScore(); // todo: in standalone game it will be health decrease
    }
}
