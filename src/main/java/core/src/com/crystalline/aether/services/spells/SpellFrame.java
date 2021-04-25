package com.crystalline.aether.services.spells;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Material;
import com.crystalline.aether.models.spells.Spell;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.models.architecture.CapsuleService;
import com.crystalline.aether.models.architecture.DisplayService;
import com.crystalline.aether.models.architecture.Scene;
import com.crystalline.aether.services.utils.MathUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpellFrame extends CapsuleService implements DisplayService<Texture> {
    private final Config conf;
    private float maxUsedMana = 0.0f;
    private final int timeFrameIndex;
    private int activeTimeframe;

    /**
     * A hashmap for different actions, by coordinates
     */
    LinkedHashMap<Integer,SpellAction> actions = new LinkedHashMap<>();

    public SpellFrame(Scene parent, Config conf_, int timeFrameIndex){
        super(parent);
        conf = conf_;
        this.timeFrameIndex = timeFrameIndex;
    }

    public SpellAction[] getActions(){
        return actions.values().toArray(new SpellAction[]{});
    }

    public void clearActions(){
        actions.clear();
    }

    @Override
    public Texture getDisplay() {
        Pixmap actionImage = new Pixmap(conf.world_block_number[0],conf.world_block_number[1], Pixmap.Format.RGB888);
        actionImage.setBlending(Pixmap.Blending.SourceOver);
        actionImage.setColor(0,0,0,0);
        actionImage.fill();
        float newMaxUsedMana = 0;
        for(Map.Entry<Integer, SpellAction> entry : actions.entrySet()){
            int x = (int)entry.getValue().pos.x;
            int y = (conf.world_block_number[1] - 1 - (int)entry.getValue().pos.y);
            Color actionColor = Spell.getColorOf(entry.getValue(), maxUsedMana);
            actionImage.drawPixel(x, y, Color.rgba8888(actionColor));
            if(newMaxUsedMana < Math.abs(entry.getValue().usedAether))
                newMaxUsedMana = Math.abs(entry.getValue().usedAether);
            if(newMaxUsedMana < Math.abs(entry.getValue().usedNether))
                newMaxUsedMana = Math.abs(entry.getValue().usedNether);
        }
        maxUsedMana = newMaxUsedMana;
        Texture tex = new Texture(actionImage);
        actionImage.dispose();
        return tex;
    }

    @Override
    public void accept_input(String name, Object... parameters) {
        if(name.equals("setTimeFrame")&&(1 == parameters.length)){
            activeTimeframe = (int)parameters[0];
        }else if(
            name.equals("lastAction")&&(1 == parameters.length)
            &&(activeTimeframe == timeFrameIndex) /* Only collect actions in our own timeframe */
        ){
            SpellAction action = ((SpellAction)parameters[0]);
            int coordinateHash = MathUtils.coordinateToHash((int)action.pos.x, (int)action.pos.y, conf.world_block_number[0]);
            SpellAction storedAction = actions.get(coordinateHash);
            if(null != storedAction) { /* overwrite actions, pile ether values */
                storedAction.targetElement = action.targetElement;
                if(Material.Elements.Nothing == storedAction.targetElement){
                    storedAction.usedNether += action.usedNether;
                    storedAction.usedAether += action.usedAether;
                }else{ /* If an element is targeted, then overwrite new values */
                    storedAction.usedNether = action.usedNether;
                    storedAction.usedAether = action.usedAether;
                }
                storedAction.pos.set(action.pos);
                if(maxUsedMana < Math.abs(storedAction.usedAether))
                    maxUsedMana = Math.abs(storedAction.usedAether);
                if(maxUsedMana < Math.abs(storedAction.usedNether))
                    maxUsedMana = Math.abs(storedAction.usedNether);

            }else{
                actions.put(coordinateHash, new SpellAction(action));
                if(maxUsedMana < Math.abs(action.usedAether))
                    maxUsedMana = Math.abs(action.usedAether);
                if(maxUsedMana < Math.abs(action.usedNether))
                    maxUsedMana = Math.abs(action.usedNether);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        /* Nothing needed here yet.. */
    }

    @Override
    public void render() {
        throw new UnsupportedOperationException("Rendering not supported from this view!");
    }

    @Override
    public void calculate() {

    }

    @Override
    public void dispose() {
        actions.clear();
    }
}
