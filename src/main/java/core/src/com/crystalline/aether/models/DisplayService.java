package com.crystalline.aether.models;

public interface DisplayService<Media> {
    Media get_display();
    void render();
    void dispose();
}
