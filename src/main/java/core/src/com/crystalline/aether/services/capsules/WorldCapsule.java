package com.crystalline.aether.services.capsules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.Material;
import com.crystalline.aether.models.Spell;
import com.crystalline.aether.services.EtherealAspect;
import com.crystalline.aether.services.architecture.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.architecture.DisplayService;
import com.crystalline.aether.services.World;
import com.crystalline.aether.services.scenes.Scene;

/** TODO:
 * - Create Different Views for the world as different DisplayServices
 * - Display heat and light
 * - Heat and cold to be counted in para-effect plane( ? )
 * - Create Ether crystals: it's not a target ratio, only when the ratio is already at that.
 * - "running" indicator
 * - debug panel to show pixel in focus
 * - Debug NANs
 * - push a to b  ( material merges )
 * - typechange conflicts --> especially handle water to disappear when lava is near
 * - "Move together" ?
 */

/**
 * Handles the creation and concatenation of possibly multiple worlds.
 */
public class WorldCapsule extends CapsuleService implements DisplayService<Texture> {
    private final Config conf;
    private final SpriteBatch batch = new SpriteBatch();
    private final OrthographicCamera camera = new OrthographicCamera();
    private final World world;

    private final Spell.Action spellAction;
    private boolean play = true;
    private boolean aetherActive = false;
    private boolean netherActive = false;
    private float manaToUse = 0.1f;
    private Spell.SpellEtherTendency tendency = Spell.SpellEtherTendency.GIVE;
    private boolean doActions = true;

    public WorldCapsule(Scene parent, Config conf_, World world_) throws Exception {
        super(parent);
        conf = conf_;
        spellAction = new Spell.Action();
        world = world_;
        if( /* The size of the world should match the configuration! */
            (world.getSizeX() != conf.world_block_number[0])
            ||(world.getSizeY() != conf.world_block_number[1])
        ) throw new Exception("World size mismatch");
        camera.setToOrtho(false, width(), height());
        camera.update();
    }

    public WorldCapsule(Scene parent, Config conf_){
        super(parent);
        conf = conf_;
        spellAction = new Spell.Action();
        world = new World(conf);
        camera.setToOrtho(false, width(), height());
        camera.update();
    }

    public void doActions(Spell.Action... actions){
        for(Spell.Action action : actions){
            world.doAction(action);
        }
    }

    public void reset() {
        world.reset();
    }

    @Override
    public void calculate() {
        if(netherActive||aetherActive){
            /* Compile the action to do */
            if(Spell.SpellEtherTendency.GIVE == tendency){
                if(aetherActive)spellAction.usedAether = manaToUse;
                else spellAction.usedAether = 0.0f;
                if(netherActive)spellAction.usedNether = manaToUse;
                else spellAction.usedNether = 0.0f;
            }else if(Spell.SpellEtherTendency.TAKE == tendency){
                if(aetherActive)spellAction.usedAether = -manaToUse;
                else spellAction.usedAether = 0.0f;
                if(netherActive)spellAction.usedNether = -manaToUse;
                else spellAction.usedNether = 0.0f;
            }else if(Spell.SpellEtherTendency.EQUALIZE == tendency){
                spellAction.usedAether = EtherealAspect.getAetherDeltaToTargetRatio(
                    manaToUse,
                    world.getEtherealPlane().aetherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                    world.getEtherealPlane().netherValueAt((int)spellAction.pos.x,(int)spellAction.pos.y),
                    Material.netherRatios[spellAction.targetElement.ordinal()]
                );
                spellAction.usedNether = EtherealAspect.getNetherDeltaToTargetRatio(
                    manaToUse,
                    world.getEtherealPlane().aetherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                    world.getEtherealPlane().netherValueAt((int)spellAction.pos.x,(int)spellAction.pos.y),
                    Material.netherRatios[spellAction.targetElement.ordinal()]
                );

                System.out.println("ratio: " +
                (
                (world.getEtherealPlane().netherValueAt((int)spellAction.pos.x,(int)spellAction.pos.y) + spellAction.usedNether)
                / (world.getEtherealPlane().aetherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y) + spellAction.usedAether)
                )
                +"/" + Material.netherRatios[spellAction.targetElement.ordinal()]
                );
            }
            /* apply the action */
            if(doActions)world.doAction(spellAction);
            signal("lastAction", spellAction);
        }
        if(play){
            world.main_loop(0.01f);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(new TextureRegion(get_display()),0,0, width(), height());
        batch.end();
    }

    @Override
    public void accept_input(String name, Object... parameters) {
        if(name.equals("initialize")&&(0 == parameters.length)){
            world.pond_with_grill();
        }else if(name.equals("stop")&&(0 == parameters.length)){
            play = false;
        }else if(name.equals("playPause")&&(0 == parameters.length)){
            play = !play;
        }else if(name.equals("step")&&(0 == parameters.length)){
            world.main_loop(0.01f); /* TODO: Make this part of the "calculate"  */
        }else if(name.equals("mouseOnScreen2D")&&(2 == parameters.length)){
            spellAction.pos = camera.unproject(new Vector3((float)parameters[0], (float)parameters[1], 0.0f));
        }else if(name.equals("netherActive")){
            netherActive = true;
        }else if(name.equals("netherInactive")){
            netherActive = false;
        }else if(name.equals("aetherActive")){
            aetherActive = true;
        }else if(name.equals("aetherInactive")){
            aetherActive = false;
        }else if(name.equals("manaToUse")&&(1 == parameters.length)){
            manaToUse = (float)parameters[0];
        }else if(name.equals("targetElement")&&(1 == parameters.length)){
            if(parameters[0] == Material.Elements.Earth){
                spellAction.targetElement = Material.Elements.Earth;
            }else if(parameters[0] == Material.Elements.Water){
                spellAction.targetElement = Material.Elements.Water;
            }else if(parameters[0] == Material.Elements.Air){
                spellAction.targetElement = Material.Elements.Air;
            }else if(parameters[0] == Material.Elements.Fire){
                spellAction.targetElement = Material.Elements.Fire;
            }else if(parameters[0] == Material.Elements.Ether){
                spellAction.targetElement = Material.Elements.Ether;
            }
        }else if(name.equals("tendencyTo")&&(1 == parameters.length)){
            if(parameters[0] == Spell.SpellEtherTendency.GIVE){
                tendency = Spell.SpellEtherTendency.GIVE;
            }else if(parameters[0] == Spell.SpellEtherTendency.EQUALIZE){
                tendency = Spell.SpellEtherTendency.EQUALIZE;
            }else if(parameters[0] == Spell.SpellEtherTendency.TAKE){
                tendency = Spell.SpellEtherTendency.TAKE;
            }
        }else if(name.equals("doActionsOn")){
            doActions = true;
        }else if(name.equals("doActionsOff")){
            doActions = false;
        }
    }

    public float width(){
        return conf.world_block_number[0];
    }
    public float height(){
        return conf.world_block_number[1];
    }

    @Override
    public void dispose() {
        batch.dispose();
    }


    @Override
    public void resize(int width, int height) {
        /* Nothing needed here yet */
    }

    @Override
    public Texture get_display() {
        Pixmap pxm = world.getWorldImage();
        Texture result = new Texture(pxm);
        pxm.dispose();
        return result;
    }
}
