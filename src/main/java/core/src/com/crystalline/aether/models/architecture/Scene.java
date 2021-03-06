package com.crystalline.aether.models.architecture;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.crystalline.aether.services.scenes.SceneHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class Scene{
    public abstract InputProcessor getInputProcessor();
    public abstract void resize(int width, int height);
    public abstract String getName();

    protected SceneHandler parent;
    private int activeUserView;

    private final ArrayList<Boolean> activeCapsules;
    private final ArrayList<CapsuleService> capsules; /* TODO: use Hashmap for targeted signals */
    private final ArrayList<DisplayService> userViews;
    private final ArrayList<InputService> inputHandlers;
    private final HashMap<String, Object[]> signalsToSend;
    private final HashMap<String, Object[]> collectedSignals;
    public static class Token{ private Token(){} }
    private Token token = new Token();
    public int getToken(){
        return token.hashCode();
    }

    public Scene(SceneHandler.Builder parentBuilder){
        parent = parentBuilder.get(token);
        capsules = new ArrayList<>();
        userViews = new ArrayList<>();
        inputHandlers = new ArrayList<>();
        activeCapsules = new ArrayList<>();
        signalsToSend = new HashMap<>();
        collectedSignals = new HashMap<>();
    }

    /** Receive a signal instantly, but do not forward it to the parent.
     *  Mainly used when a scene-transition occurs, and scenes wish to share objects
     *  in-between them.
     *  */
    public void receiveSignalFromOtherScene(Scene otherScene, Object... parameters){
        for(int i = 0;i <capsules.size(); ++i){
            if(activeCapsules.get(i)){
                capsules.get(i).acceptInput("transitionFrom:"+otherScene.getName(), parameters);
            }
        }
    }

    public void signal(String name, Object... parameters){ /* TODO: use a queue of signals instead */
        collectedSignals.put(name, parameters);
    }

    public void setActiveUserView(int i){
        if((0 <= i)&&(i < userViews.size())){
           activeUserView = i;
        }
    }

    public void calculate(){
        signalsToSend.putAll(collectedSignals);
        collectedSignals.clear();
        for(Map.Entry<String,Object[]> entry : signalsToSend.entrySet()){
            for(int i = 0;i <capsules.size(); ++i){
                if(activeCapsules.get(i)){
                    capsules.get(i).acceptInput(entry.getKey(),entry.getValue());
                }
            }
            parent.acceptSignal(entry.getKey(), entry.getValue()); /* TODO: Return with whether or not scene transitions happened*/
        }
        signalsToSend.clear();

        for(int i = 0;i < capsules.size(); ++i){
            if(activeCapsules.get(i)){
                capsules.get(i).calculate();
            }
        }
    }

    public void addViews(DisplayService... views){
        userViews.addAll(Arrays.asList(views));
    }
    public void addInputHandlers(InputService... inputHandlers_){
        inputHandlers.addAll(Arrays.asList(inputHandlers_));
    }
    public void addCapsules(CapsuleService... capsuleServices){
        for(CapsuleService capsule : capsuleServices){
            capsules.add(capsule);
            activeCapsules.add(Boolean.TRUE);
        }
    }

    public void render(){
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        for(InputService is : inputHandlers) is.handleInput();
        if(0 < userViews.size())
            userViews.get(activeUserView).render();
    }

    public void dispose(){
        while(0 < inputHandlers.size()){
            inputHandlers.get(inputHandlers.size()-1).dispose();
            inputHandlers.remove(inputHandlers.size()-1);
        }
        while(0 < userViews.size()){
            userViews.get(userViews.size()-1).dispose();
            userViews.remove(userViews.size()-1);
        }
    }
}
