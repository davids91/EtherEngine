package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.crystalline.aether.services.scenes.Scene;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class SceneHandler {
    private int activeScene;
    private ArrayList<Scene> scenes;

    /**
     * Scene Transition length should always be equal to the number of Scenes, so common indices can be used.
     * Structure of the Scene transitions:
     */
    private ArrayList<AbstractMap.SimpleImmutableEntry<String,Integer>> transitions;

    private SceneHandler(){ }
    private void addScenes(ArrayList<Scene> scenes_){
        scenes = scenes_;
    }
    private void addTransitions(ArrayList<AbstractMap.SimpleImmutableEntry<String,Integer>> transitions_){
        transitions = transitions_;
    }

    public static class Builder{
        private final SceneHandler handler;
        private final HashSet<Integer> accessors;
        public Builder(){
            handler = new SceneHandler();
            accessors = new HashSet<>();
        }
        public SceneHandler get(Scene.Token token){
            Objects.requireNonNull(token, "Require the getter to only provide the half-made SceneHandler to the scenes it will handle");
            accessors.add(token.hashCode());
            return handler;
        }
        public SceneHandler build(
                ArrayList<AbstractMap.SimpleImmutableEntry<String,Integer>> transitions,
                int startSceneIndex, ArrayList<Scene> scenes){
            if(0 == scenes.size())
                throw new IllegalStateException("SceneHandler cannot be built without Scenes!");

            for(Scene scene : scenes){
                if(!accessors.contains(scene.getToken()))
                    throw new IllegalStateException("Can't add dangling Scene into SceneHandler!");
            }

            if(transitions.size() != scenes.size())
                throw new IllegalStateException("Transition entries doesn't match with Scene entries!");

            handler.addScenes(scenes);
            handler.addTransitions(transitions);
            handler.activateScene(startSceneIndex);
            return handler;
        }
    }

    public void accept_signal(String signal){
        if(signal.equals(transitions.get(activeScene).getKey()))
            activateScene(transitions.get(activeScene).getValue());
    }

    public void activateScene(int i){
        if((0 <= i)&&(scenes.size() > i)){
            activeScene = i;
            Gdx.input.setInputProcessor(scenes.get(activeScene).getInputProcessor());
        }else activeScene = 0;
    }

    public void resize(int width, int height){
        for(Scene scene : scenes)scene.resize(width, height);
    }
    public void calculate(){
        scenes.get(activeScene).calculate();
    }
    public void render(){
        scenes.get(activeScene).render();
    }
    public void dispose(){
        for(Scene s : scenes) s.dispose();
    }
}
