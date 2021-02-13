package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.crystalline.aether.models.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.DisplayService;

public class WorldDisplay implements CapsuleService, DisplayService<Texture> {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;

    public WorldDisplay(WorldCapsule worldCapsule_, Config conf_){
        worldCapsule = worldCapsule_;
        batch = new SpriteBatch();

        conf = conf_;
        camera = new OrthographicCamera();
        camera.setToOrtho(true,worldCapsule.width(), worldCapsule.height());
        camera.update();
    }

    @Override
    public void calculate() {

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(new TextureRegion(get_display()),0,0,worldCapsule.width(), worldCapsule.height());
        batch.end();
    }

    @Override
    public void accept_input(String name, float... parameters) {

    }

    @Override
    public float get_parameter(String name, int index) {
        return 0;
    }

    @Override
    public Object get_object(String name) {
        if(name.equals("camera")){
            return camera;
        }
        return null;
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    @Override
    public Texture get_display() {
        return (Texture)worldCapsule.get_object("world_image");
    }
}
