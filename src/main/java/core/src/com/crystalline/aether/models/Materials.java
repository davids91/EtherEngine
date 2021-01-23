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
        (PHI * PHI * PHI * PHI) /* Fire */
    };

    /* Names.Materials --> { normal pressure tolerance, pressure threshold to move, pressure threshold to merge} */
    public static final float[][] type_pressure_scales = {
        {50.0f,51.0f,70.0f}, /* Earth */
        {1.0f,2.0f,3.0f}, /* Water */
        {0.1f,0.2f,0.3f}, /* Air */
        {50.0f,75.0f,100.0f}, /* Fire */
    };

    public static final float[][] type_unit_scales = {
        {0.0f,2.0f,3.5f,70.0f,700.0f,1000.0f}, /* Earth */
        {10.0f,20.0f},  /* Water */
        {0.0f, 20.0f}, /* Air*/
        {100.0f,300.0f,500.0f}, /* Fire */
    };

    public static boolean get_if_unstable_by_pressure(Names type, float pressure){
        return (1 <= Util.index_in(type_pressure_scales[type.ordinal()], pressure));
    }

    public static boolean is_same_mat(int ax, int ay, int bx, int by, Materials.Names[][] types, float[][] units){
        return ((types[ax][ay] == types[bx][by])&&(
            Util.index_in(type_unit_scales[types[ax][ay].ordinal()],units[ax][ay])
            == Util.index_in(type_unit_scales[types[bx][by].ordinal()],units[bx][by])
        ));
    }

    public static boolean movable(Names type, float unit){
        switch (type){
            case Earth:
                /* Only the first 2 scale shall be movable ( representing sand and uhm... sand2 )  */
                if(2 > Util.index_in(type_unit_scales[type.ordinal()],unit)) return true;
                    else return false;
            case Water:
            case Air:
            case Fire:
                return true;
            default: return false;
        }
    }

    public static final Color[][] colors = {
        {Color.TAN,Color.GOLDENROD,Color.BROWN,Color.GRAY,Color.SLATE,Color.TEAL},
        {Color.ROYAL,Color.NAVY},
        //Color.PURPLE,
        {Color.SKY, Color.valueOf("#e7e7e7")},
        {Color.FIREBRICK, Color.RED, Color.CYAN}
    };

    public static Color get_color(Names type, float unit){
        return colors[type.ordinal()][Math.min((
            colors[type.ordinal()].length - 1), Util.index_in(type_unit_scales[type.ordinal()],unit)
        )];
    }

}
