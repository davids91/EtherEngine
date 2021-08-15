package com.crystalline.aether.services.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MiscUtils {
    public static final Vector3 zeroVec = new Vector3(0,0, 0);
    private static final Vector2 gravity = new Vector2(0f,-1.81f);
    private static final Vector2 tmp_gravity = new Vector2(gravity);
    public final Vector2 getGravity(int posX, int posY){
//        if((conf.world_block_number[1]*0.9f) <= posY) tmp_gravity.y = -gravity.y;
//        else tmp_gravity.y = gravity.y; /* TODO: Define gravity for multiple chunks */
        return tmp_gravity;
    }

    public static float distance(float ax, float ay, float bx, float by){
        return (float)Math.sqrt(Math.pow((ax - bx),2.0f) + Math.pow((ay - by), 2.0f));
    }

    public static int indexIn(float[] table, float value){
        int index = table.length-1;
        while((index > 0)&&(table[index] >= value))--index;
        return index;
    }

    public static class MyCell extends Vector2 {
        private final int chunkSize;

        public MyCell(int chunkSize_) {
            super();
            chunkSize = chunkSize_;
        }

        public MyCell(MyCell other){
            super(other.x,other.y);
            chunkSize = other.chunkSize;
        }

        public MyCell(float x, float y, int chunkSize_) {
            super(x, y);
            chunkSize = chunkSize_;
        }

        public int getIX(){
            return (int)x;
        }

        public int getIY(){
            return (int)y;
        }

        @Override
        public int hashCode() {
            return BufferUtils.map2DTo1D((int)x,(int)y,chunkSize);
        }
    }
}
