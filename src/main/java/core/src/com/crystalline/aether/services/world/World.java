package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**TODO:
 *  - Heat gate
 *  - Speed gate ( to block or enchance the speed of objects )
 */
public class World {
    Config conf;

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    protected final int sizeX;
    protected final int sizeY;

    EtherealAspect etherealPlane;
    ElementalAspect elementalPlane;

    /**
     * A texture image representing the different scalars of reality
     * - R: units : determines mass
     * - G: not used yet..
     * - B: not used yet..
     * - A: not used yet..
     */
    private final FloatBuffer scalars;

    public World(Config conf_){
        conf = conf_;
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        scalars = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        etherealPlane = new EtherealAspect(conf);
        elementalPlane = new ElementalAspect(conf);
        etherealPlane.determineUnits(this);
        reset();
    }

    public void reset(){
        etherealPlane.reset();
        elementalPlane.reset();
        elementalPlane.defineBy(etherealPlane);
        etherealPlane.determineUnits(this);
    }

    public void pondWithGrill(){
        elementalPlane.pondWithGrill(this,(int)(sizeY/2.0f));
        elementalPlane.determineUnits(this);

        etherealPlane.defineBy(elementalPlane, scalars);
        etherealPlane.determineUnits(this);
    }

    public void switchElements(MiscUtils.MyCell from, MiscUtils.MyCell to){
        etherealPlane.switchValues(from.getIX(),from.getIY(),to.getIX(),to.getIY());
        elementalPlane.switchValues(from.getIX(),from.getIY(),to.getIX(),to.getIY());
        float tmpVal = getUnit(to.getIX(),to.getIY());
        setUnit(to.getIX(),to.getIY(), getUnit(from.getIX(),from.getIY()));
        setUnit(from.getIX(),from.getIY(), tmpVal);
    }

    public void mainLoop(float step){
        elementalPlane.debugMeasure(this);
        /* ============= PROCESS UNITS ============= */
        etherealPlane.processUnits(this);
        elementalPlane.processUnits(this);

        /* ============= PROCESS MECHANICS ============= */
        /* Elemental calculates pressures and forces */
        elementalPlane.processMechanics(this);
        etherealPlane.processMechanics(this);

        /* ============= PROCESS TYPES ============= */
        elementalPlane.processTypes(this);
        etherealPlane.processTypes(this); /* Ethereal tries to take over type changes from Elemental */

        /* ============= POST PROCESS ============= */
        etherealPlane.postProcess(this);
        elementalPlane.postProcess(this); /* Elemental takes over finalised type changes from Ethereal */
        elementalPlane.debugPrint();
    }

    public void pushState(){
        etherealPlane.pushState();
        elementalPlane.pushState();
    }

    public void popState(){
        etherealPlane.popState();
        elementalPlane.popState();
    }

    public EtherealAspect getEtherealPlane(){
        return etherealPlane;
    }
    public ElementalAspect getElementalPlane(){ return elementalPlane; }

    public void doAction(SpellAction action, Vector3 offset){
        if(action.active()){
            /* not very punctual... */ /* TODO: Make Spells of different levels; Lower level mages shall use the less punctual algorithm */
//            if(action.aetherActive())
//                addAetherTo((action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedAether);
//            if(action.netherActive())
//                addNetherTo((action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedNether);
//            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
//                getEtherealPlane().setTargetRatio(
//                        (action.pos.x + offset.x),(int)(action.pos.y + offset.y), Material.ratioOf(action.targetElement)
//                );
//                getElementalPlane().setElement(
//                        (action.pos.x + offset.x), (int)(action.pos.y + offset.y), action.targetElement
//                );
//            }

            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
                tryToEqualize(
                    (int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y),
                    action.usedAether, action.usedNether, action.targetElement
                );
            }else{
                if(action.aetherActive())
                    addAetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedAether);
                if(action.netherActive())
                    addNetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedNether);
            }
        }
    }

    public float getUnit(int x, int y) {
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, scalars);
    }
    public void setUnit(int x, int y, float value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0, scalars, value);
    }
    public void offsetUnit(int x, int y, float value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0, scalars, (value + getUnit(x,y)));
    }
    private void addAetherTo(int x, int y, float value){
        etherealPlane.addAetherTo(x,y,value);
        etherealPlane.determineUnits(this);
    }
    private void addNetherTo(int x, int y, float value){
        etherealPlane.addNetherTo(x,y,value);
        etherealPlane.determineUnits(this);
    }
    private void tryToEqualize(int x, int y, float aetherToUse, float netherToUse, Material.Elements target) {
        etherealPlane.tryToEqualize(x,y,aetherToUse,netherToUse, Material.ratioOf(target));
        etherealPlane.determineUnits(this);
        elementalPlane.defineBy(etherealPlane);
    }

    public Pixmap getWorldImage(){
        Pixmap worldImage = new Pixmap(sizeX,sizeY, Pixmap.Format.RGB888);
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                Color finalColor;
//                finalColor = elementalPlane.getColor(x,(sizeY - 1 - y),units);
                finalColor = elementalPlane.getDebugColor(x,(sizeY - 1 - y), this);
                worldImage.drawPixel(x,y, Color.rgba8888(finalColor));
            }
        }
        return worldImage;
    }

}
