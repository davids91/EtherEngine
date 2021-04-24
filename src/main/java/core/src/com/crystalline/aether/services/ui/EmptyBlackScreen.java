package com.crystalline.aether.services.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.crystalline.aether.services.architecture.DisplayService;

public class EmptyBlackScreen implements DisplayService<Texture> {
    @Override
    public Texture get_display() {
        Pixmap emptyScreen = new Pixmap(Gdx.graphics.getWidth(),Gdx.graphics.getWidth(), Pixmap.Format.RGB888);
        emptyScreen.setColor(Color.BLACK);
        emptyScreen.fill();
        Texture tex = new Texture(emptyScreen);
        emptyScreen.dispose();
        return tex;
    }

    @Override
    public void resize(int width, int height) {
        /* Nothing needed here yet.. */
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void dispose() {

    }
}
