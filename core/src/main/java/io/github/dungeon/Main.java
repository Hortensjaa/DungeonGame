package io.github.dungeon;

import com.badlogic.gdx.Game;
import io.github.dungeon.screens.MenuScreen;
import lombok.Getter;

public class Main extends Game {
    @Getter
    private static Main instance;

    @Override
    public void create() {
        instance = this;
        setScreen(new MenuScreen());
    }
}
