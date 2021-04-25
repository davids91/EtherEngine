package com.crystalline.aether.services.utils;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Util {
    public static final Vector3 zeroVec = new Vector3(0,0, 0);
    private static final Vector2 gravity = new Vector2(0f,-3.81f);
    private static final Vector2 tmp_gravity = new Vector2(gravity);
    public final Vector2 getGravity(int posX, int posY){
//        if(((float)conf.world_block_number[1]*0.9f) <= posY) tmp_gravity.y = -gravity.y;
//        else tmp_gravity.y = gravity.y; /* TODO: Define gravity for multiple chunks */
        return tmp_gravity;
    }

    public static int index_in(float[] table, float value){
        int index = table.length-1;
        while((index > 0)&&(table[index] >= value))--index;
        return index;
    }

    public static class MyCell extends Vector2 {
        private final int sizeX;

        public MyCell(int sizeX_) {
            super();
            sizeX = sizeX_;
        }

        public MyCell(MyCell other){
            super(other.x,other.y);
            sizeX = other.sizeX;
        }

        public MyCell(float x, float y, int sizeX_) {
            super(x, y);
            sizeX = sizeX_;
        }

        public int get_i_x(){
            return (int)x;
        }

        public int get_i_y(){
            return (int)y;
        }

        @Override
        public int hashCode() {
            return MathUtils.coordinateToHash((int)x,(int)y,sizeX);
        }
    }
}
