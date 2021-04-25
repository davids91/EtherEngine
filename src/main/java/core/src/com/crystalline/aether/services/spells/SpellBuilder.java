package com.crystalline.aether.services.spells;

import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.architecture.CapsuleService;
import com.crystalline.aether.models.architecture.Scene;
import com.crystalline.aether.models.spells.Spell;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.services.utils.MathUtils;

public class SpellBuilder extends CapsuleService {
    private final Config conf;
    private final Vector3 spellSize;
    private final Spell resultSpell;

    private int activeTimeframe = 0;

    public SpellBuilder(Scene parent, Config conf_, Vector3 spellSize_){
        super(parent);
        conf = conf_;
        resultSpell = new Spell(spellSize_);
        spellSize = spellSize_;
    }

    public void setFrameNumber(int frameNumber){
        int elementsToAdd = frameNumber - resultSpell.getFrames().size();
        if(0 > elementsToAdd){
            for(int i = 0; i < -elementsToAdd; ++i)
                resultSpell.removeLastFrame();
        }else{
            for(int i = 0; i < elementsToAdd; ++i)
                resultSpell.addFrame();
        }
    }

    public Spell getCurrentSpell(){
        return resultSpell;
    }

    @Override
    public void calculate() {

    }

    @Override
    public void acceptInput(String name, Object... parameters) {
        if(name.equals("setTimeFrame")&&(1 == parameters.length)){
            activeTimeframe = (int)parameters[0];
        }else if(name.equals("lastAction")&&(1 == parameters.length)){
            SpellAction action = ((SpellAction)parameters[0]);
            int coordinateHash = MathUtils.coordinateToHash((int)action.pos.x, (int)action.pos.y, conf.WORLD_BLOCK_NUMBER[0]);
            getCurrentSpell().getFrame(activeTimeframe).addAction(action, coordinateHash);
        }
    }

    @Override
    public void dispose() {

    }
}
