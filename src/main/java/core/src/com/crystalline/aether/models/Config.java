package com.crystalline.aether.models;

public class Config {
    public final int CHUNK_BLOCK_NUMBER;
    public final float WORLD_BLOCK_SIZE = 100.0f;
    public final float maxMana = 100.0f;

    public Config(){
        CHUNK_BLOCK_NUMBER = 200;
    }
    public Config(int chunkSize){
        CHUNK_BLOCK_NUMBER = chunkSize;
    }

    public float getChunkSize(){
        return CHUNK_BLOCK_NUMBER * WORLD_BLOCK_SIZE;
    }
    public int getChunkBlockSize(){
        return CHUNK_BLOCK_NUMBER;
    }

    public static final int bufferCellSize = 4; /* RGBA */
}
