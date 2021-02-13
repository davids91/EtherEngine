package com.crystalline.aether.models;

public class Spells {
    public enum SpellEtherTendency{
        Take,Equalize,Give;
        private static final SpellEtherTendency[] vals = values();
        public SpellEtherTendency next(){ return vals[(this.ordinal() + 1) % vals.length]; }
        public SpellEtherTendency previous(){
            if(0 == this.ordinal())
                return vals[vals.length-1];
            else return vals[this.ordinal() -1];
        }
    }
}
