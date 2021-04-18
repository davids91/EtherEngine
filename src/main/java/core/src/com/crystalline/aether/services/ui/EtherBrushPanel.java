package com.crystalline.aether.services.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.crystalline.aether.models.Spells;
import com.crystalline.aether.services.architecture.CapsuleService;

/**TODO:
 * - target different elements with the spell
 */
public class EtherBrushPanel extends HorizontalGroup implements CapsuleService {
    private final Skin skin;
    Image aetherImg;
    Image netherImg;

    private final float maxMana;
    private final int cntDownMax = 5;
    private final  TextButton plusBtn;
    private final TextButton ohBtn;
    private final TextButton minusBtn;
    private final ProgressBar strengthBar;

    private float manaToUse = 5.0f;
    private boolean addingAether = false;
    private boolean addingNether = false;
    private int cntDown = cntDownMax;
    private Spells.SpellEtherTendency usageTendency;

    public EtherBrushPanel(Skin skin_, float maxMana_) {
        skin = skin_;
        maxMana = maxMana_;
        usageTendency = Spells.SpellEtherTendency.GIVE;
        BitmapFont font = skin.getFont("default-font");

        ProgressBar.ProgressBarStyle pbarstyle = new ProgressBar.ProgressBarStyle();
        pbarstyle.background = skin.getDrawable("progress-bar-vertical");
        pbarstyle.knob = skin.getDrawable("progress-bar-knob-vertical");
        pbarstyle.knobBefore = skin.getDrawable("progress-bar-knob-vertical");

        strengthBar = new ProgressBar(0, maxMana,0.1f, true, pbarstyle);
        strengthBar.setAnimateDuration(0.25f);
        strengthBar.setValue(5);
        strengthBar.setProgrammaticChangeEvents(false);

        Table amount_table = new Table();
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.up = skin.getDrawable("button");
        textButtonStyle.down = skin.getDrawable("button-pressed");
        textButtonStyle.checked = skin.getDrawable("button-pressed");
        plusBtn = new TextButton("+",textButtonStyle);
        plusBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        ohBtn = new TextButton("O",textButtonStyle);
        ohBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        minusBtn = new TextButton("-",textButtonStyle);
        minusBtn.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                refreshTendency();
                return false;
            }
        });
        plusBtn.setChecked(true); /* Usage tendency is to give by default */
        amount_table.add(plusBtn).row();
        amount_table.add(ohBtn).row();
        amount_table.add(minusBtn).row();

        Table ether_dispay_table = new Table();
        netherImg = new Image(skin.getDrawable("nether"));
        aetherImg = new Image(skin.getDrawable("aether"));

        ether_dispay_table.add(netherImg).size(64,64).row();
        ether_dispay_table.add(aetherImg).size(64,64);

        addActor(strengthBar);
        addActor(amount_table);
        addActor(ether_dispay_table);
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
    public void accept_input(String name, float... parameters) {
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
            downTendecy();
        }else if(name.equals("manaModif")&&(1 == parameters.length)){
            modifyManaToUse(parameters[0]);
        }
    }

    private void refreshTendency(){
        if(Spells.SpellEtherTendency.GIVE == usageTendency){
            minusBtn.setChecked(false);
            ohBtn.setChecked(false);
            plusBtn.setChecked(true);
        }else if(Spells.SpellEtherTendency.EQUALIZE == usageTendency){
            minusBtn.setChecked(false);
            ohBtn.setChecked(true);
            plusBtn.setChecked(false);
        }else if(Spells.SpellEtherTendency.TAKE == usageTendency){
            minusBtn.setChecked(true);
            ohBtn.setChecked(false);
            plusBtn.setChecked(false);
        }
    }

    public void upTendency(){
        if(Spells.SpellEtherTendency.values().length-1 > usageTendency.ordinal())
            usageTendency = usageTendency.next();
        refreshTendency();
    }

    public void downTendecy(){
        if(0 < usageTendency.ordinal())
            usageTendency = usageTendency.previous();
        refreshTendency();
    }

    public float modifyManaToUse(float amount){
        float modif = Math.max(0.1f,Math.min(0.3f, (manaToUse / maxMana)));
        if(0 < -amount) {
            manaToUse = Math.min(Math.max(0.1f, manaToUse * (1+modif)), maxMana);
        } else {
            manaToUse = Math.min(Math.max(0.1f, manaToUse * (1-modif)), maxMana);
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
