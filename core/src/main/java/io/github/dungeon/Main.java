package io.github.dungeon;

import com.badlogic.gdx.Game;
import io.github.dungeon.screens.DungeonScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        setScreen(new DungeonScreen(this));
    }
}
