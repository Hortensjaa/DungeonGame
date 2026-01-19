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
    private static final float HIT_COOLDOWN = 2.0f; // seconds

    private float cooldown = 0;

    private int hp = MAX_HP;
    private int stamina = MAX_STAMINA;
    private int score = 0;

    public Player(Coord position) {
        super("player/sheet.png", position, 0.1f);
    }

    public void update(float delta) {
        super.update(delta);
        if (cooldown > 0) {
            cooldown -= delta;
        }
    }

    private boolean canBeHit() {
        return cooldown <= 0;
    }

    private void triggerCooldown() {
        cooldown = HIT_COOLDOWN;
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
        if (!canBeHit()) return;

        this.score = Math.max(0, this.score - 1);
        triggerCooldown();
    }

    public boolean decreaseHp(int value) {
        if (!canBeHit()) return false;

        this.hp -= value;
        triggerCooldown();
        return this.hp <= 0;
    }
}
