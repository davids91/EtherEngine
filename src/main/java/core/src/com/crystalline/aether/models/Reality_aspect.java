package com.crystalline.aether.models;

import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.services.World;

public interface Reality_aspect {
    void determine_units(float[][] units, World parent);
    void process_units(float[][] units, Vector2[][] velocity, World parent);
    void process_types(float[][] units, Vector2[][] velocity, World parent);
    void process_mechanics(float[][] units, Vector2[][] velocity, World parent);
    void post_process(float[][] units, Vector2[][] velocity, World parent);
    void switch_values(int fromX, int fromY, int toX, int toY);
    void take_over_unit_changes(int x, int y, float[][] units);
    void merge_a_to_b(int ax, int ay, int bx, int by);
    void split_a_to_b(int ax, int ay, int bx, int by);
}
