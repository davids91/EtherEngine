package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.CapsuleService;
import com.crystalline.aether.models.Config;

/** TODO:
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
public class WorldCapsule implements CapsuleService {
    private final Config conf;
    private final World world;
    private final Vector2 mouseInWorld2D = new Vector2();

    private boolean play = true;
    private float spell_amount = 0.0f;

    public WorldCapsule(Config conf_){
        conf = conf_;
        world = new World(conf);
    }

    @Override
    public void calculate() {
        if(play){
            world.main_loop(0.01f);
        }else
        if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
            world.main_loop(0.01f);
        }
    }

    @Override
    public void accept_input(String name, float... parameters) {
        /* Modify parameters */
        if(name.equals("mouseInWorld2D")&&(2 == parameters.length)){
            mouseInWorld2D.x = parameters[0];
            mouseInWorld2D.y = parameters[1];
        }else if(name.equals("initialize")&&(0 == parameters.length)){
            world.pond_with_grill();
        }else if(name.equals("play_pause")&&(0 == parameters.length)){
            play = !play;
        }

        /* interact */
        if(name.equals("spell_amount")&&(1==parameters.length)){
            spell_amount = parameters[0];
        }else if(name.equals("add_aether")&&(1 == parameters.length)){
            world.add_aether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, parameters[0]);
        }else if(name.equals("add_nether")&&(1 == parameters.length)){
            world.add_nether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, parameters[0]);
        }else if(name.equals("equalize")&&(1 == parameters.length)){
            world.try_to_equalize((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, parameters[0]);
        }
    }

    @Override
    public float get_parameter(String name, int index) {
        throw new UnsupportedOperationException("No parameters available!");
    }

    @Override
    public Object get_object(String name) {
        if(name.equals("world_image")){
            return new Texture(world.getWorldImage(mouseInWorld2D,(spell_amount /10.0f),world.get_eth_plane()));
        }
        throw new UnsupportedOperationException("No Objects with given name!");
    }

    public float width(){
        return conf.world_block_number[0];
    }
    public float height(){
        return conf.world_block_number[1];
    }

    @Override
    public void dispose() {

    }

}
