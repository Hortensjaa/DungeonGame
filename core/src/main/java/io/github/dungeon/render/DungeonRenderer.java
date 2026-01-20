package io.github.dungeon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.DungeonGame;
import io.github.dungeon.dungeon_game.danger.Enemy;
import io.github.dungeon.dungeon_game.game_objects.Interactable;
import io.github.dungeon.dungeon_game.reward.Reward;
import lombok.Getter;

import java.util.Map;

public class DungeonRenderer implements Disposable {

    private final DungeonGame game;
    private final SpriteBatch batch;
    @Getter private final OrthographicCamera camera;
    @Getter private final ExtendViewport viewport;

    private final Map<Integer, Texture> gridTextures = Map.of(
        Constants.WALL, new Texture(Constants.WALL_SPRITE),
        Constants.ROOM, new Texture(Constants.ROOM_SPRITE),
        Constants.CORRIDOR, new Texture(Constants.CORRIDOR_SPRITE)
    );
    private final Texture goalTexture = new Texture(Constants.GOAL_SPRITE);

    public DungeonRenderer(DungeonGame game) {
        this.game = game;
        this.batch = new SpriteBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(false,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());
        viewport = new ExtendViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera);
        viewport.apply();

    }

    public void render() {
        Coord p = game.getPlayer().getPosition();

        float targetX = p.getX() * Constants.CELL_SIZE + Constants.CELL_SIZE / 2f;
        float targetY = p.getY() * Constants.CELL_SIZE + Constants.CELL_SIZE / 2f;

        camera.position.set(targetX, targetY, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        drawGrid();
        drawPlayer();
        drawInteractables();

        batch.end();
    }

    private void drawGrid() {

        int[][] grid = game.getGrid();

        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                Texture t = gridTextures.get(grid[y][x]);
                batch.draw(t, x * Constants.CELL_SIZE, y * Constants.CELL_SIZE);
            }
        }

        batch.draw(goalTexture, game.getExit().getX() * Constants.CELL_SIZE, game.getExit().getY() * Constants.CELL_SIZE);
    }

    private void drawPlayer() {
        Coord p = game.getPlayer().getPosition();
        batch.draw(
            game.getPlayer().getCurrentFrame(),
            p.getX() * Constants.CELL_SIZE,
            p.getY() * Constants.CELL_SIZE,
            Constants.CELL_SIZE,
            Constants.CELL_SIZE
        );
    }

    private void drawInteractables() {
        for (Interactable e : game.getInteractables()) {
            Coord c = e.getPosition();
            TextureRegion t = e.getCurrentFrame();

            float cellX = c.getX() * Constants.CELL_SIZE;
            float cellY = c.getY() * Constants.CELL_SIZE;

            int texW = t.getRegionWidth();
            int texH = t.getRegionHeight();

            float drawWidth;
            float drawHeight;

            float aspect = (float) texW / texH;
            // fit
            if (texW >= texH) {
                drawWidth = Constants.CELL_SIZE;
                drawHeight = Constants.CELL_SIZE / aspect;
            } else {
                drawHeight = Constants.CELL_SIZE;
                drawWidth = Constants.CELL_SIZE * aspect;
            }
            // scale
            drawWidth *= e.getScale();
            drawHeight *= e.getScale();
            // center
            float drawX = cellX + (Constants.CELL_SIZE - drawWidth) / 2f;
            float drawY = cellY + (Constants.CELL_SIZE - drawHeight) / 2f;

            batch.draw(t, drawX, drawY, drawWidth, drawHeight);
        }
    }

    public void dispose() {
        batch.dispose();
        game.getPlayer().dispose();
        goalTexture.dispose();
        for (Interactable e : game.getInteractables()) {
            e.dispose();
        }
        for (Texture t : gridTextures.values()) {
            t.dispose();
        }
    }
}

