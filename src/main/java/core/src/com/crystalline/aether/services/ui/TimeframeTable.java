package com.crystalline.aether.services.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.capsules.WorldCapsule;

import java.util.ArrayList;

public class TimeframeTable extends Table{
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private int selected_frame;
    private final SpriteBatch batch;
    ArrayList<Timeframe> frames;

    public TimeframeTable(WorldCapsule worldCapsule_, final int numberOfFrames, Skin skin, Config conf_){
        conf = conf_;
        worldCapsule = worldCapsule_;
        batch = new SpriteBatch();

        frames = new ArrayList<>();
        for(int i = 0; i < numberOfFrames;++i){
            frames.add(new Timeframe(this, i,new TextureRegion(worldCapsule.getDisplay()), batch));
            frames.get(i).layout();
            add(frames.get(i)).size(64,64).pad(3);
        }
        selected_frame = 0;
        setSelected(0);
        row();
    }

    public void setFrame(int i, Texture tex){
        if((0 <= i)&&(i < frames.size())){
            frames.get(i).setFrame(tex);
        }
    }

    public Timeframe getFrame(int i){
        if((0 <= i)&&(i < frames.size())){
            return frames.get(i);
        }else throw new IndexOutOfBoundsException("Frames index out of bounds!");
    }

    public void setSelected(int i){
        if((0 <= i)&&(i < frames.size())){
            frames.get(selected_frame).unselect();
            selected_frame = i;
            frames.get(selected_frame).select();

        }
    }

    public int getSelectedIndex(){
        return selected_frame;
    }
    public Timeframe getSelected(){
        return frames.get(selected_frame);
    }
}
