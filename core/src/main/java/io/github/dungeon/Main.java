package io.github.dungeon;

import com.badlogic.gdx.Game;
import io.github.dungeon.screens.MenuScreen;
import lombok.Getter;
import lombok.Setter;

public class Main extends Game {
    @Getter
    private static Main instance;
    @Getter @Setter
    private int level = 1;

    @Override
    public void create() {
        instance = this;
        setScreen(new MenuScreen());
    }
}
