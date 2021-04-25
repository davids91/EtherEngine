package com.crystalline.aether.models.architecture;

public abstract class InputService{

    public abstract void handleInput();
    public abstract void dispose();

    private final Scene parent;
    public InputService(Scene parent_){
        parent = parent_;
    }
    protected void signal(String signal, Object... parameters){
        parent.signal(signal,parameters);
    }
}
