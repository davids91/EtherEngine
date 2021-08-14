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
    public static final float [] netherRatios = {
        /* Ratio of sides of the golden rectangle */
        1.0f, /* Ether *//*!Note: PHI^0 == 0.1*/
        (PHI), /* Earth */
        (PHI * PHI), /* Water */
        (PHI * PHI * PHI), /* Air */
        (PHI * PHI * PHI * PHI), /* Fire */
        0.0f, /* Nothing */
    };

    public static float ratioOf(Elements element){
        return netherRatios[element.ordinal()];
    }

    public static final float[][] TYPE_UNIT_SELECTOR = {
        {0.0f, 50.0f}, /* Ether */
        {0.0f,10.0f,15f,70.0f,700.0f,1000.0f}, /* Earth */
        {0,50.0f,100.0f},  /* Water */
        {0.0f, 10.0f}, /* Air*/
        {10.0f,50.0f,100.0f}, /* Fire */
        {0.0f} /* Nothing */
    };

    public static final Color[][] TYPE_COLORS = { /* TODO: Ether Vapor */
        {Color.PINK, Color.PURPLE}, /* Ether */
        {Color.TAN,Color.GOLDENROD,Color.BROWN,Color.GRAY,Color.SLATE,Color.TEAL}, /* Earth */
        {Color.ROYAL, Color.valueOf("#d9c9c9"), Color.NAVY}, /* Water */
        {Color.SKY, Color.SKY}, /* Air, also Air */
        {Color.FIREBRICK, Color.SCARLET, Color.CYAN}, /* Fire */
        {Color.CLEAR} /* Nothing */
    };

    public static final MechaProperties[][] TYPE_SPECIFIC_STATE = {
        {MechaProperties.Gas, MechaProperties.Granular}, /* Ether */
        {MechaProperties.Granular, MechaProperties.Granular, MechaProperties.Solid, MechaProperties.Crystal, MechaProperties.Hard, MechaProperties.Superhard}, /* Earth */
        {MechaProperties.Fluid, MechaProperties.Gas, MechaProperties.Fluid},/* Water */
        {MechaProperties.Negligible, MechaProperties.Negligible}, /* Air */
        {MechaProperties.Plasma, MechaProperties.Fluid, MechaProperties.Ultrahard}, /* Fire */
        {MechaProperties.Negligible}, /* Nothing */
    };

    public static final float[][] TYPE_SPECIFIC_GRAVITY = {
        {0.0f,0.0f}, /* Ether */
        {8.0f,16.0f,32.0f,64.0f,128.0f,256.0f}, /* Earth */ /* TODO: Glass */
        {2.5f,-10f,4.0f}, /* Water */ /* TODO: steam, foam and ice */
        {-0.002f, -0.001f}, /* Air */ /* TODO: Make sulfur, which is highly flammable */ /* TODO: Make compressed air, as a kind of slashing weapon maybe */
        {-0.05f,80.0f,160.0f}, /* Fire */
        {0,0} /* Nothing */
    };

    public static boolean isSameMat(Elements typeA, float unitA, Elements typeB, float unitB){
        return((typeA == typeB)&&(isSameMat(typeA, unitA,unitB)));
    }

    public static boolean isSameMat(Elements type, float unitA, float unitB){
        return MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitA) == MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitB);
    }

    public static MechaProperties getState(Elements type, float unit){
        return TYPE_SPECIFIC_STATE[type.ordinal()][MiscUtils.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()], unit)];
    }

    public static boolean discardable(Elements type, float unit){
        return MechaProperties.Negligible == getState(type,unit);
    }

    public static boolean movable(Elements type, float unit){
        MechaProperties state = getState(type,unit);
        return (
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
