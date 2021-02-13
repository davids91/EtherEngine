package com.crystalline.aether.models;

import com.badlogic.gdx.InputProcessor;
import com.crystalline.aether.services.Scene;

public abstract class InputService implements InputProcessor {

    public abstract void handle_input();
    public abstract void dispose();


    Scene parent;
    public InputService(Scene parent_){
        parent = parent_;
    }

    protected void signal(String signal, float... parameters){
        parent.signal(signal,parameters);
    }
}
