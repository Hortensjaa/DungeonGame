package io.github.dungeon.dungeon_game.danger;

import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.Player;
import io.github.dungeon.dungeon_game.game_objects.Interactable;
import io.github.dungeon.dungeon_game.game_objects.Thing;
import lombok.Getter;


public class Trap extends Thing implements Interactable {
    @Getter private final Action action = Action.STAY;

    public Trap(DangerType type, Coord position) {
        super(type.getPath(), position, type.getFrameWidth(), type.getFrameHeight(), type.getFrameCount());
    }

    public void onInteraction(Player player) {
        player.decreaseScore(); // todo: in standalone game it will be health decrease
    }
}
