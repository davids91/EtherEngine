package com.crystalline.aether.services.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.crystalline.aether.services.architecture.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.capsules.WorldCapsule;

import java.util.ArrayList;

public class TimeframePanel extends Table implements CapsuleService {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private int selected_frame;
    private final SpriteBatch batch;
    ArrayList<Timeframe> frames;

    public TimeframePanel(WorldCapsule worldCapsule_, final int number_of_frames, Skin skin, Config conf_){
        conf = conf_;
        worldCapsule = worldCapsule_;
        batch = new SpriteBatch();

        BitmapFont font = skin.getFont("default-font");
        ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
        imageButtonStyle.up = skin.getDrawable("button");
        imageButtonStyle.down = skin.getDrawable("button-pressed");
        ImageButton apply_btn = new ImageButton(imageButtonStyle);

        apply_btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
            for(int i = 0; i < number_of_frames; ++i){
                frames.get(i).setFrame(worldCapsule.get_display());
                worldCapsule.accept_input("step");
            }
            event.handle();
            }
        });

        apply_btn.add( new Image(skin.getDrawable("check")));
        add(apply_btn).row();

        frames = new ArrayList<>();
        for(int i = 0; i < number_of_frames;++i){
            frames.add(new Timeframe(this, i,new TextureRegion(worldCapsule.get_display()), batch));
            frames.get(i).layout();
            add(frames.get(i)).size(32,32).pad(3);
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

    @Override
    public void calculate() {

    }

    @Override
    public void accept_input(String name, float... parameters) {

    }

    @Override
    public void dispose() {

    }
}
