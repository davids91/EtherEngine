package com.crystalline.aether.models.world;

import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;

public class ElementalAspectStrategy extends RealityAspectStrategy{
    public static boolean isMovable(int x, int y, int sizeX, FloatBuffer elements, FloatBuffer scalars){
        return Material.movable(getElementEnum(x,y,sizeX,elements), World.getUnit(x,y,sizeX,scalars));
    }

    public static Material.Elements getElementEnum(int x, int y, int sizeX, FloatBuffer buffer){
        return Material.Elements.get((int)getElement(x,y,sizeX,buffer));
    }

    public static float getElement(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX, Config.bufferCellSize,0, buffer);
    }

    public static void setElement(int x, int y, int sizeX, FloatBuffer buffer, Material.Elements element){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer,(float)element.ordinal());
    }

    private static final Vector2 tmpVec = new Vector2();
    public static Vector2 getForce(int x, int y, int sizeX, FloatBuffer buffer){
        tmpVec.set(
                BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer),
                BufferUtils.get(x,y,sizeX,Config.bufferCellSize,1, buffer)
        );
        return tmpVec;
    }
    public static float getForceX(int x, int y, int sizeX, FloatBuffer buffer){
        return getForce(x,y, sizeX, buffer).x;
    }
    public static float getForceY(int x, int y, int sizeX, FloatBuffer buffer){
        return getForce(x,y, sizeX, buffer).y;
    }
    public static void setForce(int x, int y, int sizeX, FloatBuffer buffer, Vector2 value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, value.x);
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, value.y);
    }
    public static void setForce(int x, int y, int sizeX, FloatBuffer buffer, float valueX, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, valueX);
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void setForceX(int x, int y, int sizeX, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, valueX);
    }
    public static void setForceY(int x, int y, int sizeX, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void addToForce(int x, int y, int sizeX, FloatBuffer buffer, float valueX, float valueY){
        addToForceX(x, y, sizeX, buffer, valueX);
        addToForceY(x, y, sizeX, buffer, valueY);
    }
    public static void addToForceX(int x, int y, int sizeX, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, getForceX(x,y, sizeX, buffer) + valueX);
    }
    public static void addToForceY(int x, int y, int sizeX, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, getForceY(x,y, sizeX,  buffer) + valueY);
    }
    public static int getVelocityTick(int x, int y, int sizeX, FloatBuffer buffer){
        return (int)BufferUtils.get(x,y,sizeX,Config.bufferCellSize,2,buffer);
    }
    public static void setVelocityTick(int x, int y, int sizeX, FloatBuffer buffer, int value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,2,buffer, value);
    }
    public static void increaseVelocityTick(int x, int y, int sizeX, FloatBuffer buffer){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,2,buffer, getVelocityTick(x,y, sizeX, buffer)+1);
    }
    public static float getGravityCorrection(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,3,buffer);
    }
    public static void setGravityCorrection(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, sizeX, Config.bufferCellSize,3,buffer, value);
    }

    /* TODO: Weight to include pressure somehow? or at least the same materials on top */
    public static float getWeight(int x, int y, int sizeX, FloatBuffer elements, FloatBuffer scalars){
        return (
                World.getUnit(x,y, sizeX, scalars)
                        * Material.TYPE_SPECIFIC_GRAVITY
                        [(int)ElementalAspectStrategy.getElement(x,y, sizeX, elements)]
                        [MiscUtils.indexIn(
                        Material.TYPE_UNIT_SELECTOR[(int)ElementalAspectStrategy.getElement(x,y, sizeX, elements)],
                        World.getUnit(x,y, sizeX, scalars)
                )]
        );
    }
}
