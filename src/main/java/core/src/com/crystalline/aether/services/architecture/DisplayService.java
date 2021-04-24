package com.crystalline.aether.services.architecture;

public interface DisplayService<Media> {
    Media getDisplay();
    void resize(int width, int height);
    void render();
    void dispose();
}
