package io.github.dungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.dungeon.Main;

public class VictoryScreen implements Screen {
    private final Stage stage;
    private final BitmapFont font;

    public VictoryScreen() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        font = fontGenerator();
    }

    private BitmapFont fontGenerator() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/Jacquard12-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 180;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    @Override
    public void show() {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.GOLD;
        Label title = new Label("Escaped", labelStyle);
        title.setPosition(
            Gdx.graphics.getWidth() / 2f - title.getWidth() / 2f,
            Gdx.graphics.getHeight() / 2f - title.getHeight() / 2f
        );
        title.getColor().a = 0f;
        title.addAction(Actions.fadeIn(2f));
        stage.addAction(Actions.sequence(
            Actions.delay(3f),
            Actions.fadeOut(2f),
            Actions.run(() -> {
                int currentLevel = Main.getInstance().getLevel();
                if (currentLevel < 5) {
                    Main.getInstance().setLevel(currentLevel + 1);
                    Main.getInstance().setScreen(new DungeonScreen(Main.getInstance()));
                } else {
                    Main.getInstance().setLevel(1);
                    Main.getInstance().setScreen(new MenuScreen());
                }
            })
        ));
        stage.addActor(title);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }


    @Override
    public void resize(int i, int i1) {

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
        stage.dispose();
        font.dispose();
    }
}
