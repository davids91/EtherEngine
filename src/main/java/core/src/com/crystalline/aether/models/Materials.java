package com.crystalline.aether.models;

import com.badlogic.gdx.graphics.Color;
import com.crystalline.aether.Util;

import java.util.*;

public class Materials {
    public enum Names {
        Earth,
        Water,
        Air,
        Fire,
        Nothing
    }

    public static final HashMap<Names,HashSet<Names>> compatibility = new HashMap<Names,HashSet<Names>>(){{
        put(Names.Earth,new HashSet<>(Arrays.asList(Names.Earth, Names.Water, Names.Air, Names.Fire)));
        put(Names.Water,new HashSet<>(Arrays.asList(Names.Earth, Names.Water, Names.Air, Names.Fire)));
        put(Names.Air,new HashSet<>(Arrays.asList(Names.Earth, Names.Water, Names.Air, Names.Fire)));
        put(Names.Fire,new HashSet<>(Arrays.asList(Names.Earth, Names.Water, Names.Air, Names.Fire)));
    }};

    /**!Note: The ratio of the two values define the material states. Reality tries to "stick" to given ratios,
     * The difference in s radiating away.  */
    public static final float PHI = 1.61803398875f;
    public static final float [] nether_ratios = {
        /* Ratio of sides of the golden rectangle */
        (PHI), /* Earth */
        (PHI * PHI), /* Water */
        (PHI * PHI * PHI), /* Air */
        (PHI * PHI * PHI * PHI), /* Fire */
//        0.0f, /* Nothing */
    };

    /* Names.Materials --> { normal pressure tolerance, pressure threshold to move, pressure threshold to merge} */
    public static final float[][] type_specific_gravity = {
        {10.0f,20.0f,40.0f,80.0f,160.0f,320.0f}, /* Earth */
        {2.0f,4.0f}, /* Water */
        {0.001f,0.02f}, /* Air */
        {50.0f,75.0f,100.0f}, /* Fire */
        {0,0} /* Nothing */
    };

    public static final float[][] type_color_scale = {
        {0.0f,2.0f,3.5f,70.0f,700.0f,1000.0f}, /* Earth */
        {10.0f,20.0f},  /* Water */
        {0.0f, 20.0f}, /* Air*/
        {100.0f,300.0f,500.0f}, /* Fire */
        {0,0} /* Nothing */
    };

    public static boolean is_same_mat(int ax, int ay, int bx, int by, Materials.Names[][] types, float[][] units){
        return is_same_mat(types[ax][ay],units[ax][ay],types[bx][by],units[bx][by]);
    }

    public static boolean is_same_mat(Materials.Names typeA, float unitA,Materials.Names typeB, float unitB){
        return((typeA == typeB)&&(is_same_mat(typeA, unitA,unitB)));
    }

    public static boolean is_same_mat(Materials.Names type, float unitA, float unitB){
        return Util.index_in(type_color_scale[type.ordinal()],unitA) == Util.index_in(type_color_scale[type.ordinal()],unitB);
    }

    public static boolean discardable(Names type, float unit){
        switch (type){
            case Air:
                /* Only the first scale shall be discardable */
                if(1 > Util.index_in(type_color_scale[type.ordinal()],unit)) return true;
                else return false;
            case Water:
            case Earth:
            case Fire:
                return false;
            default: return false;
        }
    }

    public static boolean is_hard(Names type, float unit){ /* basically means that the material is collapsible in itself */
        switch (type){
            case Earth:
                return true;
            case Fire:
                if(0 > Util.index_in(type_color_scale[type.ordinal()],unit)) return true;
                else return false;
            case Air:
            case Water:
            default: return false;
        }
    }

    public static boolean a_can_be_merged_into_b(int ax, int ay, int bx, int by, Materials.Names[][] types, float[][] units){
        return(
            is_same_mat(ax,ay, bx,by, types, units) /* If the materials are the same in the 2 cells */
            &&(!is_hard(types[ax][ay], units[ax][ay]))
            &&(
                is_same_mat(types[ax][ay], units[ax][ay], (units[ax][ay] + units[bx][by]) ) /* and would remain the same should they merge */
                ||(Names.Nothing == types[bx][by])
            )
        );
    }

    public static boolean movable(Names type, float unit){
        switch (type){
            case Earth:
                /* Only the first 2 scale shall be movable ( representing sand and uhm... sand2 )  */
                if(2 > Util.index_in(type_color_scale[type.ordinal()],unit)) return true;
                    else return false;
            case Water:
            case Air:
            case Fire:
                return true;
            default: return false;
        }
    }

    public static final Color[][] colors = {
        {Color.TAN,Color.GOLDENROD,Color.BROWN,Color.GRAY,Color.SLATE,Color.TEAL}, /* Earth */
        {Color.ROYAL,Color.NAVY}, /* Water */
        //Color.PURPLE,
        {Color.SKY, Color.valueOf("#d9c9c9")}, /* Air */
        {Color.FIREBRICK, Color.SCARLET, Color.CYAN}, /* Fire */
        {Color.CLEAR} /* Nothing */
    };

    public static Color get_color(Names type, float unit){
        return colors[type.ordinal()][Math.min((
            colors[type.ordinal()].length - 1),
            Util.index_in(type_color_scale[type.ordinal()],unit)
        )];
    }

}
