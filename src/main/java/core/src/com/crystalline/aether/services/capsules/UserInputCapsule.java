package com.crystalline.aether.services.capsules;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.crystalline.aether.models.architecture.InputService;
import com.crystalline.aether.models.architecture.Scene;

public class UserInputCapsule extends InputService implements InputProcessor {
    boolean leftMouseBtnIsUp = true;
    boolean rightMouseBtnIsUp = true;

    public UserInputCapsule(Scene parent){
        super(parent);
    }

    @Override
    public boolean keyDown(int keycode) {
        if(Input.Keys.BACKSPACE == keycode){
            signal("initialize");
        }
        if(Input.Keys.ENTER == keycode){
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
        if(Input.Buttons.LEFT == button){
            leftMouseBtnIsUp = false;
            signal("netherActive");
        }
        if(Input.Buttons.RIGHT == button){
            rightMouseBtnIsUp = false;
            signal("aetherActive");
        }
        if(Input.Buttons.FORWARD == button){
            signal("upTendency");
        }else if(Input.Buttons.BACK == button){
            signal("downTendency");
        }
        signal("mouseOnScreen2D", (float)screenX, (float)screenY);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if(Input.Buttons.LEFT == button) {
            leftMouseBtnIsUp = true;
            signal("netherInactive");
        }
        if(Input.Buttons.RIGHT == button) {
            rightMouseBtnIsUp = true;
            signal("aetherInactive");
        }
        signal("mouseOnScreen2D", (float)screenX,(float)screenY);
        if(leftMouseBtnIsUp && rightMouseBtnIsUp){
            signal("interactionOver");
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        signal("mouseOnScreen2D", (float)screenX,(float)screenY);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        signal("mouseOnScreen2D", (float)screenX,(float)screenY);
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
