package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.crystalline.aether.models.Config;

public class EditorScene extends Scene{
    private final Stage stage;
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final EtherBrushPanel ebrushPanel;
    private final ShapeRenderer shapeRenderer;
    private final InputMultiplexer inputMultiplexer;

    private final float max_spell_amount = 100.0f;

    public EditorScene(SceneHandler.Builder builder, Config conf_){
        super(builder);
        conf = conf_;
        worldCapsule = new WorldCapsule(conf);
        inputMultiplexer = new InputMultiplexer();
        WorldDisplay world_display = new WorldDisplay(worldCapsule, conf);
        UserInputCapsule inputCapsule = new UserInputCapsule(this,worldCapsule, world_display,max_spell_amount, conf);
        addViews(world_display);
        setActiveUserView(0);
        inputMultiplexer.addProcessor(inputCapsule);

        shapeRenderer = new ShapeRenderer();
        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        inputMultiplexer.addProcessor(stage);

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
            }
        });

        Table spellPanel = new Table();
        ebrushPanel = new EtherBrushPanel(skin,max_spell_amount);
        spellPanel.setDebug(true);
        spellPanel.setBackground(skin.getDrawable("spellbar_panel"));
        spellPanel.add(ebrushPanel).row();

        Table outer_table = new Table();
        outer_table.setFillParent(true);
        outer_table.align(Align.topLeft);

        outer_table.add(back_btn).align(Align.topLeft).row();
        outer_table.add(spellPanel).left();

        stage.addActor(outer_table);

        addCapsules(worldCapsule, ebrushPanel);
        addInputHandlers(inputCapsule);
    }

    private void drawGrid(float lineWidth, float cellSize) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.LIME);
        for(float x = cellSize;x<conf.world_size[0];x+=cellSize){
            shapeRenderer.rect(x,0,lineWidth,conf.world_size[1]);
        }
        for(float y = cellSize;y<conf.world_size[0];y+=cellSize){
            shapeRenderer.rect(0,y,conf.world_size[0],lineWidth);
        }
        shapeRenderer.end();
    }


    @Override
    public void calculate() {
        super.calculate();
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
        drawGrid(5.0f,conf.world_block_size);
        stage.draw();
    }
}
