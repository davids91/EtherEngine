package com.crystalline.aether.services.architecture;

import com.crystalline.aether.services.scenes.Scene;

public abstract class InputService{

    public abstract void handleInput();
    public abstract void dispose();

    Scene parent;
    public InputService(Scene parent_){
        parent = parent_;
    }
    protected void signal(String signal, float... parameters){
        parent.signal(signal,parameters);
    }
}
