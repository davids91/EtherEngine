package com.crystalline.aether.services.architecture;

public interface DisplayService<Media> {
    Media get_display();
    void render();
    void dispose();
}
