package io.github.dungeon.dungeon_game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.game_objects.GameObject;

public class Goal extends GameObject implements Disposable {
    protected static final float FRAME_DURATION = 0.1f;
    private final TextureRegion[] animations;

    public Goal(Coord position) {
        super(position, false, 0);

        this.spriteSheet = new Texture("goal/sprite-sheet.png");
        this.animations = splitSpriteSheet();
    }

    private TextureRegion[] splitSpriteSheet() {
        // Split the entire sheet into 64x64 tiles
        TextureRegion[][] regions = TextureRegion.split(
            spriteSheet,
            64,
            64
        );

        // Manually extract frames from irregular layout
        // Row 0: 4 frames (indices 0-3)
        // Row 1: 3 frames (indices 0-2)
        TextureRegion[] frames = new TextureRegion[7];

        // First row - 4 frames
        frames[0] = regions[0][0];
        frames[1] = regions[0][1];
        frames[2] = regions[0][2];
        frames[3] = regions[0][3];

        // Second row - 3 frames
        frames[4] = regions[1][0];
        frames[5] = regions[1][1];
        frames[6] = regions[1][2];

        return frames;
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
