package com.crystalline.aether.models;

import com.badlogic.gdx.graphics.Color;
import com.crystalline.aether.Util;

import java.util.*;

public class Materials {
    public enum Names {
        Earth, Water, Air, Fire, Nothing
    }

    public enum Mecha_properties{
        Negligible, Gas, Fluid, Plasma, Granular, Solid, Crystal, Hard, Superhard, Ultrahard, Morning_wood
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
        0.0f, /* Nothing */
    };

    public static final float[][] type_unit_selector = {
        {0.0f,4.0f,8f,70.0f,700.0f,1000.0f}, /* Earth */
        {0,10.0f,20.0f},  /* Water */
        {0.0f, 10.0f}, /* Air*/
        {10.0f,50.0f,100.0f}, /* Fire */
        {0,0} /* Nothing */
    };

    public static final Color[][] type_colors = { /* TODO: Ether Crystals */ /* TODO: Ether Vapor */
        {Color.TAN,Color.GOLDENROD,Color.BROWN,Color.GRAY,Color.SLATE,Color.TEAL}, /* Earth */
        {Color.ROYAL, Color.valueOf("#d9c9c9"), Color.NAVY}, /* Water */
        {Color.SKY, Color.SKY}, /* Air, also Ait */
        {Color.FIREBRICK, Color.SCARLET, Color.CYAN}, /* Fire */
        {Color.CLEAR} /* Nothing */
    };

    public static final Mecha_properties[][] type_specific_state = {
        {Mecha_properties.Granular, Mecha_properties.Granular,Mecha_properties.Solid, Mecha_properties.Crystal, Mecha_properties.Hard, Mecha_properties.Superhard}, /* Earth */
        {Mecha_properties.Fluid, Mecha_properties.Gas, Mecha_properties.Fluid},/* Water */
        {Mecha_properties.Negligible, Mecha_properties.Negligible}, /* Air */
        {Mecha_properties.Plasma, Mecha_properties.Fluid, Mecha_properties.Ultrahard}, /* Fire */
        {Mecha_properties.Negligible}, /* Nothing */
    };

    public static final float[][] type_specific_gravity = {
        {4.0f,8.0f,16.0f,32.0f,64.0f,128.0f}, /* Earth */ /* TODO: GLass */
        {2.5f,-0.2f,4.0f}, /* Water */ /* TODO: steam, foam and ice */
        {-0.002f, -0.001f}, /* Air */ /* TODO: Make sulfur, which is highly flammable */ /* TODO: Make compressed air, as a kind of slashing weapon maybe */
        {-2.5f,2.0f,4.0f}, /* Fire */
        {0,0} /* Nothing */
    };

    public static boolean is_same_mat(int ax, int ay, int bx, int by, Names[][] types, float[][] units){
        return is_same_mat(types[ax][ay],units[ax][ay],types[bx][by],units[bx][by]);
    }

    public static boolean is_same_mat(Names typeA, float unitA,Names typeB, float unitB){
        return((typeA == typeB)&&(is_same_mat(typeA, unitA,unitB)));
    }

    public static boolean is_same_mat(Names type, float unitA, float unitB){
        return Util.index_in(type_unit_selector[type.ordinal()],unitA) == Util.index_in(type_unit_selector[type.ordinal()],unitB);
    }

    public static Mecha_properties get_state(Names type, float unit){
        return type_specific_state[type.ordinal()][Util.index_in(type_unit_selector[type.ordinal()], unit)];
    }

    public static boolean discardable(Names type, float unit){
        return Mecha_properties.Negligible == get_state(type,unit);
    }

    public static boolean movable(Names type, float unit){
        Mecha_properties state = get_state(type,unit);
        return (
            (Mecha_properties.Negligible.ordinal() < state.ordinal())
            &&(Mecha_properties.Solid.ordinal() > state.ordinal())
        );
    }

    public static Color get_color(Names type, float unit){
        return type_colors[type.ordinal()][Math.min((
            type_colors[type.ordinal()].length - 1),
            Util.index_in(type_unit_selector[type.ordinal()],unit)
        )];
    }

}
