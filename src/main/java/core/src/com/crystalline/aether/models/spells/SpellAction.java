package com.crystalline.aether.models.spells;

import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.world.Material;

public class SpellAction {
    public float usedAether = 0, usedNether = 0;
    public Material.Elements targetElement = Material.Elements.Nothing;
    public Vector3 pos = new Vector3();

    public SpellAction(){ }
    public SpellAction(SpellAction o){
        usedAether = o.usedAether;
        usedNether = o.usedNether;
        targetElement = o.targetElement;
        pos = new Vector3(o.pos);
    }

    public void clear(){
        usedAether = 0;
        usedNether = 0;
        targetElement = Material.Elements.Nothing;
    }

    public boolean active(){
        return ((0 <Math.abs(usedNether))||(0<Math.abs(usedAether)));
    }
    public boolean aetherActive(){
        return (0 < Math.abs(usedAether));
    }

    public boolean netherActive(){
        return (0 < Math.abs(usedNether));
    }

    public void acceptInput(String name, Float... parameters){

    }
}
