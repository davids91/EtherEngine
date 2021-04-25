package com.crystalline.aether.models;

import com.badlogic.gdx.math.Vector3;

public class Config {
    public final int[] WORLD_BLOCK_NUMBER = {200,200};
    public final float WORLD_BLOCK_SIZE = 100.0f;
    public final float[] WORLD_SIZE = {
        WORLD_BLOCK_NUMBER[0] * WORLD_BLOCK_SIZE,
        WORLD_BLOCK_NUMBER[1] * WORLD_BLOCK_SIZE,
        0.0f
    };
    public final Vector3 WORLD_DIMENSIONS = new Vector3(
        WORLD_BLOCK_NUMBER[0],
        WORLD_BLOCK_NUMBER[1],
        0.0f
    );
    public final float maxMana = 100.0f;

    public Config block_dimensions(int... numbers){
        if(numbers.length > 0) WORLD_BLOCK_NUMBER[0] = numbers[0];
        if(numbers.length > 1) WORLD_BLOCK_NUMBER[1] = numbers[1];
        WORLD_DIMENSIONS.x = WORLD_BLOCK_NUMBER[0];
        WORLD_DIMENSIONS.y = WORLD_BLOCK_NUMBER[1];
        return this;
    }
}
