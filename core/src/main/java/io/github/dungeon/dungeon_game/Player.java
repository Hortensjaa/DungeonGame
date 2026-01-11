package io.github.dungeon.dungeon_game;


import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.game_objects.Character;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player extends Character {
    @Getter private static final int MAX_HP = 100;
    @Getter private static final int MAX_STAMINA = 50;

    private int hp = MAX_HP;
    private int stamina = MAX_STAMINA;
    private int score = 0;

    public Player(Coord position) {
        super("player/sheet.png", position, 0.1f);
    }

    public Void addScore() {
        this.score++;
        return null;
    }

    public Void addHp(int value) {
        if (this.hp + value > MAX_HP) {
            this.hp = MAX_HP;
            return null;
        }
        this.hp += value;
        return null;
    }

    public void decreaseScore() {
        this.score = Math.max(0, this.score - 1);
    }

    public boolean decreaseHp(int value) {
        this.hp -= value;
        return this.hp <= 0;
    }
}
