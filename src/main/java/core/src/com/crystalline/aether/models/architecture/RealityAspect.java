package com.crystalline.aether.models.architecture;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;
import java.util.Stack;

public abstract class RealityAspect {
    protected abstract Object[] getState();
    protected abstract void setState(Object[] state);
    public abstract void switchValues(FloatBuffer proposals);
    public abstract FloatBuffer determineUnits(World parent);

    /* Processing functions to be called by World in its main loop */
    public abstract void processUnits(World parent);
    public abstract void processTypes(World parent);
    public abstract void processMechanics(World parent);
    public abstract void postProcess(World parent);

    protected Config conf;
    private final Stack<Object[]> state;
    public RealityAspect(Config conf_){
        conf = conf_;
        state = new Stack<>();
    }
    public void pushState(){
        state.push(getState());
    }
    public void popState(){
        setState(state.pop());
    }

}
