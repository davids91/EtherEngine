package com.crystalline.aether.services.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.architecture.DisplayService;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.models.spells.SpellFrame;
import com.crystalline.aether.services.utils.SpellUtil;

public class SpellFrameView implements DisplayService<Texture> {
    private final Config conf;
    private final SpellFrame target;
    public SpellFrameView(Config conf_, SpellFrame target_){
        conf = conf_;
        target = target_;
    }
    @Override
    public Texture getDisplay() {
        Pixmap actionImage = new Pixmap(conf.WORLD_BLOCK_NUMBER[0],conf.WORLD_BLOCK_NUMBER[1], Pixmap.Format.RGB888);
        actionImage.setBlending(Pixmap.Blending.SourceOver);
        actionImage.setColor(0,0,0,0);
        actionImage.fill();
        float newMaxUsedMana = 0;
        for(SpellAction action : target.getActions()){
            int x = (int)action.pos.x;
            int y = (conf.WORLD_BLOCK_NUMBER[1] - 1 - (int)action.pos.y);
            Color actionColor = SpellUtil.getColorOf(action, target.getMaxUsedMana());
            actionImage.drawPixel(x, y, Color.rgba8888(actionColor));
            if(newMaxUsedMana < Math.abs(action.usedAether))
                newMaxUsedMana = Math.abs(action.usedAether);
            if(newMaxUsedMana < Math.abs(action.usedNether))
                newMaxUsedMana = Math.abs(action.usedNether);
        }
        target.setMaxUsedMana(newMaxUsedMana);
        Texture tex = new Texture(actionImage);
        actionImage.dispose();
        return tex;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void render() {

    }

    @Override
    public void dispose() {

    }
}
