package com.crystalline.aether.models;

public class Config {
    public final int[] world_block_number = {200,200};
    public final float worldBlockSize = 100.0f;
    public final float block_radius = worldBlockSize /2.0f;
    public final float[] world_size = {world_block_number[0] * worldBlockSize, world_block_number[1] * worldBlockSize};
    public final float maxMana = 100.0f;

    public Config block_dimensions(int... numbers){
        if(numbers.length > 0)world_block_number[0] = numbers[0];
        if(numbers.length > 1)world_block_number[1] = numbers[1];
        return this;
    }
}
