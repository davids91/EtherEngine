package com.crystalline.aether.models.world;

import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;

public class ElementalAspectStrategy extends RealityAspectStrategy{
    public static boolean isMovable(int x, int y, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return Material.movable(getElementEnum(x,y,chunkSize,elements), World.getUnit(x,y,chunkSize,scalars));
    }

    /*TODO: include forces into the logic ( e.g. a big force can force a switch ) */
    public static boolean aCanMoveB(int ax, int ay, int bx, int by, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return(
            Material.discardable(ElementalAspectStrategy.getElementEnum(bx, by, chunkSize, elements), World.getUnit(bx, by, chunkSize, scalars))
            ||(
                (ElementalAspectStrategy.getWeight(ax,ay, chunkSize, elements, scalars) >= ElementalAspectStrategy.getWeight(bx, by, chunkSize, elements, scalars))
                && Material.movable(ElementalAspectStrategy.getElementEnum(ax,ay, chunkSize, elements), World.getUnit(ax,ay, chunkSize, scalars))
                && Material.movable(ElementalAspectStrategy.getElementEnum(bx,by, chunkSize, elements), World.getUnit(bx,by, chunkSize, scalars))
            )
        );
    }

    public static Material.Elements getElementEnum(int x, int y, int chunkSize, FloatBuffer buffer){
        return Material.Elements.get((int)getElement(x,y,chunkSize,buffer));
    }

    public static float getElement(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, buffer);
    }

    public static void setElement(int x, int y, int chunkSize, FloatBuffer buffer, Material.Elements element){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer,(float)element.ordinal());
    }

    private static final Vector2 tmpVec = new Vector2();
    public static Vector2 getForce(int x, int y, int chunkSize, FloatBuffer buffer){
        tmpVec.set(
                BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,0, buffer),
                BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,1, buffer)
        );
        return tmpVec;
    }
    public static float getForceX(int x, int y, int chunkSize, FloatBuffer buffer){
        return getForce(x,y, chunkSize, buffer).x;
    }
    public static float getForceY(int x, int y, int chunkSize, FloatBuffer buffer){
        return getForce(x,y, chunkSize, buffer).y;
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer buffer, Vector2 value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, value.x);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, value.y);
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer buffer, float valueX, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, valueX);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void setForceX(int x, int y, int chunkSize, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, valueX);
    }
    public static void setForceY(int x, int y, int chunkSize, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void addToForce(int x, int y, int chunkSize, FloatBuffer buffer, float valueX, float valueY){
        addToForceX(x, y, chunkSize, buffer, valueX);
        addToForceY(x, y, chunkSize, buffer, valueY);
    }
    public static void addToForceX(int x, int y, int chunkSize, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, getForceX(x,y, chunkSize, buffer) + valueX);
    }
    public static void addToForceY(int x, int y, int chunkSize, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, getForceY(x,y, chunkSize,  buffer) + valueY);
    }
    public static int getVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer){
        return (int)BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,2,buffer);
    }
    public static void setVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer, int value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,2,buffer, value);
    }
    public static void increaseVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,2,buffer, getVelocityTick(x,y, chunkSize, buffer)+1);
    }
    public static float getGravityCorrection(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,3,buffer);
    }
    public static void setGravityCorrection(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, chunkSize, Config.bufferCellSize,3,buffer, value);
    }

    /* TODO: Weight to include pressure somehow? or at least the same materials on top */
    public static float getWeight(int x, int y, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return (
                World.getUnit(x,y, chunkSize, scalars)
                        * Material.TYPE_SPECIFIC_GRAVITY
                        [(int)ElementalAspectStrategy.getElement(x,y, chunkSize, elements)]
                        [MiscUtils.indexIn(
                        Material.TYPE_UNIT_SELECTOR[(int)ElementalAspectStrategy.getElement(x,y, chunkSize, elements)],
                        World.getUnit(x,y, chunkSize, scalars)
                )]
        );
    }
}
