package io.github.dungeon.dungeon_game.game_objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.dungeon.common.Action;
import io.github.dungeon.common.Coord;

import java.util.EnumMap;
import java.util.Map;

public abstract class Character extends GameObject implements Disposable {

    private static final float FRAME_DURATION_MOVE = 0.1f;
    private static final float FRAME_DURATION_IDLE = 0.25f;

    private final Map<Action, TextureRegion[]> animations;

    protected Character(
        String spriteSheetPath,
        Coord position,
        float velocity
    ) {
        super(position, true, velocity);

        this.spriteSheet = new Texture(spriteSheetPath);
        this.animations = new EnumMap<>(Action.class);

        splitSpriteSheet();
    }

    private void splitSpriteSheet() {
        TextureRegion[][] regions = TextureRegion.split(
            spriteSheet,
            64,
            64
        );

        animations.put(Action.DOWN, extract(regions, 8, 9));
        animations.put(Action.LEFT, extract(regions, 9, 9));
        animations.put(Action.UP, extract(regions, 10, 9));
        animations.put(Action.RIGHT, extract(regions, 11, 9));
        animations.put(Action.STAY, extract(regions, 10, 2));
    }

    public void update(float delta) {
        timer += delta;

        float frameDuration = (lastAction == Action.STAY) ? FRAME_DURATION_IDLE : FRAME_DURATION_MOVE;

        if (timer >= frameDuration) {
            frame = (frame + 1) % animations.get(lastAction).length;
            timer -= frameDuration;
        }
    }

    public TextureRegion getCurrentFrame() {
        TextureRegion[] frames = animations.get(lastAction);
        return frames[Math.min(frame, frames.length - 1)];
    }

    public void dispose() {
        spriteSheet.dispose();
    }
}
