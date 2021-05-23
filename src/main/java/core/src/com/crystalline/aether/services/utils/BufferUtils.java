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

    public static void set(int x, int y, int maxX, int cellSize, int offset,  FloatBuffer buffer, float value){
        if(offset >= cellSize) throw new IndexOutOfBoundsException("internal offset value is greater, than cellSize!");
        buffer.put(indexOf(x,y,cellSize,maxX) + offset, value);
    }

    public static void increase(int x, int y, int maxX, int cellSize, int offset,  FloatBuffer buffer, float value){
        if(offset >= cellSize) throw new IndexOutOfBoundsException("internal offset value is greater, than cellSize!");
        buffer.put(indexOf(x,y,cellSize,maxX) + offset, (get(x,y,cellSize,maxX,offset,buffer) + value));
    }

    public static void decrease(int x, int y, int maxX, int cellSize, int offset,  FloatBuffer buffer, float value){
        if(offset >= cellSize) throw new IndexOutOfBoundsException("internal offset value is greater, than cellSize!");
        buffer.put(indexOf(x,y,cellSize,maxX) + offset, (get(x,y,maxX,cellSize,offset,buffer) - value));
    }

    public static void multiply(int x, int y, int maxX, int cellSize, int offset,  FloatBuffer buffer, float value){
        if(offset >= cellSize) throw new IndexOutOfBoundsException("internal offset value is greater, than cellSize!");
        buffer.put(indexOf(x,y,cellSize,maxX) + offset, (get(x,y,cellSize,maxX,offset,buffer) * value));
    }

    public static float get(int x, int y, int maxX, int cellSize, int offset, FloatBuffer buffer){
        if(offset >= cellSize) throw new IndexOutOfBoundsException("internal offset value is greater, than cellSize!");
        return buffer.get(indexOf(x,y,cellSize,maxX) + offset);
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

    public static void copy(FloatBuffer source, FloatBuffer target){
        if(source.capacity() != target.capacity()) throw new UnsupportedOperationException("Buffer size mismatch!");
        target.position(0);source.position(0);
        for(int i = 0; i < source.capacity(); ++i) target.put(source.get(i));
    }
}
