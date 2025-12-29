package io.github.dungeon.dungeon_game.game_objects;


import io.github.dungeon.common.Coord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player extends GameObject {
    private static final int MAX_STAMINA = 10;

    private int lives = 1;
    private int score = 0;
    private int stamina = MAX_STAMINA;

    public Player(Coord position) {
        super(10, "player/", position, true, 0.2f);
    }

    public Void addScore() {
        this.score++;
        return null;
    }

    public Void addLife() {
        this.lives++;
        return null;
    }

    public void decreaseScore() {
        this.score--;
    }

    public boolean decreaseLife() {
        this.lives--;
        return this.lives <= 0;
    }
}
