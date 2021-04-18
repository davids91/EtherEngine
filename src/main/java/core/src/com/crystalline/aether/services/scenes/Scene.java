package com.crystalline.aether.services.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.crystalline.aether.services.architecture.CapsuleService;
import com.crystalline.aether.services.architecture.DisplayService;
import com.crystalline.aether.services.architecture.InputService;
import com.crystalline.aether.services.SceneHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class Scene{
    public abstract InputProcessor getInputProcessor();
    public abstract void resize(int width, int height);

    protected SceneHandler parent;
    private int activeUserView;

    private final ArrayList<Boolean> activeCapsules;
    private final ArrayList<CapsuleService> capsules; /* TODO: use Hashmap for targeted signals */
    private final ArrayList<DisplayService> userViews;
    private final ArrayList<InputService> inputHandlers;
    private final HashMap<String, Float[]> signalsToSend;
    private final HashMap<String, Float[]> collectedSignals;
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

    public void signal(String signal, Float... parameters){ /* TODO: use a queue of signals instead */
        collectedSignals.put(signal, parameters);
    }

    public void setActiveUserView(int i){
        if((0 < i)&&(i < userViews.size())){
           activeUserView = i;
        }
    }

    public void calculate(){
        signalsToSend.putAll(collectedSignals);
        collectedSignals.clear();
        for(Map.Entry<String,Float[]> entry : signalsToSend.entrySet()){
            for(int i = 0;i <capsules.size(); ++i){
                if(activeCapsules.get(i)){
                    capsules.get(i).accept_input(entry.getKey(),entry.getValue());
                }
            }
            parent.accept_signal(entry.getKey());
        }
        signalsToSend.clear();

        for(int i = 0;i <capsules.size(); ++i){
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
        for(CapsuleService capsl : capsuleServices){
            capsules.add(capsl);
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
