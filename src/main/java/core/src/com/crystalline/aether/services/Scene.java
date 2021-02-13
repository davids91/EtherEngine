package com.crystalline.aether.services;

import com.crystalline.aether.models.CapsuleService;
import com.crystalline.aether.models.DisplayService;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Scene {

    public abstract void calculate();

    private int activeUserView;
    private final ArrayList<DisplayService> userViews;

    private final ArrayList<Integer> activeUIs;
    private final ArrayList<DisplayService> UIs;

    private final ArrayList<Integer> activeInputHandlers;
    private final ArrayList<CapsuleService> inputHandlers;

    public Scene(){
        userViews = new ArrayList<>();

        UIs = new ArrayList<>();
        activeUIs = new ArrayList<>();

        inputHandlers = new ArrayList<>();
        activeInputHandlers = new ArrayList<>();
    }

    public void setActiveUserView(int i){
        if((0 < i)&&(i < userViews.size())){
           activeUserView = i;
        }
    }
    public void addViews(DisplayService... views){
        userViews.addAll(Arrays.asList(views));
    }

    public void setActiveInputHandlers(int... indices){
        activeInputHandlers.clear();
        for(int i : indices){
            System.out.println(i +" < " +inputHandlers.size());
            if((0 <= i)&&(i < inputHandlers.size())){
                activeInputHandlers.add(i);
            }
        }
    }

    public void addInputHandlers(CapsuleService... inputHandlers_){
        inputHandlers.addAll(Arrays.asList(inputHandlers_));
    }

    public void setActiveUserInterfaces(int... indices){
        activeUIs.clear();
        for(int i : indices){
            if((0 <= i)&&(i < UIs.size())){
                activeUIs.add(i);
            }
        }
    }

    public void addUserInterfaces(DisplayService... uis){
        UIs.addAll(Arrays.asList(uis));
    }

    public void render(){
        for(int i : activeInputHandlers){
            inputHandlers.get(i).calculate();
        }

        for(int i : activeUIs){
            UIs.get(i).render();
        }

        userViews.get(activeUserView).render();
    }

    public void dispose(){
        while(0 < inputHandlers.size()){
            inputHandlers.get(inputHandlers.size()-1).dispose();
            inputHandlers.remove(inputHandlers.size()-1);
        }
        while(0 < UIs.size()){
            UIs.get(UIs.size()-1).dispose();
            UIs.remove(UIs.size()-1);
        }
        while(0 < userViews.size()){
            userViews.get(userViews.size()-1).dispose();
            userViews.remove(userViews.size()-1);
        }
    }
}
