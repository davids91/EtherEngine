package com.crystalline.aether.services.capsules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.spells.Spell;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.services.utils.SpellUtil;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.world.EtherealAspect;
import com.crystalline.aether.models.architecture.CapsuleService;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.architecture.DisplayService;
import com.crystalline.aether.services.world.World;
import com.crystalline.aether.models.architecture.Scene;

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

    private final SpellAction spellAction;

    private Spell usedSpell;
    private boolean play = true;
    private boolean aetherActive = false;
    private boolean netherActive = false;
    private float manaToUse = 1;
    private SpellUtil.SpellEtherTendency tendency = SpellUtil.SpellEtherTendency.GIVE;
    private boolean doActions = true;
    private boolean collectActions = true;
    private boolean doSpell = false;
    private boolean broadcastActions = true;
    private int spellFrameCounter = 0;

    public WorldCapsule(Scene parent, Config conf_, World world_) throws Exception {
        super(parent);
        conf = conf_;
        spellAction = new SpellAction();
        world = world_;
        if( /* The size of the world should match the configuration! */
            (world.getSizeX() != conf.WORLD_BLOCK_NUMBER[0])
            ||(world.getSizeY() != conf.WORLD_BLOCK_NUMBER[1])
        ) throw new Exception("World size mismatch");
        camera.setToOrtho(false, width(), height());
        camera.update();
    }

    public WorldCapsule(Scene parent, Config conf_){
        super(parent);
        conf = conf_;
        spellAction = new SpellAction();
        world = new World(conf);
        camera.setToOrtho(false, width(), height());
        camera.update();
    }

    public void doSpell(Spell spell, int spellFrame, Vector3 offset){
        Vector3 actualOffset = new Vector3(offset);
        if(0 < actualOffset.len()){
            actualOffset.x -= spell.getSize().x/2;
            actualOffset.y -= spell.getSize().y/2;
        }
        doActions(actualOffset, spell.getFrame(spellFrame).getActions());
    }

    public void doActions(Vector3 offset, SpellAction... actions){
        for(SpellAction action : actions){
            world.doAction(action, offset);
        }
    }

    public void step(){
        world.mainLoop(0.01f);
    }

    public void reset(){
        world.reset();
    }

    @Override
    public void calculate() {
        if(netherActive||aetherActive){
            /* Compile the action to do */
            if(collectActions){
                if(SpellUtil.SpellEtherTendency.GIVE == tendency){
                    if(aetherActive)spellAction.usedAether += manaToUse;
                    else spellAction.usedAether = 0;
                    if(netherActive)spellAction.usedNether += manaToUse;
                    else spellAction.usedNether = 0;
                }else if(SpellUtil.SpellEtherTendency.TAKE == tendency){
                    if(aetherActive)spellAction.usedAether -= manaToUse;
                    else spellAction.usedAether = 0;
                    if(netherActive)spellAction.usedNether -= manaToUse;
                    else spellAction.usedNether = 0;
                }else if(SpellUtil.SpellEtherTendency.EQUALIZE == tendency){
                    spellAction.usedAether = EtherealAspect.getAetherDeltaToTargetRatio(
                            manaToUse,
                            world.getEtherealPlane().aetherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                            world.getEtherealPlane().netherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                            Material.ratioOf(spellAction.targetElement)
                    );
                    spellAction.usedNether = EtherealAspect.getNetherDeltaToTargetRatio(
                            manaToUse,
                            world.getEtherealPlane().aetherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                            world.getEtherealPlane().netherValueAt((int)spellAction.pos.x, (int)spellAction.pos.y),
                            Material.ratioOf(spellAction.targetElement)
                    ); /* TODO: Make editor punctual enough for ether crytals */
                }
            }
            /* applying simple actions */
            if(doActions){
                world.doAction(spellAction, MiscUtils.zeroVec);
            }
            if(broadcastActions)signal("lastAction", spellAction);

            /* applying spells */
            if(doSpell && (spellFrameCounter < usedSpell.getFrames().size())){
                doSpell(usedSpell, spellFrameCounter, spellAction.pos);
                ++spellFrameCounter;
            }
        }else{
            spellFrameCounter = 0;
        }
        if(play){
            world.mainLoop(0.01f);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(new TextureRegion(getDisplay()),0,0, width(), height());
        batch.end();
    }

    @Override
    public void acceptInput(String name, Object... parameters) {
        if(name.equals("initialize")&&(0 == parameters.length)){
            world.reset();
        }else if(name.equals("fill")&&(0 == parameters.length)){
            world.pondWithGrill();
        }else if(name.equals("step")){
            world.mainLoop(0.0f);
        }else if(name.equals("playPause")&&(0 == parameters.length)){
            play = !play;
        }else if(name.equals("mouseOnScreen2D")&&(2 == parameters.length)){
            spellAction.pos = camera.unproject(new Vector3((float)parameters[0], (float)parameters[1], 0));
        }else if(name.equals("netherActive")){
            netherActive = true;
        }else if(name.equals("netherInactive")){
            netherActive = false;
            if(collectActions)spellAction.usedNether = 0;
        }else if(name.equals("aetherActive")){
            aetherActive = true;
        }else if(name.equals("aetherInactive")){
            aetherActive = false;
            if(collectActions)spellAction.usedAether = 0;
        }else if(name.equals("transitionFrom:editor")&&(1 == parameters.length)){
            usedSpell = (Spell)(parameters[0]);
            if(null == usedSpell){
                setDoActions(true);
                setCollectActions(true);
                setDoSpell(false);
            }else{
                spellFrameCounter = 0;
                setDoActions(false);
                setCollectActions(false);
                setDoSpell(true);
            }
        }else if(collectActions){
            if(name.equals("manaToUse")&&(1 == parameters.length)){
                manaToUse = (float)parameters[0];
            }else if(name.equals("targetElement")&&(1 == parameters.length)){
                if(parameters[0] == Material.Elements.Nothing){
                    spellAction.targetElement = Material.Elements.Nothing;
                }else if(parameters[0] == Material.Elements.Earth){
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
            }else if(name.equals("tendencyTo")&&(1 == parameters.length)) {
                if (parameters[0] == SpellUtil.SpellEtherTendency.GIVE) {
                    tendency = SpellUtil.SpellEtherTendency.GIVE;
                } else if (parameters[0] == SpellUtil.SpellEtherTendency.EQUALIZE) {
                    tendency = SpellUtil.SpellEtherTendency.EQUALIZE;
                } else if (parameters[0] == SpellUtil.SpellEtherTendency.TAKE) {
                    tendency = SpellUtil.SpellEtherTendency.TAKE;
                }
            }
        }
    }

    public float width(){
        return conf.WORLD_BLOCK_NUMBER[0];
    }
    public float height(){
        return conf.WORLD_BLOCK_NUMBER[1];
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
    public Texture getDisplay() {
        Pixmap pxm = world.getWorldImage();
        Texture result = new Texture(pxm);
        pxm.dispose();
        return result;
    }

    public void setDoActions(boolean doActions) {
        this.doActions = doActions;
    }
    public void setBroadcastActions(boolean broadcastActions) {
        this.broadcastActions = broadcastActions;
    }
    public void setPlay(boolean play) {
        this.play = play;
    }
    public void setCollectActions(boolean collectActions) {
        this.collectActions = collectActions;
    }
    public void setDoSpell(boolean doSpell) {
        this.doSpell = doSpell;
    }

    public void setShowForces(boolean show){
        world.getElementalPlane().setShowForces(show);
    }
    public void setDebugViewPercent(float percent){
        world.getElementalPlane().setDebugViewPercent(percent);
    }

    public boolean isBroadcastActions() {
        return broadcastActions;
    }
}
