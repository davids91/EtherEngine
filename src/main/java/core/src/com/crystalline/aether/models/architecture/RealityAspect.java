package com.crystalline.aether.models.architecture;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;
import java.util.Stack;

public abstract class RealityAspect {
    public abstract void determineUnits(World parent);
    public abstract void processUnits(World parent);
    public abstract void processTypes(World parent);
    public abstract void processMechanics(World parent);
    public abstract void postProcess(World parent);
    public abstract void switchValues(int fromX, int fromY, int toX, int toY);
    public abstract void takeOverUnitChanges(int x, int y, World parent);
    protected abstract Object[] getState();
    protected abstract void setState(Object[] state);

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
