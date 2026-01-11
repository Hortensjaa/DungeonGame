package io.github.dungeon.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
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

    public DungeonScreen(Game gdxGame) {
        GridDefinition def = GenerationUtils.generateFromFile("Large_rooms_8x6", 4, 5);
        this.game = new DungeonGame(def);
        this.renderer = new DungeonRenderer(game);
        this.uiRenderer = new UIRenderer(game.getPlayer());
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
        renderer.getCamera().zoom = MathUtils.clamp(renderer.getCamera().zoom, 0.5f, 3f);
        return true;
    }

}
