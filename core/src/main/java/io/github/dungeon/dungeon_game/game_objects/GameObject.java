package io.github.dungeon.dungeon_game.game_objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public abstract class GameObject implements Disposable {
    private static final float FRAME_DURATION = 0.05f;
    protected final float HITBOX = 0.8f;

    private final int framesCount;
    private final String filePrefix;
    private final boolean isMoving;
    private final float velocity;
    private Map<Action, Texture[]> texturesMap;

    private Coord position;

    @Setter private Action lastAction = Action.STAY;
    private int animationFrame = 0;
    private float animationTimer = 0f;

    public GameObject(int framesCount, String prefix, Coord position, boolean moving, float velocity) {
        this.framesCount = framesCount;
        this.filePrefix = prefix;
        this.position = position;
        this.isMoving = moving;
        this.velocity = !isMoving ? 0 : velocity;
        populateTexturesMap();
    }

    public GameObject(int framesCount, String prefix, Coord position, boolean moving) {
        this.framesCount = framesCount;
        this.filePrefix = prefix;
        this.position = position;
        this.isMoving = moving;
        this.velocity = 0;
        populateTexturesMap();
    }

    private void populateTexturesMap() {
        if (isMoving) {
            texturesMap = Map.of(
                    Action.UP, new Texture[framesCount],
                    Action.DOWN, new Texture[framesCount],
                    Action.LEFT, new Texture[framesCount],
                    Action.RIGHT, new Texture[framesCount],
                    Action.STAY, new Texture[framesCount]
            );
        } else {
            texturesMap = Map.of(
                    Action.STAY, new Texture[framesCount]
            );
        }
        for (Action action : texturesMap.keySet()) {
            for (int i = 0; i < framesCount; i++) {
                try {
                    String filePath = filePrefix + action.getName() + "/" + i + ".png";
                    texturesMap.get(action)[i] = new Texture(filePath);
                } catch (Exception e) {
                    System.out.println("No texture for " + action.getName() + " (" + e.getMessage() + ")");
                }
            }
        }
    }

    public void update(float delta) {
        animationTimer += delta;

        if (animationTimer >= FRAME_DURATION) {
            animationFrame = (animationFrame + 1) % framesCount;
            animationTimer -= FRAME_DURATION;
        }
    }


    public Texture getCurrentTexture() {
        return texturesMap.get(lastAction)[animationFrame];
    }

    public Coord move(Action action) {
        this.position = this.position.applyAction(action, velocity);
        this.lastAction = action;
        return position;
    }

    public void undoMove() {
        this.position = this.position.applyAction(lastAction.opposite(), velocity);
    }

    // hitboxes
    public float left() {
        return position.getX() + (1 - HITBOX) / 2f;
    }

    public float right() {
        return left() + HITBOX;
    }

    public float top() {
        return position.getY() + (1 - HITBOX) / 2f;
    }

    public float bottom() {
        return top() + HITBOX;
    }

    @Override
    public void dispose() {
        for (Texture[] textures : texturesMap.values()) {
            for (Texture texture : textures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
        }
    }
}
