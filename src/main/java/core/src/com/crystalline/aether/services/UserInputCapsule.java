package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Materials;

public class UserInputCapsule implements CapsuleService {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final WorldDisplay displayCapsule;
    private final Vector3 mouseInCam3D = new Vector3();

    public UserInputCapsule(WorldCapsule worldCapsule_, WorldDisplay displayCapsule_, Config conf_){
        conf = conf_;
        worldCapsule = worldCapsule_;
        displayCapsule = displayCapsule_;
    }

    @Override
    public void calculate() {
        mouseInCam3D.x = Gdx.input.getX();
        mouseInCam3D.y = Gdx.graphics.getHeight() - Gdx.input.getY();
        mouseInCam3D.z = 0;
        ((OrthographicCamera)displayCapsule.get_object("camera")).unproject(mouseInCam3D);

        worldCapsule.accept_input("mouseInWorld2D",
        ((mouseInCam3D.x - (conf.block_radius) + (conf.world_block_size / 4.0f)) / conf.world_block_size),
        ((mouseInCam3D.y - (conf.block_radius) + (conf.world_block_size / 4.0f)) / conf.world_block_size)
        );

        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            worldCapsule.accept_input("modify_spell_range", 1.1f);
        } else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            worldCapsule.accept_input("modify_spell_range", 0.9f);
        }

        float add_this;
        if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)){
            if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
                add_this = worldCapsule.get_parameter("spell_range",0);
                if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
                worldCapsule.accept_input("equalize", add_this);
            }
        }else{
            if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
                add_this = worldCapsule.get_parameter("spell_range",0);
                if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
                worldCapsule.accept_input("add_nether", add_this);
                if(Gdx.input.isKeyPressed(Input.Keys.C))
                    worldCapsule.accept_input("add_aether", (add_this / Materials.nether_ratios[Materials.Names.Fire.ordinal()]));
            }
            if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)){
                add_this = worldCapsule.get_parameter("spell_range",0);
                if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
                worldCapsule.accept_input("add_aether", add_this);
                if(Gdx.input.isKeyPressed(Input.Keys.C))
                    worldCapsule.accept_input("add_nether", (add_this * Materials.nether_ratios[Materials.Names.Fire.ordinal()]));
            }
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
            worldCapsule.accept_input("initialize");
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            worldCapsule.accept_input("play_pause");
        }
    }

    @Override
    public void accept_input(String name, float... values) {

    }

    @Override
    public float get_parameter(String name, int index) {
        return 0;
    }

    @Override
    public Object get_object(String name) {
        return null;
    }

    @Override
    public void dispose() {

    }
}
