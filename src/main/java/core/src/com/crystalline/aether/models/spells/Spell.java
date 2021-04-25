package com.crystalline.aether.models.spells;

import com.badlogic.gdx.graphics.Color;

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

    public static Color getColorOf(SpellAction action, float maxMana){
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
