package io.github.dungeon.dungeon_game.game_objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;
import lombok.Getter;
import lombok.Setter;


@Getter
public abstract class GameObject {
    protected final boolean isMoving;
    protected final float velocity;

    protected int frame = 0;
    protected float timer = 0f;
    protected Texture spriteSheet = null;

    protected Coord position;

    @Setter protected Action lastAction = Action.STAY;

    public GameObject(Coord position, boolean moving, float velocity) {
        this.position = position;
        this.isMoving = moving;
        this.velocity = !isMoving ? 0 : velocity;
    }

    public GameObject(Coord position, boolean moving) {
        this.position = position;
        this.isMoving = moving;
        this.velocity = 0;
    }

    public Coord move(Action action) {
        this.position = this.position.applyAction(action, velocity);
        this.lastAction = action;
        return position;
    }

    public void undoMove() {
        this.position = this.position.applyAction(lastAction.opposite(), velocity);
    }

    protected TextureRegion[] extract(TextureRegion[][] regions, int row, int count) {
        TextureRegion[] frames = new TextureRegion[count];
        System.arraycopy(regions[row], 0, frames, 0, count);
        return frames;
    }

    public float getScale() {
        return 1f;
    }

    public float getHitbox() {
        return 0.8f;
    }

    // hitboxes
    public float left() {
        return position.getX() + (1 - getHitbox()) / 2f;
    }

    public float right() {
        return left() + getHitbox();
    }

    public float top() {
        return position.getY() + (1 - getHitbox()) / 2f;
    }

    public float bottom() {
        return top() + getHitbox();
    }
}
