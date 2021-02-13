package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.crystalline.aether.models.DisplayService;
import com.crystalline.aether.models.InputService;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Scene{

    public abstract void calculate();
    public abstract InputProcessor getInputProcessor();

    private SceneHandler parent;
    private int activeUserView;
    private int activeInputHandler;

    private final ArrayList<DisplayService> userViews;
    private final ArrayList<InputService> inputHandlers;
    public static class Token{ private Token(){} }
    private Token token = new Token();
    public int getToken(){
        return token.hashCode();
    }

    public Scene(SceneHandler.Builder parentBuilder){
        parent = parentBuilder.get(token);
        userViews = new ArrayList<>();
        inputHandlers = new ArrayList<>();
    }

    protected void signal(String signal){
        parent.accept_signal(signal);
    }

    public void setActiveUserView(int i){
        if((0 < i)&&(i < userViews.size())){
           activeUserView = i;
        }
    }
    public void addViews(DisplayService... views){
        userViews.addAll(Arrays.asList(views));
    }

    public void setActiveInputHandler(int i){
        if((0 <= i)&&(i < inputHandlers.size())){
            activeInputHandler = i;
        }
    }
    public void addInputHandlers(InputService... inputHandlers_){
        inputHandlers.addAll(Arrays.asList(inputHandlers_));
    }

    public void render(){
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if(0 < inputHandlers.size())
            inputHandlers.get(activeInputHandler).handle_input();
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
