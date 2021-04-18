package com.crystalline.aether.services.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Spells;
import com.crystalline.aether.services.*;
import com.crystalline.aether.services.capsules.WorldCapsule;
import com.crystalline.aether.services.ui.EtherBrushPanel;
import com.crystalline.aether.services.ui.TimeframePanel;

/**
 * A spell editor scene, starting with an initial expected mana value. The purpose of the scene is
 * to fabricate a way to spend the given amount of mana and fuse it into the environment.
 */
public class EditorScene extends Scene {
    private final Stage stage;
    private final Config conf;
    private final WorldCapsule worldCapsule; /* a sandbox world to display the direct consequences of actions */
    private final EtherBrushPanel ebrushPanel; /* a spell panel indicator to see which actions are in focus */
    private final ShapeRenderer shapeRenderer;
    private final InputMultiplexer inputMultiplexer;

    private final float maxMana = 100.0f;
    private Spells.Action action_table[][][]; /* Stores Actions for every frame and possible coordinate */
    private Spells.Action spell[][]; /* Stores Actions for every frame in different coordinates */

    public EditorScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        conf = conf_;
        worldCapsule = new WorldCapsule(conf);
        worldCapsule.accept_input("stop");
        inputMultiplexer = new InputMultiplexer();

        shapeRenderer = new ShapeRenderer();
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));

        TextureAtlas ui_atlas = new TextureAtlas(Gdx.files.internal("skins/default/neutralizer-ui.atlas"));
        TextureAtlas nether_atlas = new TextureAtlas(Gdx.files.internal("atlases/Nether.atlas"));
        BitmapFont font = new BitmapFont(Gdx.files.internal("skins/default/font-export.fnt"), ui_atlas.findRegion("font-export"));

        Skin skin = new Skin();
        skin.addRegions(nether_atlas);
        skin.addRegions(ui_atlas);
        skin.add("default-font",font,BitmapFont.class);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");
        TextButton back_btn = new TextButton("Back",textButtonStyle);
        back_btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                signal("back");
                changeEvent.handle();
            }
        });

        Table spellPanel = new Table();
        ebrushPanel = new EtherBrushPanel(skin, maxMana);
        spellPanel.setBackground(skin.getDrawable("spellbar_panel"));

        ProgressBar.ProgressBarStyle pbarstyle = new ProgressBar.ProgressBarStyle();
        pbarstyle.background = skin.getDrawable("progress-bar-vertical");
        pbarstyle.knob = skin.getDrawable("progress-bar-knob-vertical");
        pbarstyle.knobBefore = skin.getDrawable("progress-bar-knob-vertical");
        ProgressBar manaBar = new ProgressBar(0, maxMana,0.1f, true, pbarstyle);
        manaBar.setAnimateDuration(0.25f);
        manaBar.setValue(5);
        manaBar.setProgrammaticChangeEvents(false);

        spellPanel.add(manaBar);
        spellPanel.add(ebrushPanel).row();

        final TimeframePanel tfp = new TimeframePanel(worldCapsule,10,skin,conf);
        tfp.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                setActiveUserView(tfp.getSelectedIndex());
            }
        });

        Table outer_table = new Table();
        outer_table.setFillParent(true);
        outer_table.align(Align.topLeft);

        outer_table.add(back_btn).align(Align.topLeft).row();
        outer_table.add(spellPanel).left().row();
        outer_table.add(tfp).align(Align.bottom | Align.center).expand().row();

        stage.addActor(outer_table);

        for(int i = 0; i < 10; ++i){
            addViews(tfp.getFrame(i));
        }
        addViews(worldCapsule);
        setActiveUserView(0);
        addCapsules(worldCapsule, ebrushPanel);
        inputMultiplexer.addProcessor(stage);
    }

    @Override
    public void calculate() {
        super.calculate();
        ebrushPanel.calculate();
        stage.act();
    }

    @Override
    public InputProcessor getInputProcessor() {
        return inputMultiplexer;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        super.render();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.LIME);
        float lineWidth = 5.0f;
        for(float x = conf.world_block_size;x<conf.world_size[0];x+=conf.world_block_size){
            shapeRenderer.rect(x - lineWidth/2.0f,0,lineWidth,conf.world_size[1]);
        }
        for(float y = conf.world_block_size;y<conf.world_size[0];y+=conf.world_block_size){
            shapeRenderer.rect(0,y - lineWidth/2.0f,conf.world_size[0],lineWidth);
        }
        shapeRenderer.end();
        stage.draw();
    }
}
