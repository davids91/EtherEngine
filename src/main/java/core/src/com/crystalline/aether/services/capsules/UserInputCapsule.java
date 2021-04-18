package com.crystalline.aether.services.capsules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.architecture.InputService;
import com.crystalline.aether.services.scenes.Scene;

public class UserInputCapsule extends InputService implements InputProcessor {
    private boolean touchIsDown = false;
    public UserInputCapsule(Scene parent, Config conf_){
        super(parent);
    }

    @Override
    public boolean keyDown(int i) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
            signal("initialize");
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            signal("playPause");
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchIsDown = true;
        if(Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            signal("netherActive");
        if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
            signal("aetherActive");
        if(Input.Buttons.FORWARD == button){
            signal("upTendency");
        }else if(Input.Buttons.BACK == button){
            signal("downTendency");
        }
        signal("mouseOnScreen2D", screenX,screenY);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        touchIsDown = false;
        if(!Gdx.input.isButtonPressed(Input.Buttons.LEFT))
            signal("netherInactive");
        if(!Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
            signal("aetherInactive");
        signal("mouseOnScreen2D", screenX,screenY);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        signal("mouseOnScreen2D", screenX,screenY);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        signal("mouseOnScreen2D", screenX,screenY);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        signal("manaModif",amountY);
        return false;
    }

    @Override
    public void handleInput() {

    }

    @Override
    public void dispose() {

    }
}
