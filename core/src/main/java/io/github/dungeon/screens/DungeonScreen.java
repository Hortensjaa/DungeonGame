package io.github.dungeon.screens;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import io.github.dungeon.Main;
import io.github.dungeon.common.Action;
import io.github.dungeon.dungeon_game.DungeonGame;
import io.github.dungeon.generator.GenerationUtils;
import io.github.dungeon.generator.grid.GridDefinition;
import io.github.dungeon.render.DungeonRenderer;
import io.github.dungeon.render.UIRenderer;


public class DungeonScreen implements Screen, InputProcessor {

    private final DungeonGame game;
    private final UIRenderer uiRenderer;
    private final DungeonRenderer renderer;
    private Action currentAction = Action.STAY;

    public DungeonScreen(Main gdxGame) {
        long startTime = System.currentTimeMillis();
        GridDefinition def = null;
        while (def == null) {
            try {
                def = GenerationUtils.generateGivenDifficulty("202604111244", gdxGame.getLevel());
            } catch (Exception e) {
                System.out.println("Generation failed, retrying...");
            }
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Generation took: " + duration + " ms");
        GenerationTimeLogger.log(gdxGame.getLevel(), duration);
        this.game = new DungeonGame(def);
        this.renderer = new DungeonRenderer(game);
        this.uiRenderer = new UIRenderer(game.getPlayer(), gdxGame.getLevel());
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (currentAction != Action.STAY) {
            game.move(currentAction);
        } else {
            game.getPlayer().setLastAction(Action.STAY);
        }
        game.update(delta);      // logic
        renderer.render();      // drawing
        uiRenderer.render();    // UI (bars, score)

        if (game.hasWon()) {
            Main.getInstance().setScreen(new VictoryScreen());
        } else if (game.getPlayer().getHp() <= 0) {
            io.github.dungeon.Main.getInstance().setScreen(new YouDiedScreen());
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.getViewport().update(width, height, true);
    }


    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) currentAction = Action.UP;
        if (keycode == Input.Keys.W || keycode == Input.Keys.UP) currentAction = Action.DOWN;
        if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) currentAction = Action.LEFT;
        if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) currentAction = Action.RIGHT;
        if (keycode == Input.Keys.ESCAPE) Gdx.app.exit();
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        currentAction = Action.STAY;
        return true;
    }


    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int i, int i1, int i2) {
        return false;
    }

    @Override
    public boolean mouseMoved(int i, int i1) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomSpeed = 0.1f;
        renderer.getCamera().zoom += amountY * zoomSpeed;
        renderer.getCamera().zoom = MathUtils.clamp(renderer.getCamera().zoom, 0.5f, 5f);
        return true;
    }

}

class GenerationTimeLogger {
    public static void log(int level, long timeMs) {
        try (FileWriter fw = new FileWriter("generation_times.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println( level + ", " + timeMs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
