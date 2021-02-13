package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.crystalline.aether.models.CapsuleService;
import com.crystalline.aether.models.Spells;

public class EtherBrushPanel extends HorizontalGroup implements CapsuleService {
    private final Skin skin;
    Image aether_img;
    Image nether_img;

    boolean adding_aether = false;
    boolean adding_nether = false;
    private final int cntDownMax = 5;
    private int cntDown = cntDownMax;

    TextButton plus_btn;
    TextButton oh_btn;
    TextButton minus_btn;
    ProgressBar strength_bar;

    public EtherBrushPanel(Skin skin_, float max_spellrange) {
        skin = skin_;
        BitmapFont font = skin.getFont("default-font");

        ProgressBar.ProgressBarStyle pbarstyle = new ProgressBar.ProgressBarStyle();
        pbarstyle.background = skin.getDrawable("progress-bar-vertical");
        pbarstyle.knob = skin.getDrawable("progress-bar-knob-vertical");
        pbarstyle.knobBefore = skin.getDrawable("progress-bar-knob-vertical");

        strength_bar = new ProgressBar(0, max_spellrange,0.1f, true, pbarstyle);
        strength_bar.setAnimateDuration(0.25f);
        strength_bar.setValue(5);
        strength_bar.setProgrammaticChangeEvents(false);

        Table amount_table = new Table();
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");
        textButtonStyle.checked = skin.getDrawable("button-pressed");
        plus_btn = new TextButton("+",textButtonStyle);
        oh_btn = new TextButton("O",textButtonStyle);
        minus_btn = new TextButton("-",textButtonStyle);
        minus_btn.setChecked(true);
        amount_table.add(plus_btn).row();
        amount_table.add(oh_btn).row();
        amount_table.add(minus_btn).row();

        Table ether_dispay_table = new Table();
        nether_img = new Image(skin.getDrawable("nether"));
        aether_img = new Image(skin.getDrawable("aether"));

        ether_dispay_table.add(nether_img).size(64,64).row();
        ether_dispay_table.add(aether_img).size(64,64);

        addActor(strength_bar);
        addActor(amount_table);
        addActor(ether_dispay_table);
    }

    @Override
    public void calculate() {
        if(0 == cntDown){
            cntDown = cntDownMax;
            if(adding_aether){
                aether_img.setDrawable(skin.getDrawable("aether_colored"));
                adding_aether = false;
            }else{
                aether_img.setDrawable(skin.getDrawable("aether"));
            }
            if(adding_nether){
                nether_img.setDrawable(skin.getDrawable("nether_colored"));
                adding_nether = false;
            }else{
                nether_img.setDrawable(skin.getDrawable("nether"));
            }
        }cntDown--;
    }

    @Override
    public void accept_input(String name, float... parameters) {
        if(name.equals("add_nether")){
            adding_nether = true;
        }else if(name.equals("add_aether")){
            adding_aether = true;
        }else if(name.equals("equalize")){
            adding_nether = true;
            adding_aether = true;
        }else if(name.equals(Spells.SpellEtherTendency.Give.name())){
            minus_btn.setChecked(false);
            oh_btn.setChecked(false);
            plus_btn.setChecked(true);
        }else if(name.equals(Spells.SpellEtherTendency.Equalize.name())){
            minus_btn.setChecked(false);
            oh_btn.setChecked(true);
            plus_btn.setChecked(false);
        }else if(name.equals(Spells.SpellEtherTendency.Take.name())){
            minus_btn.setChecked(true);
            oh_btn.setChecked(false);
            plus_btn.setChecked(false);
        }else if(name.equals("spell_amount")&&(1 == parameters.length)){
            strength_bar.setValue(parameters[0]);
        }
    }

    @Override
    public float get_parameter(String name, int index) {
        return 0;
    }

    @Override
    public Object get_object(String name) {
        return null;
    }

    @Override
    public void dispose() {

    }
}
