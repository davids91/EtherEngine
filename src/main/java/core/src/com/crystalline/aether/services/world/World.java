package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.services.utils.MiscUtil;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;


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
    private final int[][] units;

    public World(Config conf_){
        conf = conf_;
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        units = new int[sizeX][sizeY];
        etherealPlane = new EtherealAspect(conf);
        elementalPlane = new ElementalAspect(conf);
        etherealPlane.determineUnits(units,this);
        reset();
    }

    public void reset(){
        etherealPlane.reset();
        elementalPlane.reset();
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = 0;
            }
        }

    }

    public void pondWithGrill(){
        elementalPlane.pondWithGrill(units,(int)(sizeY/2.0f));
        elementalPlane.determineUnits(units, this);

        etherealPlane.defineBy(elementalPlane, units);
        etherealPlane.determineUnits(units,this);
    }

    public float avgOfCompatible(int x, int y, float[][] table){
        float averageVal = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(
                    //(50.0f > Math.abs(units[x][y] - units[nx][ny])) /* Only reach out only for the same solidity */
                    (Material.compatibility.get(elementalPlane.elementAt(x,y)).contains(elementalPlane.elementAt(nx,ny)))
                ){
                    averageVal += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)averageVal /= division;
        return averageVal;
    }

    public void switchElements(MiscUtil.MyCell from, MiscUtil.MyCell to){
        etherealPlane.switchValues(from.getIX(),from.getIY(),to.getIX(),to.getIY());
        elementalPlane.switchValues(from.getIX(),from.getIY(),to.getIX(),to.getIY());

        int tmp_val = units[to.getIX()][to.getIY()];
        units[to.getIX()][to.getIY()] = units[from.getIX()][from.getIY()];
        units[from.getIX()][from.getIY()] = tmp_val;
    }

    public void mainLoop(float step){
        /* ============= PROCESS UNITS ============= */
        etherealPlane.processUnits(units,this);
        elementalPlane.processUnits(units, this);

        /* ============= PROCESS MECHANICS ============= */
        /* Elemental calculates pressures and forces */
        elementalPlane.processMechanics(units, this);
        etherealPlane.processMechanics(units, this);

        /* ============= PROCESS TYPES ============= */
        elementalPlane.processTypes(units, this);
        etherealPlane.processTypes(units,this); /* Ethereal tries to take over type changes from Elemental */

        /* ============= POST PROCESS ============= */
        etherealPlane.postProcess(units, this);

        /* Elemental takes over finalised type changes from Ethereal */
        elementalPlane.postProcess(units, this);
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
//                addAetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedAether);
//            if(action.netherActive())
//                addNetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedNether);
//            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
//                getEtherealPlane().setTargetRatio(
//                        (int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), Material.ratioOf(action.targetElement)
//                );
//                getElementalPlane().setElement(
//                        (int)(action.pos.x + offset.x), (int)(action.pos.y + offset.y), action.targetElement
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
    private void addAetherTo(int x, int y, int value){
        etherealPlane.addAetherTo(x,y,value);
        etherealPlane.determineUnits(units,this);
        elementalPlane.defineBy(etherealPlane);
    }

    private void addNetherTo(int x, int y, int value){
        etherealPlane.addNetherTo(x,y,value);
        etherealPlane.determineUnits(units,this);
        elementalPlane.defineBy(etherealPlane);
    }

    private void tryToEqualize(int x, int y, int aetherToUse, int netherToUse, Material.Elements target) {
        etherealPlane.tryToEqualize(x,y,aetherToUse,netherToUse, Material.netherRatios[target.ordinal()]);
        etherealPlane.determineUnits(units,this);
        elementalPlane.defineBy(etherealPlane);
    }

    public Pixmap getWorldImage(){
        Pixmap worldImage = new Pixmap(sizeX,sizeY, Pixmap.Format.RGB888);
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                Color finalColor;
                finalColor = elementalPlane.getColor(x,(sizeY - 1 - y),units);
//                finalColor = elementalPlane.getDebugColor(x,(sizeY - 1 - y),units);
                worldImage.drawPixel(x,y, Color.rgba8888(finalColor));
            }
        }
        return worldImage;
    }

}
