package com.crystalline.aether.models.architecture;

public abstract class CapsuleService {

    private final Scene parent;
    public CapsuleService(Scene parent_){
        parent = parent_;
    }
    protected void signal(String signal, Object... parameters){
        parent.signal(signal,parameters);
    }
    public abstract void calculate();
    public abstract void accept_input(String name, Object... parameters);
    public abstract void dispose();
}
