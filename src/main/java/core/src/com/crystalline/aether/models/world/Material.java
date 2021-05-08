package com.crystalline.aether.models.world;

import com.badlogic.gdx.graphics.Color;
import com.crystalline.aether.services.utils.MiscUtil;

import java.util.*;

public class Material {
    public enum Elements {
        Ether, Earth, Water, Air, Fire, Nothing;
        private static final Elements[] vals = values();
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
    public static final int [] netherRatios = {
        /* Ratio of sides of the golden rectangle */
        1, /* Ether *//*!Note: PHI^0 == 0.1*/
        2, /* Earth */
        3, /* Water */
        4, /* Air */
        5, /* Fire */
        0 /* Nothing */
    };

    public static int ratioOf(Elements element){
        return netherRatios[element.ordinal()];
    }

    public static final int[][] TYPE_UNIT_SELECTOR = {
        {0,50},
        {0,4,8,70,700,1000}, /* Earth */
        {0,10,20},  /* Water */
        {0, 10}, /* Air*/
        {100,200,500}, /* Fire */
        {0,0} /* Nothing */
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

    public static final int[][] TYPE_SPECIFIC_GRAVITY = {
        {0,0}, /* Ether */
        {8,16,32,64,128,256}, /* Earth */ /* TODO: Glass */
        {5,-10,10}, /* Water */ /* TODO: steam, foam and ice */
        {-6, -5}, /* Air */ /* TODO: Make sulfur, which is highly flammable */ /* TODO: Make compressed air, as a kind of slashing weapon maybe */
        {-10,2,4}, /* Fire */
        {0,0} /* Nothing */
    };

    public static boolean isSameMat(int ax, int ay, int bx, int by, Elements[][] types, int[][] units){
        return isSameMat(types[ax][ay],units[ax][ay],types[bx][by],units[bx][by]);
    }

    public static boolean isSameMat(Elements typeA, int unitA, Elements typeB, int unitB){
        return((typeA == typeB)&&(isSameMat(typeA, unitA,unitB)));
    }

    public static boolean isSameMat(Elements type, int unitA, int unitB){
        return MiscUtil.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitA) == MiscUtil.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unitB);
    }

    public static MechaProperties getState(Elements type, int unit){
        return TYPE_SPECIFIC_STATE[type.ordinal()][MiscUtil.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()], unit)];
    }

    public static boolean discardable(Elements type, int unit){
        return MechaProperties.Negligible == getState(type,unit);
    }

    public static boolean movable(Elements type, int unit){
        MechaProperties state = getState(type,unit);
        return (
            (MechaProperties.Negligible.ordinal() < state.ordinal())
            &&(MechaProperties.Solid.ordinal() > state.ordinal())
        );
    }

    public static Color getColor(Elements type, float unit){
        return TYPE_COLORS[type.ordinal()][Math.min((
            TYPE_COLORS[type.ordinal()].length - 1),
            MiscUtil.indexIn(TYPE_UNIT_SELECTOR[type.ordinal()],unit)
        )];
    }

}
