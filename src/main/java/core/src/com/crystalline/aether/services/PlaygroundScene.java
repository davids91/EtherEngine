package com.crystalline.aether.services;

import com.crystalline.aether.models.Config;

public class PlaygroundScene extends Scene{
    private final Config conf;
    private final WorldCapsule worldCapsule;

    public PlaygroundScene(Config conf_){
        conf = conf_;
        worldCapsule = new WorldCapsule(conf);
        WorldDisplay worldDisplayCapsule = new WorldDisplay(worldCapsule, conf);
        UserInputCapsule userInputCapsule = new UserInputCapsule(worldCapsule, worldDisplayCapsule, conf);
        addViews(worldDisplayCapsule);
        setActiveUserView(0);
        addInputHandlers(userInputCapsule);
        setActiveInputHandlers(0);
    }

    @Override
    public void calculate() {
        worldCapsule.calculate();
    }
}
