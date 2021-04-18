package com.crystalline.aether.services.capsules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.services.architecture.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.architecture.DisplayService;
import com.crystalline.aether.services.World;

/** TODO:
 * - Create Different Views for the world as different DisplayServices
 * - Display heat and light
 * - Heat and cold to be counted in para-effect plane( ? )
 * - Create Ether crystals: it's not a target ratio, only when the ratio is already at that.
 * - "running" indicator
 * - debug panel to show pixel in focus
 * - Debug NANs
 * - push a to b  ( material merges )
 * - typechange conflicts --> especially handle water to disappear when lava is near
 * - "Move together" ?
 */

/**
 * Handles the creation and concatenation of possibly multiple worlds.
 */
public class WorldCapsule implements CapsuleService, DisplayService<Texture> {
    private final Config conf;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private Vector3 mouseInWorld;
    private final World world;

    private boolean play = true;
    private boolean aetherActive = false;
    private boolean netherActive = false;
    private float manaToUse = 0;

    public WorldCapsule(Config conf_){
        conf = conf_;
        world = new World(conf);
        mouseInWorld = new Vector3();
        camera.setToOrtho(false, width(), height());
        camera.update();
    }

    @Override
    public void calculate() {
        if(play){
            if(aetherActive){
                System.out.println("adding "+manaToUse+" aether to ["+mouseInWorld.x+","+mouseInWorld.y+"]");
                world.addAetherTo((int)mouseInWorld.x,(int)mouseInWorld.y, manaToUse);
            }
            if(netherActive){
                world.addNetherTo((int)mouseInWorld.x,(int)mouseInWorld.y, manaToUse);
            }
//                world.try_to_equalize((int)mouseInWorld.x,(int)mouseInWorld.y, parameters[0]);
            world.main_loop(0.01f);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(new TextureRegion(get_display()),0,0, width(), height());
        batch.end();
    }

    @Override
    public void accept_input(String name, float... parameters) {
        /* Accept User input */
        if(name.equals("mouseOnScreen2D")&&(2 == parameters.length)){
            mouseInWorld = camera.unproject(new Vector3(parameters[0], parameters[1], 0.0f));
        }else if(name.equals("netherActive")){
            netherActive = true;
        }else if(name.equals("netherInactive")){
            netherActive = false;
        }else if(name.equals("aetherActive")){
            aetherActive = true;
        }else if(name.equals("aetherInactive")){
            aetherActive = false;
        }else if(name.equals("manaToUse")&&(1 == parameters.length)){
            manaToUse = parameters[0];
        }
        /* Modify parameters */
        else if(name.equals("initialize")&&(0 == parameters.length)){
            world.pond_with_grill();
        }else if(name.equals("stop")&&(0 == parameters.length)){
            play = false;
        }else if(name.equals("playPause")&&(0 == parameters.length)){
            play = !play;
        }else if(name.equals("step")&&(0 == parameters.length)){
            world.main_loop(0.01f); /* TODO: Make this part of the "calculate"  */
        }
    }

    public float width(){
        return conf.world_block_number[0];
    }
    public float height(){
        return conf.world_block_number[1];
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    @Override
    public Texture get_display() {
        Pixmap pxm = world.getWorldImage(new Vector2(mouseInWorld.x,mouseInWorld.y),world.get_eth_plane());
        Texture result = new Texture(pxm);
        pxm.dispose();
        return result;
    }

}
