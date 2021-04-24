package com.crystalline.aether.services.architecture;

import com.crystalline.aether.services.scenes.Scene;

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
