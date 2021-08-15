package com.crystalline.aether;

import com.badlogic.gdx.ApplicationAdapter;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.scenes.EditorScene;
import com.crystalline.aether.services.scenes.PlaygroundScene;
import com.crystalline.aether.models.architecture.Scene;
import com.crystalline.aether.services.scenes.SceneHandler;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;

public class GameClass extends ApplicationAdapter {
	private SceneHandler sceneHandler;

	@Override
	public void create () {
		final SceneHandler.Builder builder = new SceneHandler.Builder();
		sceneHandler = builder.build(
			new ArrayList<SimpleImmutableEntry<String,Integer>>() {{
				add(new SimpleImmutableEntry<>("open_editor",1));
				add(new SimpleImmutableEntry<>("back",0));
			}},
			0,
			new ArrayList<Scene>() {{
				add(new PlaygroundScene(builder, new Config()));
				add(new EditorScene(builder, new Config(16)));
			}}
		);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		sceneHandler.resize(width, height);
	}

	@Override
	public void render() {
		sceneHandler.calculate();
		sceneHandler.render();
	}

	@Override
	public void dispose () {
		sceneHandler.dispose();
	}
}
