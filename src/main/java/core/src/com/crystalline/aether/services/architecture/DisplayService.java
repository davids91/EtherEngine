package com.crystalline.aether.services.architecture;

public interface DisplayService<Media> {
    Media get_display();
    void resize(int width, int height);
    void render();
    void dispose();
}
