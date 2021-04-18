package com.crystalline.aether.services.architecture;

public interface CapsuleService {
    void calculate();
    void accept_input(String name, float... parameters);
    void dispose();
}
