package com.crystalline.aether.models;

import com.crystalline.aether.services.World;

public abstract class Reality_aspect {
    protected Config conf;
    public Reality_aspect(Config conf_){
        conf = conf_;
    }
    public abstract void determine_units(float[][] units, World parent);
    public abstract void processUnits(float[][] units, World parent);
    public abstract void processTypes(float[][] units, World parent);
    public abstract void processMechanics(float[][] units, World parent);
    public abstract void postProcess(float[][] units, World parent);
    public abstract void switch_values(int fromX, int fromY, int toX, int toY);
    public abstract void take_over_unit_changes(int x, int y, float[][] units);
}
