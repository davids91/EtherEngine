package com.crystalline.aether.models.architecture;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.world.World;

import java.util.Stack;

public abstract class RealityAspect {
    public abstract void determine_units(float[][] units, World parent);
    public abstract void processUnits(float[][] units, World parent);
    public abstract void processTypes(float[][] units, World parent);
    public abstract void processMechanics(float[][] units, World parent);
    public abstract void postProcess(float[][] units, World parent);
    public abstract void switch_values(int fromX, int fromY, int toX, int toY);
    public abstract void take_over_unit_changes(int x, int y, float[][] units);
    protected abstract Object[] getState();
    protected abstract void setState(Object[] state);

    protected Config conf;
    private Stack<Object[]> state;
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
