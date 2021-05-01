package com.crystalline.aether.services.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.services.utils.SpellUtil;
import com.crystalline.aether.models.architecture.CapsuleService;
import com.crystalline.aether.models.architecture.Scene;

/**TODO:
 * - target different elements with the spell
 */
public class EtherBrushPanel extends CapsuleService {
    private final Skin skin;
    Image aetherImg;
    Image netherImg;

    private final float maxMana;
    private final  TextButton plusBtn;
    private final ImageTextButton ohBtn;
    private final TextButton minusBtn;
    private final ProgressBar strengthBar;
    private final HorizontalGroup horizontalGroup;

    private int manaToUse = 5;
    private boolean addingAether = false;
    private boolean addingNether = false;
    private SpellUtil.SpellEtherTendency usageTendency;
    private Material.Elements targetMaterial;

    public EtherBrushPanel(Scene parent, Skin skin_, float maxMana_) {
        super(parent);
        skin = skin_;
        maxMana = maxMana_;
        usageTendency = SpellUtil.SpellEtherTendency.GIVE;
        targetMaterial = Material.Elements.Nothing;
        BitmapFont font = skin.getFont("default-font");

        ProgressBar.ProgressBarStyle pbarstyle = new ProgressBar.ProgressBarStyle();
        pbarstyle.background = skin.getDrawable("progress-bar-vertical");
        pbarstyle.knob = skin.getDrawable("progress-bar-knob-vertical");
        pbarstyle.knobBefore = skin.getDrawable("progress-bar-knob-vertical");

        strengthBar = new ProgressBar(0.1f, maxMana,0.1f, true, pbarstyle);
        strengthBar.setAnimateDuration(0.25f);
        strengthBar.setValue(5);
        strengthBar.setProgrammaticChangeEvents(false);

        Table amount_table = new Table();
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");
        textButtonStyle.checked = skin.getDrawable("button-pressed");

        plusBtn = new TextButton("+",new TextButton.TextButtonStyle(textButtonStyle));
        plusBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        ohBtn = new ImageTextButton("",new ImageTextButton.ImageTextButtonStyle(textButtonStyle));
        ohBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        minusBtn = new TextButton("-",new TextButton.TextButtonStyle(textButtonStyle));
        minusBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        plusBtn.setChecked(true); /* Usage tendency is to give by default */
        amount_table.add(plusBtn).row();
        amount_table.add(ohBtn).size(32,32).row();
        amount_table.add(minusBtn).row();

        Table ether_dispay_table = new Table();
        netherImg = new Image(skin.getDrawable("nether"));
        aetherImg = new Image(skin.getDrawable("aether"));

        ether_dispay_table.add(netherImg).size(64,64).row();
        ether_dispay_table.add(aetherImg).size(64,64);

        horizontalGroup = new HorizontalGroup();
        horizontalGroup.addActor(strengthBar);
        horizontalGroup.addActor(amount_table);
        horizontalGroup.addActor(ether_dispay_table);
    }

    public Actor getContainer(){
        return horizontalGroup;
    }

    @Override
    public void calculate() {
        if(addingAether){
            aetherImg.setDrawable(skin.getDrawable("aether_colored"));
        }else{
            aetherImg.setDrawable(skin.getDrawable("aether"));
        }
        if(addingNether){
            netherImg.setDrawable(skin.getDrawable("nether_colored"));
        }else{
            netherImg.setDrawable(skin.getDrawable("nether"));
        }
    }

    @Override
    public void acceptInput(String name, Object... parameters) {
        if(name.equals("netherActive")){
            setBrushAction(getAddingAether(),true);
        }else if(name.equals("netherInactive")){
            setBrushAction(getAddingAether(),false);
        }else if(name.equals("aetherActive")){
            setBrushAction(true, getAddingNether());
        }else if(name.equals("aetherInactive")){
            setBrushAction(false, getAddingNether());
        }else if(name.equals("upTendency")){
            upTendency();
        }else if(name.equals("downTendency")){
            downTendency();
        }else if(name.equals("manaModif")&&(1 == parameters.length)){
            signal("manaToUse", modifyManaToUse((float)parameters[0]));
        }
    }

    private void setNormalizeTendency(Drawable drw){
        ohBtn.getStyle().imageChecked = drw;
        ohBtn.getStyle().imageUp = drw;
        ohBtn.getStyle().imageDown = drw;
    }

    private void refreshTendency(){
        if(SpellUtil.SpellEtherTendency.GIVE == usageTendency){
            minusBtn.setChecked(false);
            ohBtn.setChecked(false);
            plusBtn.setChecked(true);
        }else if(SpellUtil.SpellEtherTendency.EQUALIZE == usageTendency){
            minusBtn.setChecked(false);
            ohBtn.setChecked(true);
            plusBtn.setChecked(false);
        }else if(SpellUtil.SpellEtherTendency.TAKE == usageTendency){
            minusBtn.setChecked(true);
            ohBtn.setChecked(false);
            plusBtn.setChecked(false);
        }
        switch (targetMaterial){
            case Earth:
                setNormalizeTendency(skin.getDrawable("earth")); break;
            case Water:
                setNormalizeTendency(skin.getDrawable("water")); break;
            case Air:
                setNormalizeTendency(skin.getDrawable("air")); break;
            case Fire:
                setNormalizeTendency(skin.getDrawable("fire")); break;
            case Ether:
                setNormalizeTendency(skin.getDrawable("ether")); break;
            case Nothing:
                setNormalizeTendency(null); break;
        }
        signal("targetElement",targetMaterial);
        signal("tendencyTo", usageTendency);
    }

    public void upTendency() {
        if (SpellUtil.SpellEtherTendency.values().length - 1 > usageTendency.ordinal()){
            usageTendency = usageTendency.next();
        }else{
            targetMaterial = targetMaterial.next();
        }
        refreshTendency();
    }

    public void downTendency(){
        if(0 < usageTendency.ordinal()) {
            usageTendency = usageTendency.previous();
        }else{
            targetMaterial = targetMaterial.previous();
        }
        refreshTendency();
    }

    public int modifyManaToUse(float amount){
        float modif = Math.max(0.4f,Math.min(0.6f, (manaToUse / maxMana)));
        if(0 < -amount) {
            manaToUse = (int)Math.min(Math.max(0.1f, manaToUse * (1+modif)), maxMana);
        } else {
            manaToUse = (int)Math.min(Math.max(0.1f, manaToUse * (1-modif)), maxMana);
        }
        strengthBar.setValue(manaToUse);
        return manaToUse;
    }

    public boolean getAddingAether(){
        return addingAether;
    }
    public boolean getAddingNether(){
        return  addingNether;
    }
    public void setBrushAction(boolean addingAether_, boolean addingNether_){
        addingAether = addingAether_;
        addingNether = addingNether_;
    }

    @Override
    public void dispose() {

    }
}
