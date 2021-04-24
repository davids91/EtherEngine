package com.crystalline.aether.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public class Spell {
    public enum SpellEtherTendency{
        TAKE, EQUALIZE, GIVE;
        private static final SpellEtherTendency[] vals = values();
        public SpellEtherTendency next(){ return vals[(this.ordinal() + 1) % vals.length]; }
        public SpellEtherTendency previous(){
            if(0 == this.ordinal())
                return vals[vals.length-1];
            else return vals[this.ordinal() -1];
        }
    }

    public static class Action{
        public float usedAether = 0.0f, usedNether = 0.0f;
        public Material.Elements targetElement = Material.Elements.Nothing;
        public Vector3 pos = new Vector3();

        public Action(){ }
        public Action(Action o){
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
            return ((0 < usedNether)||(0<usedAether));
        }
        public boolean aetherActive(){
            return (0 < usedAether);
        }

        public boolean netherActive(){
            return (0 < usedNether);
        }

        public void acceptInput(String name, Float... parameters){

        }
    }

    public static Color getColorOf(Action action, float maxMana){
        float r,g,b;
        r = (Math.abs(action.usedNether))/(maxMana); /* Re component ==> Nether */
        if(0 >= action.usedAether){
            g = (Math.abs(action.usedAether))/(maxMana);
        }else g = 0.0f;
        if(0 >= action.usedNether){
            g += (Math.abs(action.usedNether))/(maxMana);
        }else g += 0.0f;
        g = g/2.0f; /* Green component ==> Sign indicator for both components */
        b = (Math.abs(action.usedAether))/(maxMana); /* Blue component ==> Aether */
        /* Used Aether --> Blue; Nether --> Red; range: 0;1 */
        if(0 == (r+g+b))return new Color(0,0,0,0);
        return new Color(r,g,b,1.0f);
    }
}
