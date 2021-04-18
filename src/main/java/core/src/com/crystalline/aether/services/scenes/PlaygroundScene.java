package com.crystalline.aether.services.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.SceneHandler;
import com.crystalline.aether.services.capsules.UserInputCapsule;
import com.crystalline.aether.services.capsules.WorldCapsule;
import com.crystalline.aether.services.ui.EtherBrushPanel;

public class PlaygroundScene extends Scene {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final Stage stage;
    private final EtherBrushPanel etherBrushPanel;
    private final InputMultiplexer inputMultiplexer;

    public PlaygroundScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        inputMultiplexer = new InputMultiplexer();
        conf = conf_;
        worldCapsule = new WorldCapsule(this,conf);
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        TextureAtlas ui_atlas = new TextureAtlas(Gdx.files.internal("skins/default/neutralizer-ui.atlas"));
        TextureAtlas nether_atlas = new TextureAtlas(Gdx.files.internal("atlases/Nether.atlas"));
        BitmapFont font = new BitmapFont(Gdx.files.internal("skins/default/font-export.fnt"), ui_atlas.findRegion("font-export"));
        Skin skin = new Skin();
        skin.add("default-font",font,BitmapFont.class);
        skin.addRegions(nether_atlas);
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
        etherBrushPanel = new EtherBrushPanel(this, skin,100);

        table.add(spell_editor_btn).align(Align.topLeft).row();
        table.add(etherBrushPanel.getContainer()).align(Align.topLeft);
        stage.addActor(table);

        UserInputCapsule userInputCapsule = new UserInputCapsule(this, conf);
        addViews(worldCapsule);
        setActiveUserView(0);
        addInputHandlers(userInputCapsule);
        addCapsules(worldCapsule, etherBrushPanel);
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(userInputCapsule);
    }

    @Override
    public InputProcessor getInputProcessor(){
        return inputMultiplexer;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width,height,true);
    }

    @Override
    public void calculate() {
        super.calculate();
        etherBrushPanel.calculate();
        stage.act(Gdx.graphics.getDeltaTime());
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
