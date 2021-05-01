package com.crystalline.aether.models.architecture;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.world.World;

import java.util.Stack;

public abstract class RealityAspect {
    public abstract void determineUnits(int[][] units, World parent);
    public abstract void processUnits(int[][] units, World parent);
    public abstract void processTypes(int[][] units, World parent);
    public abstract void processMechanics(int[][] units, World parent);
    public abstract void postProcess(int[][] units, World parent);
    public abstract void switchValues(int fromX, int fromY, int toX, int toY);
    public abstract void takeOverUnitChanges(int x, int y, int[][] units);
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
