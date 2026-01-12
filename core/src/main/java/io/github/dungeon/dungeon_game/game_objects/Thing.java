package io.github.dungeon.dungeon_game.game_objects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.dungeon.common.Coord;


public abstract class Thing extends GameObject implements Disposable {
    protected static final float FRAME_DURATION = 0.1f;
    private final TextureRegion[] animations;

    protected Thing(
        String spriteSheetPath,
        Coord position,
        // sprite specific, as I have sprites from different sources
        int frameWidth,
        int frameHeight,
        int frameCount
    ) {
        super(position, false, 0);

        this.spriteSheet = new Texture(spriteSheetPath);
        this.animations = splitSpriteSheet(frameWidth, frameHeight, frameCount);
    }

    private TextureRegion[] splitSpriteSheet(int frameWidth, int frameHeight, int frameCount) {
        TextureRegion[][] regions = TextureRegion.split(
            spriteSheet,
            frameWidth,
            frameHeight
        );
        return extract(regions, 0, frameCount);
    }

    public void update(float delta) {
        timer += delta;
        if (timer >= FRAME_DURATION) {
            frame = (frame + 1) % animations.length;
            timer -= FRAME_DURATION;
        }
    }

    public TextureRegion getCurrentFrame() {
        return animations[Math.min(frame, animations.length - 1)];
    }

    @Override
    public float getHitbox() {
        return 0.5f;
    }

    public void dispose() {
        spriteSheet.dispose();
    }
}
