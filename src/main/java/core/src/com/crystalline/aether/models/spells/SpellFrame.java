package com.crystalline.aether.models.spells;

import com.crystalline.aether.models.world.Material;

import java.util.LinkedHashMap;

public class SpellFrame{
    private float maxUsedMana = 0.0f;

    /**
     * A hashmap for different actions, by coordinates
     */
    LinkedHashMap<Integer,SpellAction> actions = new LinkedHashMap<>();

    public SpellAction[] getActions(){
        return actions.values().toArray(new SpellAction[]{});
    }

    public SpellAction getAction(Integer hash){
        return actions.get(hash);
    }

    public void addAction(SpellAction action, Integer hash){
        SpellAction storedAction = getAction(hash);
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
            actions.put(hash, new SpellAction(action));
            if(maxUsedMana < Math.abs(action.usedAether))
                maxUsedMana = Math.abs(action.usedAether);
            if(maxUsedMana < Math.abs(action.usedNether))
                maxUsedMana = Math.abs(action.usedNether);
        }
    }

    public void clearActions(){
        actions.clear();
    }

    public float getMaxUsedMana() {
        return maxUsedMana;
    }

    public void setMaxUsedMana(float maxUsedMana) {
        this.maxUsedMana = maxUsedMana;
    }
}
