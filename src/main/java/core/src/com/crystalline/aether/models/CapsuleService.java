package com.crystalline.aether.models;

public interface CapsuleService {
    void calculate();
    void accept_input(String name, float... parameters);
    float get_parameter(String name, int index);
    Object get_object(String name);
    void dispose();
}
