package com.crystalline.aether.services.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BufferUtils {
    public static int map2DTo1D(int x, int y, int maxX){ /* TODO: make it 3D */
        return  (y * maxX) + x;
    }

    public static int indexOf(int x, int y, int cellsize, int maxX){
        return map2DTo1D(x,y,maxX) * cellsize;
    }

    public static void set(int x, int y, int cellsize, int maxX, FloatBuffer buffer, float value){
        buffer.position(indexOf(x,y,cellsize,maxX));
        buffer.put(value);
    }

    public static FloatBuffer clone(FloatBuffer original) {
        FloatBuffer clone = ByteBuffer.allocate(original.capacity() * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        int originalPosition = original.position();
        original.rewind();
        clone.put(original);
        original.position(originalPosition);
        clone.flip();
        return clone;
    }
}
