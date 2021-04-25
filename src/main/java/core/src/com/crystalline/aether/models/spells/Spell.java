package com.crystalline.aether.models.spells;

import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class Spell{
    private final ArrayList<SpellFrame> spellFrames;
    private final Vector3 spellSize;

    public Spell(Vector3 size){
        spellFrames = new ArrayList<>();
        spellSize = size;
    }

    public void addFrame(){
        spellFrames.add(new SpellFrame());
    }

    public void removeLastFrame(){
        getFrames().remove(getFrames().size()-1);
    }

    public final ArrayList<SpellFrame> getFrames(){
        return spellFrames;
    }

    public SpellFrame getFrame(int index){
        return spellFrames.get(index);
    }
    public final Vector3 getSize(){
        return spellSize;
    }
}
