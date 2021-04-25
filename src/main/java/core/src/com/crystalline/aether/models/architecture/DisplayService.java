package com.crystalline.aether.models.architecture;

public interface DisplayService<Media> {
    Media getDisplay();
    void resize(int width, int height);
    void render();
    void dispose();
}
