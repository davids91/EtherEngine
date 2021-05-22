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
import com.crystalline.aether.models.architecture.Scene;
import com.crystalline.aether.services.capsules.UserInputCapsule;
import com.crystalline.aether.services.capsules.WorldCapsule;
import com.crystalline.aether.services.spells.SpellBuilder;
import com.crystalline.aether.services.ui.EmptyBlackScreen;
import com.crystalline.aether.services.ui.EtherBrushPanel;
import com.crystalline.aether.services.ui.SpellFrameView;
import com.crystalline.aether.services.ui.TimeframeTable;
import com.crystalline.aether.services.utils.SkinFactory;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.world.World;

import java.util.ArrayList;

/**
 * A spell editor scene, starting with an initial expected mana value. The purpose of the scene is
 * to fabricate a way to spend the given amount of mana and fuse it into the environment.
 */
public class EditorScene extends Scene {
    private final Stage stage;
    private final SpriteBatch sb;
    private final Config conf;
    private final World world;
    private WorldCapsule worldCapsule; /* a sandbox world to display the direct consequences of actions */
    private final SpellBuilder spellBuilder;
    private final ArrayList<SpellFrameView> spellFrameViews;
    private final TimeframeTable timeframeTable;
    private final EtherBrushPanel ebrushPanel; /* a spell panel indicator to see which actions are in focus */
    private final ShapeRenderer shapeRenderer;
    private final InputMultiplexer inputMultiplexer;

    private boolean showActions = true;
    private int numberOfFrames = 10;
    private int activeTimeFrame = 0;

    @Override
    public String getName() {
        return "editor";
    }

    public EditorScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        sb = new SpriteBatch();
        world = new World(conf_);
        Skin skin = SkinFactory.getDefaultSkin();
        conf = conf_;
        try {
            worldCapsule = new WorldCapsule(this,conf, world);
            worldCapsule.setPlay(false);
            worldCapsule.setDoActions(false);
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
            if(0 < spellBuilder.getCurrentSpell().getFrames().size()) {
                signal("back", spellBuilder.getCurrentSpell());
            }else signal("back");
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
        ProgressBar availableManaInFrame = new ProgressBar(0, conf.maxMana,0.1f, true, pbarstyle);
        availableManaInFrame.setAnimateDuration(0.25f);
        availableManaInFrame.setValue(5);
        availableManaInFrame.setProgrammaticChangeEvents(false);

//        spellPanel.add(availableManaInFrame); /*TODO: Maximize the amount of mana to be moved in a frame */
        spellPanel.add(ebrushPanel.getContainer()).row();

        spellBuilder = new SpellBuilder(this, conf, conf.WORLD_DIMENSIONS);
        /*!Note: The editor is a full world of a relative small size */
        timeframeTable = new TimeframeTable(worldCapsule,numberOfFrames,skin,conf);
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
        ImageButton cancelBtn = new ImageButton(imageButtonStyle);
        cancelBtn.add(new Image(skin.getDrawable("icon-trash")));
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
            worldCapsule.reset();
            for(int i = 0; i < numberOfFrames; ++i){
                timeframeTable.getFrame(i).setFrame(worldCapsule.getDisplay());
                spellBuilder.getCurrentSpell().getFrame(i).clearActions();
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
        outerTable.add(cancelBtn, timeframeTable).align(Align.bottom | Align.center).row();

        stage.addActor(outerTable);

        addViews(new EmptyBlackScreen(), worldCapsule);
        spellFrameViews = new ArrayList<>();
        spellBuilder.setFrameNumber(numberOfFrames);
        for(int i = 0; i < numberOfFrames; ++i){ /* TODO: Adapt to be able to handle multiple different spells */
            spellFrameViews.add(new SpellFrameView(conf, spellBuilder.getCurrentSpell().getFrame(i)));
            addViews(timeframeTable.getFrame(i));
        }
        setActiveUserView(0); /* TODO: select views by name */
        UserInputCapsule userInputCapsule = new UserInputCapsule(this);
        addInputHandlers(userInputCapsule);
        addCapsules(worldCapsule, ebrushPanel);
        addCapsules(spellBuilder);
        inputMultiplexer = new InputMultiplexer(stage, userInputCapsule);
    }

    @Override
    public void signal(String name, Object... parameters) {
        super.signal(name, parameters);
        if(name.equals("interactionOver")&&(0 == parameters.length)){
            applySpell();
        }
    }

    private void applySpell(){
        worldCapsule.reset();
        worldCapsule.setBroadcastActions(false);
        for(int i = 0; i < numberOfFrames; ++i){
            if(activeTimeFrame == i)world.pushState();
            worldCapsule.doActions(MiscUtils.zeroVec, spellBuilder.getCurrentSpell().getFrame(i).getActions());
            worldCapsule.step();
            timeframeTable.getFrame(i).setFrame(worldCapsule.getDisplay());
        }
        world.popState();
        worldCapsule.setBroadcastActions(true);
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
        for(float x = 0; x<conf.WORLD_BLOCK_NUMBER[0]; x+=1.0){
            float xPos = (stage.getWidth()/conf.WORLD_BLOCK_NUMBER[0]) * x;
            shapeRenderer.rect(xPos - lineWidth/2.0f,0,lineWidth,conf.WORLD_SIZE[1]);
        }
        for(float y = 0; y<conf.WORLD_BLOCK_NUMBER[1]; y+=1.0){
            float yPos = (stage.getHeight()/conf.WORLD_BLOCK_NUMBER[0]) * y;
            shapeRenderer.rect(0,yPos - lineWidth/2.0f,conf.WORLD_SIZE[0],lineWidth);
        }
        shapeRenderer.end();
        if(showActions){
            sb.enableBlending();
            sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_DST_ALPHA);
            sb.setProjectionMatrix(stage.getCamera().combined);
            sb.begin();
            sb.draw(spellFrameViews.get(activeTimeFrame).getDisplay(), 0,0,stage.getWidth(), stage.getHeight());
            sb.end();
        }
        stage.draw();
    }
}
