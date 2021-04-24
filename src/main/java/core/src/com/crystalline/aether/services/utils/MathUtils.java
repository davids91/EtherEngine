package com.crystalline.aether.services.utils;

public class MathUtils {
    public static int coordinateToHash(int x, int y, int maxX){ /* TODO: make it 3D */
        return  (y * maxX) + x;
    }
}
