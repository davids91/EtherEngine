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

public abstract class Scene{
    public abstract InputProcessor getInputProcessor();
    public abstract void resize(int width, int height);

    protected SceneHandler parent;
    private int activeUserView;

    private final ArrayList<Boolean> active_capsules;
    private final ArrayList<CapsuleService> capsules; /* TODO: use Hashmap for targeted signals */
    private final ArrayList<DisplayService> userViews;
    private final ArrayList<InputService> inputHandlers;
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
        active_capsules = new ArrayList<>();
    }

    public void signal(String signal, float... parameters){ /* TODO: use a queue of signals instead */
        for(int i = 0;i <capsules.size(); ++i){
            if(active_capsules.get(i)){
                capsules.get(i).accept_input(signal, parameters);
            }
        }
        parent.accept_signal(signal);
    }

    public void setActiveUserView(int i){
        if((0 < i)&&(i < userViews.size())){
           activeUserView = i;
        }
    }

    public void calculate(){
        for(int i = 0;i <capsules.size(); ++i){
            if(active_capsules.get(i)){
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
            active_capsules.add(Boolean.TRUE);
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
