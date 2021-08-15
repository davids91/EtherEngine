package com.crystalline.aether.models.world;

import com.badlogic.gdx.Gdx;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.computation.Includer;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.StringUtils;

import java.nio.FloatBuffer;

public class RealityAspectStrategy {
    public static final String materialLibrary = StringUtils.readFileAsString(Gdx.files.internal("shaders/materialLibrary.fshader"));
    public static final String worldLibrary = StringUtils.readFileAsString(Gdx.files.internal("shaders/worldLibrary.fshader"));
    public static final String etherealLibrary = StringUtils.readFileAsString(Gdx.files.internal("shaders/ethLibrary.fshader"));
    public static final String elementalLibrary = StringUtils.readFileAsString(Gdx.files.internal("shaders/elmLibrary.fshader"));
    protected static final Includer baseIncluder = new Includer()
            .addSource(materialLibrary).addSource(worldLibrary).addSource(etherealLibrary).addSource(elementalLibrary);

    public static void setPriority(int x, int y, int chunkSize, FloatBuffer buffer, float prio){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,3, buffer, prio);
    }

    public static float getPriority(int x, int y, int chunkSize, FloatBuffer elements){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,3, elements);
    }

    public static float getOffsetCode(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,0, buffer);
    }

    public static void setOffsetCode(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, chunkSize,Config.bufferCellSize,0, buffer, value);
    }

    public static float getToApply(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y, chunkSize,Config.bufferCellSize,3, buffer);
    }

    public static void setToApply(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,3, buffer, value);
    }

    public static int getXFromOffsetCode(int x, int code){
        switch(code){
            case 1: case 8: case 7: return (x-1);
            case 2: case 0: case 6: return (x);
            case 3: case 4: case 5: return (x+1);
        }
        return x;
    }

    public static int getYFromOffsetCode(int y, int code){
        switch(code){
            case 7: case 6: case 5: return (y+1);
            case 8: case 0: case 4: return (y);
            case 1: case 2: case 3: return (y-1);
        }
        return y;
    }

    /**
     * Returns a code for the hardcoded directions based on the arguments
     * @param ox offset x -
     * @param oy offset y - the direction to which the direction should point
     * @return a code unique for any direction, generated by the given offsets
     */
    public static int getOffsetCode(int ox, int oy){
        if((ox < 0)&&(oy < 0)) return 1;
        if((ox == 0)&&(oy < 0)) return 2;
        if((ox > 0)&&(oy < 0)) return 3;
        if((ox > 0)&&(oy == 0)) return 4;
        if((ox > 0)/*&&(oy > 0)*/) return 5;
        if((ox == 0)&&(oy > 0)) return 6;
        if((ox < 0)&&(oy > 0)) return 7;
        if((ox < 0)/*&&(oy == 0)*/) return 8;
        return 0;
    }

    public static int getTargetX(int x, int y, int chunkSize, FloatBuffer buffer){
        return getXFromOffsetCode(x,(int)getOffsetCode(x,y,chunkSize,buffer));
    }

    public static int getTargetY(int x, int y, int chunkSize, FloatBuffer buffer){
        return getYFromOffsetCode(y,(int)getOffsetCode(x,y,chunkSize,buffer));
    }
}
