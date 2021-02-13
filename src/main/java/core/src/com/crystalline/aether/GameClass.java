package com.crystalline.aether;

import com.badlogic.gdx.ApplicationAdapter;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.PlaygroundScene;

public class GameClass extends ApplicationAdapter {
	private final Config conf = new Config();
	private PlaygroundScene defaultScene;

	@Override
	public void create () {
		defaultScene = new PlaygroundScene(conf);
	}

	@Override
	public void render() {
		defaultScene.calculate();
		defaultScene.render();
	}

	@Override
	public void dispose () {
		defaultScene.dispose();
	}
}
