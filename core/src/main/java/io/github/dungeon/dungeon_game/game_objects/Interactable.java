package io.github.dungeon.dungeon_game.game_objects;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.Player;

public interface Interactable {
    void onInteraction(Player player);
    Coord getPosition();
    void update(float delta);
    TextureRegion getCurrentFrame();
    void dispose();
    float getScale();
    int getDrawLayer();
}
