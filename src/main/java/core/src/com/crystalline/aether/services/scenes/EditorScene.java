package com.crystalline.aether.services.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.*;
import com.crystalline.aether.services.capsules.UserInputCapsule;
import com.crystalline.aether.services.capsules.WorldCapsule;
import com.crystalline.aether.services.SpellFrame;
import com.crystalline.aether.services.ui.EmptyBlackScreen;
import com.crystalline.aether.services.ui.EtherBrushPanel;
import com.crystalline.aether.services.ui.TimeframeTable;
import com.crystalline.aether.services.utils.SkinFactory;

import java.util.ArrayList;

/**
 * A spell editor scene, starting with an initial expected mana value. The purpose of the scene is
 * to fabricate a way to spend the given amount of mana and fuse it into the environment.
 */
public class EditorScene extends Scene {
    private final Stage stage;
    private final SpriteBatch sb;
    private final Config conf;
    private WorldCapsule worldCapsule; /* a sandbox world to display the direct consequences of actions */
    private final EtherBrushPanel ebrushPanel; /* a spell panel indicator to see which actions are in focus */
    private final ShapeRenderer shapeRenderer;
    private final InputMultiplexer inputMultiplexer;

    private boolean showActions = true;
    private final ArrayList<SpellFrame> spellFrames;
    private int activeTimeFrame = 0;
    private int numberOfFrames = 10;

    public EditorScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        sb = new SpriteBatch();
        World world = new World(conf_);
        Skin skin = SkinFactory.getDefaultSkin();
        conf = conf_;
        try {
            worldCapsule = new WorldCapsule(this,conf, world);
            worldCapsule.accept_input("stop");
        } catch (Exception e) {
            e.printStackTrace();
        }

        shapeRenderer = new ShapeRenderer();
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        stage.getBatch().enableBlending();

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");
        TextButton backBtn = new TextButton("Back",textButtonStyle);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                signal("back");
                changeEvent.handle();
            }
        });

        /* TODO: 3 toggle button instead of this one */
        TextButton toggleViewBtn = new TextButton("Show Actions only", textButtonStyle);
        toggleViewBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(toggleViewBtn.getText().toString().equals("Show Actions only")){
                    toggleViewBtn.setText("Show World only");
                    setActiveUserView(0); /* Set Black Screen as active */
                    showActions = true;
                }else if(toggleViewBtn.getText().toString().equals("Show World only")){
                    toggleViewBtn.setText("Show World and Actions");
                    setActiveUserView(1); /* set world as user View */
                    showActions = false;
                }else if(toggleViewBtn.getText().toString().equals("Show World and Actions")){
                    toggleViewBtn.setText("Show Actions only");
                    setActiveUserView(1); /* set world as user View */
                    showActions = true;
                }
                event.handle();
            }
        });

        Table spellPanel = new Table();
        ebrushPanel = new EtherBrushPanel(this, skin, conf.maxMana);
        spellPanel.setBackground(skin.getDrawable("spellbar_panel"));

        ProgressBar.ProgressBarStyle pbarstyle = new ProgressBar.ProgressBarStyle();
        pbarstyle.background = skin.getDrawable("progress-bar-vertical");
        pbarstyle.knob = skin.getDrawable("progress-bar-knob-vertical");
        pbarstyle.knobBefore = skin.getDrawable("progress-bar-knob-vertical");
        ProgressBar manaBar = new ProgressBar(0, conf.maxMana,0.1f, true, pbarstyle);
        manaBar.setAnimateDuration(0.25f);
        manaBar.setValue(5);
        manaBar.setProgrammaticChangeEvents(false);

        spellPanel.add(manaBar);
        spellPanel.add(ebrushPanel.getContainer()).row();

        final TimeframeTable timeframeTable = new TimeframeTable(worldCapsule,10,skin,conf);
        timeframeTable.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                activeTimeFrame = timeframeTable.getSelectedIndex();
                signal("setTimeFrame", activeTimeFrame);
                setActiveUserView(2 + activeTimeFrame);
            }
        });

        ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
        imageButtonStyle.up = skin.getDrawable("button");
        imageButtonStyle.down = skin.getDrawable("button-pressed");
        ImageButton applyBtn = new ImageButton(imageButtonStyle);
        applyBtn.add( new Image(skin.getDrawable("check")));
        applyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                worldCapsule.reset();
                for(int i = 0; i < numberOfFrames; ++i){
                    /* apply the corresponding actions *//* TODO: real time apply so the world is in sync with the equalize attempt */
                    worldCapsule.doActions(spellFrames.get(i).getActions());
                    /* and then step */
                    worldCapsule.accept_input("step");
                    timeframeTable.getFrame(i).setFrame(worldCapsule.get_display());
                }
                event.handle();
            }
        });


        ImageButton cancelBtn = new ImageButton(imageButtonStyle);
        cancelBtn.add(new Image(skin.getDrawable("icon-trash")));
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                worldCapsule.reset();
                for(int i = 0; i < numberOfFrames; ++i){
                    timeframeTable.getFrame(i).setFrame(worldCapsule.get_display());
                    spellFrames.get(i).clearActions();
                }
                event.handle();
            }
        });

        Table outerTable = new Table();
        outerTable.setFillParent(true);
        outerTable.align(Align.topLeft);

        outerTable.add(backBtn).align(Align.topLeft);
        outerTable.add(toggleViewBtn).align(Align.topLeft).expand().row();
        outerTable.add(spellPanel).left().row();
        outerTable.add(cancelBtn, applyBtn,timeframeTable).align(Align.bottom | Align.center).row();

        stage.addActor(outerTable);

        addViews(new EmptyBlackScreen(), worldCapsule);
        spellFrames = new ArrayList<>();
        for(int i = 0; i < numberOfFrames; ++i){
            addViews(timeframeTable.getFrame(i));
            spellFrames.add(new SpellFrame(this, conf, i));
        }
        setActiveUserView(0); /* TODO: select views by name */
        UserInputCapsule userInputCapsule = new UserInputCapsule(this);
        addInputHandlers(userInputCapsule);
        addCapsules(worldCapsule, ebrushPanel);
        addCapsules(spellFrames.toArray(new SpellFrame[]{}));
        inputMultiplexer = new InputMultiplexer(stage, userInputCapsule);
        signal("doActionsOff");
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
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.LIME);
        float lineWidth = 5.0f;
        for(float x = 0; x<conf.world_block_number[0]; x+=1.0){
            float xPos = (stage.getWidth()/conf.world_block_number[0]) * x;
            shapeRenderer.rect(xPos - lineWidth/2.0f,0,lineWidth,conf.world_size[1]);
        }
        for(float y = 0; y<conf.world_block_number[1]; y+=1.0){
            float yPos = (stage.getHeight()/conf.world_block_number[0]) * y;
            shapeRenderer.rect(0,yPos - lineWidth/2.0f,conf.world_size[0],lineWidth);
        }
        shapeRenderer.end();
        if(showActions){
            sb.enableBlending();
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_DST_ALPHA);
            sb.setProjectionMatrix(stage.getCamera().combined);
            sb.begin();
            sb.draw(spellFrames.get(activeTimeFrame).get_display(), 0,0,stage.getWidth(), stage.getHeight());
            sb.end();
        }
        stage.draw();
    }
}
