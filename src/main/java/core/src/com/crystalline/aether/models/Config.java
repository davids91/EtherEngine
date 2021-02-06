package com.crystalline.aether.models;

public class Config {
    public final int[] world_block_number = {200,200};
    public final float world_block_size = 100.0f;
    public final float block_radius = world_block_size/2.0f;
    public final float[] world_size = {world_block_number[0] * world_block_size, world_block_number[1] * world_block_size};
}
