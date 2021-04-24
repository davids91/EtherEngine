package com.crystalline.aether.services.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.crystalline.aether.services.architecture.DisplayService;

public class Timeframe extends Stack implements DisplayService<Image> {
    private final int border_width = 2;
    private final Image insideImage;
    private final Image borderImage;
    private final Image borderImage_hover;
    private final Image borderImage_selected;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private TextureRegion content;
    private final int index;
    private final TimeframeTable parent;
    private boolean selected;

    public Timeframe(TimeframeTable parent_, int index_, TextureRegion txr, SpriteBatch batch_){
        super();
        selected = false;
        parent = parent_;
        index = index_;
        batch = batch_;
        insideImage = new Image(txr);
        content = txr;
        Pixmap border_px = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        border_px.setColor(Color.BLACK);
        border_px.fill();
        borderImage = new Image(
            new TextureRegion(
                new Texture(border_px),
                (int)insideImage.getWidth() + border_width * 2,
                (int)insideImage.getHeight() + border_width * 2
            )
        );

        border_px.setColor(Color.YELLOW);
        border_px.fill();
        borderImage_hover = new Image(
            new TextureRegion(
                new Texture(border_px),
                (int)insideImage.getWidth() + (border_width * 2),
                (int)insideImage.getHeight() + (border_width * 2)
            )
        );
        borderImage_hover.setVisible(false);

        border_px.setColor(Color.LIME);
        border_px.fill();
        borderImage_selected = new Image(
                new TextureRegion(
                        new Texture(border_px),
                        (int)insideImage.getWidth() + (border_width * 2),
                        (int)insideImage.getHeight() + (border_width * 2)
                )
        );
        borderImage_selected.setVisible(false);
        border_px.dispose();


        borderImage.setZIndex(0);
        borderImage_hover.setZIndex(1);
        borderImage_selected.setZIndex(2);
        insideImage.setZIndex(3);

        addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                parent.setSelected(index);
                event.handle();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                borderImage.setVisible(false);
                borderImage_hover.setVisible(true);
                event.handle();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                borderImage.setVisible(true);
                borderImage_hover.setVisible(false);
                event.handle();
            }
        });

        add(borderImage);
        add(borderImage_hover);
        add(borderImage_selected);
        add(insideImage);

        /* set up rendering */
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
    }

    public void select(){
        borderImage_selected.setVisible(true);
    }

    public void unselect(){
        borderImage_selected.setVisible(false);
    }

    public void setFrame(Texture txr){
        insideImage.setDrawable(new SpriteDrawable(new Sprite(txr)));
        content = new TextureRegion(txr);
    }

    @Override
    public void layout() {
        super.layout();
        borderImage.setSize(
        (int)insideImage.getWidth() + border_width * 2,
        (int)insideImage.getHeight() + border_width * 2
        );
        borderImage.setPosition(
        (int)insideImage.getX() - border_width,
        (int)insideImage.getY() - border_width
        );

        borderImage_hover.setSize(
        (int)insideImage.getWidth() + border_width * 2,
        (int)insideImage.getHeight() + border_width * 2
        );
        borderImage_hover.setPosition(
        (int)insideImage.getX() - border_width,
        (int)insideImage.getY() - border_width
        );

        borderImage_selected.setSize(
        (int)insideImage.getWidth() + border_width * 2,
        (int)insideImage.getHeight() + border_width * 2
        );
        borderImage_selected.setPosition(
        (int)insideImage.getX() - border_width,
        (int)insideImage.getY() - border_width
        );
    }

    @Override
    public Image get_display() {
        return null;
    }

    @Override
    public void resize(int width, int height) {
        /* Nothing needed here yet.. */
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(content,0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void dispose() {

    }
}
