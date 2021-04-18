package com.crystalline.aether.services.architecture;

import com.crystalline.aether.services.scenes.Scene;

public abstract class InputService{

    public abstract void handleInput();
    public abstract void dispose();

    private final Scene parent;
    public InputService(Scene parent_){
        parent = parent_;
    }
    protected void signal(String signal, Float... parameters){
        parent.signal(signal,parameters);
    }
}
