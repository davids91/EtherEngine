package com.crystalline.aether.models.world;

import com.badlogic.gdx.graphics.Color;
import com.crystalline.aether.services.utils.MiscUtils;

import java.nio.FloatBuffer;
import java.util.*;

public class Material {
    public enum Elements {
        Ether, Earth, Water, Air, Fire, Nothing;
        private static final Elements[] vals = values();

        public static final Elements get(int index){
            return vals[index];
        }

        public Elements next(){ return vals[(this.ordinal() + 1) % vals.length]; }
        public Elements strictNext(){ return vals[Math.min((vals.length - 1),(this.ordinal() + 1))]; }
        public Elements previous(){
            if(0 == this.ordinal())
                return vals[vals.length-1];
            else return vals[this.ordinal() -1];
        }
        public Elements strictPrevious(){ return vals[Math.max(0,(this.ordinal() - 1))]; }

    }

    public enum MechaProperties {
        Negligible, Gas, Fluid, Plasma, Granular, Solid, Crystal, Hard, Superhard, Ultrahard, MorningWood
    }

    public static final HashMap<Elements,HashSet<Elements>> compatibility = new HashMap<Elements,HashSet<Elements>>(){{
        put(Elements.Ether,new HashSet<>(Arrays.asList(Elements.Earth, Elements.Water, Elements.Air, Elements.Fire)));
        put(Elements.Earth,new HashSet<>(Arrays.asList(Elements.Earth, Elements.Water, Elements.Air, Elements.Fire)));
        put(Elements.Water,new HashSet<>(Arrays.asList(Elements.Earth, Elements.Water, Elements.Air, Elements.Fire)));
        put(Elements.Air,new HashSet<>(Arrays.asList(Elements.Earth, Elements.Water, Elements.Air, Elements.Fire)));
        put(Elements.Fire,new HashSet<>(Arrays.asList(Elements.Earth, Elements.Water, Elements.Air, Elements.Fire)));
    }};

    /**!Note: The ratio of the two values define the material states. Reality tries to "stick" to given ratios,
     * The difference is radiating away.  */
    public static final float PHI = 1.6f;//1803398875f;
    public static final float [] NETHER_RATIOS = {
        /* Ratio of sides of the golden rectangle */
        1.0f, /* Ether *//*!Note: PHI^0 == 1*/
        (PHI), /* Earth */
        (PHI * PHI), /* Water */
        (PHI * PHI * PHI), /* Air */
        (PHI * PHI * PHI * PHI), /* Fire */
        0.0f, /* Nothing */
    };

    public static float ratioOf(Elements element){
        return NETHER_RATIOS[element.ordinal()];
    }

    public static final float[][] TYPE_UNIT_SELECTOR = {
    /* Ether */     {0,  50,  500, 5000, 50000, 500000},
    /* Earth */     {0,  10,  15,  70,   700,   1000},
    /* Water */     {0,  50,  100, 1000, 10000, 100000},
    /* Air*/        {0,  10,  100, 1000, 10000, 100000},
    /* Fire */      {10, 50,  100, 1000, 10000, 100000},
    /* Nothing */   {0}
    };

    public static final Color[][] TYPE_COLORS = { /* TODO: Ether Vapor */
        {Color.PINK, Color.PURPLE, Color.PURPLE, Color.PURPLE, Color.PURPLE, Color.PURPLE}, /* Ether */
        {Color.TAN, Color.GOLDENROD, Color.BROWN, Color.GRAY, Color.SLATE, Color.TEAL}, /* Earth */
        {Color.ROYAL, Color.valueOf("#d9c9c9"), Color.NAVY, Color.NAVY, Color.NAVY, Color.NAVY}, /* Water */
        {Color.SKY, Color.SKY, Color.SKY, Color.SKY, Color.SKY, Color.SKY}, /* Air, also Air */
        {Color.FIREBRICK, Color.SCARLET, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN}, /* Fire */
        {Color.CLEAR} /* Nothing */
    };

    public static final MechaProperties[][] TYPE_SPECIFIC_STATE = {
        {MechaProperties.Gas, MechaProperties.Granular, MechaProperties.Crystal, MechaProperties.Crystal, MechaProperties.Crystal, MechaProperties.Crystal}, /* Ether */ /* TODO: Ether to have Crystal state */
        {MechaProperties.Granular, MechaProperties.Granular, MechaProperties.Solid, MechaProperties.Crystal, MechaProperties.Hard, MechaProperties.Superhard}, /* Earth */
        {MechaProperties.Fluid, MechaProperties.Gas, MechaProperties.Fluid, MechaProperties.Fluid, MechaProperties.Fluid, MechaProperties.Fluid},/* Water */
        {MechaProperties.Negligible, MechaProperties.Negligible, MechaProperties.Negligible, MechaProperties.Negligible, MechaProperties.Negligible, MechaProperties.Negligible}, /* Air */ /* TODO: Make additional gasses */
        {MechaProperties.Plasma, MechaProperties.Fluid, MechaProperties.Ultrahard, MechaProperties.Ultrahard, MechaProperties.Ultrahard, MechaProperties.Ultrahard}, /* Fire */
        {MechaProperties.Negligible}, /* Nothing */
    };

    public static final float[][] TYPE_SPECIFIC_GRAVITY = {
    /* Ether */ {0,         0,          0,      0,      0,        0},
    /* Earth */ {8,         16,         32,     64,     128,      256}, /* TODO: Glass */
    /* Water */ {2.5f,      -10,        4,      4,      4,        4},  /* TODO: steam, foam and ice */
    /* Air */   {-0.002f,   -0.001f,    0,      0,      0,        0},  /* TODO: Make sulfur, which is highly flammable */ /* TODO: Make compressed air, as a kind of slashing weapon maybe */
    /* Fire */  {-0.05f,    80,         160,    160,    160,      160},
    /* Nothing */{0,0}
    };

    public static boolean isSameMat(Elements typeA, float unitA, Elements typeB, float unitB){
        return((typeA == typeB)&&(isSameMat(typeA, unitA,unitB)));
    }

    public static boolean isSameMat(Elements type, float unitA, float unitB){
        return MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitA) == MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitB);
    }

    public static MechaProperties getState(Elements type, float unit){
        int index = MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()], unit);
        return TYPE_SPECIFIC_STATE[type.ordinal()][index];
    }

    public static boolean discardable(Elements type, float unit){
        return MechaProperties.Negligible == getState(type,unit);
    }

    public static boolean movable(Elements type, float unit){
        MechaProperties state = getState(type,unit);
        return ( /* TODO: Make movability regardless of material state ( store a flag inside the element instead )  */
            (MechaProperties.Negligible.ordinal() < state.ordinal())
            &&(MechaProperties.Solid.ordinal() > state.ordinal())
        );
    }

    public static Color getColor(Elements type, float unit){
        return TYPE_COLORS[type.ordinal()][Math.min((
            TYPE_COLORS[type.ordinal()].length - 1),
            MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unit)
        )];
    }

}
