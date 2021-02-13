package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.crystalline.aether.models.Config;

public class PlaygroundScene extends Scene {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final Stage stage;

    public PlaygroundScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        conf = conf_;
        worldCapsule = new WorldCapsule(conf);
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        TextureAtlas ui_atlas = new TextureAtlas(Gdx.files.internal("skins/default/neutralizer-ui.atlas"));
        BitmapFont font = new BitmapFont(Gdx.files.internal("skins/default/font-export.fnt"), ui_atlas.findRegion("font-export"));
        Skin skin = new Skin();
        skin.addRegions(ui_atlas);

        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.topLeft);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");

        TextButton spell_editor_btn = new TextButton("Spell Editor",textButtonStyle);
        spell_editor_btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
            signal("open_editor");
            }
        });

        table.add(spell_editor_btn).align(Align.topLeft).row();
        table.setDebug(true);
        stage.addActor(table);

        WorldDisplay worldDisplayCapsule = new WorldDisplay(worldCapsule, conf);
        UserInputCapsule userInputCapsule = new UserInputCapsule(worldCapsule, worldDisplayCapsule, conf);
        addViews(worldDisplayCapsule);
        setActiveUserView(0);
        addInputHandlers(userInputCapsule);
        setActiveInputHandler(0);
    }

    @Override
    public InputProcessor getInputProcessor(){
        return stage;
    }

    @Override
    public void calculate() {
        stage.act(Gdx.graphics.getDeltaTime());
        worldCapsule.calculate();
    }

    @Override
    public void render() {
        super.render();
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
        stage.dispose();
    }
}
