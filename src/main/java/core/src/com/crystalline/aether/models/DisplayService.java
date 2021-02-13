package com.crystalline.aether.models;

public interface DisplayService<Media> {
    Media display();
    void render();
    void dispose();
}
