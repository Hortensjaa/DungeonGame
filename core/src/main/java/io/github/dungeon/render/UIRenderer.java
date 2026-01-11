package io.github.dungeon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import io.github.dungeon.dungeon_game.Player;


public class UIRenderer implements Disposable {
    private final Player player;
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;
    OrthographicCamera uiCamera;


    public UIRenderer(Player player) {
        this.player = player;
        this.shapeRenderer = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight());

    }

    public void render() {
        uiCamera.update();

        batch.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        drawBars();
        drawScore();
    }

    private void drawScore() {
        float h = uiCamera.viewportHeight;

        batch.begin();
        font.draw(batch, "Score: " + player.getScore(), 20, h - 10);
        batch.end();
    }

    private void drawBars() {
        float h = uiCamera.viewportHeight;

        float barWidth = 200;
        float barHeight = 16;
        float margin = 20;

        float x = margin;
        float y = h - barHeight - margin - 10;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float hpPercent = (float) player.getHp() / Player.getMAX_HP();

        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(x, y, barWidth * hpPercent, barHeight);

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}

