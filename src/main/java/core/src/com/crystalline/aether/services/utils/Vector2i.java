package com.crystalline.aether.services.utils;

import com.badlogic.gdx.math.Vector2;

public class Vector2i{
    public int x,y;

    public Vector2i(){
        this.x = 0;
        this.y = 0;
    }
    public Vector2i(Vector2i other){
        this.x = other.x;
        this.y = other.y;
    }
    public Vector2i cpy(){
        return new Vector2i(this);
    }
    public Vector2 fCpy(){
        return new Vector2(x,y);
    }
    public Vector2i set(int x, int y){
        this.x = x;
        this.y = y;
        return this;
    }
    public Vector2i set(float x, float y){
        this.x = (int)x;
        this.y = (int)y;
        return this;
    }
    public Vector2i add(int x, int y){
        this.x += x;
        this.y += y;
        return this;
    }
    public Vector2i add(float x, float y){
        this.x += x;
        this.y += y;
        return this;
    }
    public Vector2i add(Vector2i other){
        this.x += other.x;
        this.y += other.y;
        return this;
    }
    public Vector2i scl(float m){
        x *= m;
        y *= m;
        return this;
    }
    public Vector2i div(float m){
        x /= m;
        y /= m;
        return this;
    }
    public double len(){
        return Math.sqrt((double)(x*x + y*y));
    }
    public Vector2i nor(){
        double len = len();
        this.x = (int)(x / len);
        this.y = (int)(y / len);
        return this;
    }
}