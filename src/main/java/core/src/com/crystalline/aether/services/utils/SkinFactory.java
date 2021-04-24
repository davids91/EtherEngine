package com.crystalline.aether.services.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class SkinFactory {
    public static Skin getDefaultSkin(){
        TextureAtlas ui_atlas = new TextureAtlas(Gdx.files.internal("skins/default/neutralizer-ui.atlas"));
        TextureAtlas nether_atlas = new TextureAtlas(Gdx.files.internal("atlases/Nether.atlas"));
        BitmapFont font = new BitmapFont(Gdx.files.internal("skins/default/font-export.fnt"), ui_atlas.findRegion("font-export"));
        Skin skin = new Skin();
        skin.addRegions(nether_atlas);
        skin.addRegions(ui_atlas);
        skin.add("default-font",font, BitmapFont.class);
        return skin;
    }
}
