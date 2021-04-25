package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.services.utils.Util;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Material;


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
    private final float [][] units;

    public World(Config conf_){
        conf = conf_;
        sizeX = conf.world_block_number[0];
        sizeY = conf.world_block_number[1];
        units = new float[sizeX][sizeY];
        etherealPlane = new EtherealAspect(conf);
        elementalPlane = new ElementalAspect(conf);
        etherealPlane.determine_units(units,this);
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

    public void pond_with_grill(){
        elementalPlane.pond_with_grill(units,(int)(sizeY/2.0f));
        elementalPlane.determine_units(units, this);

        etherealPlane.define_by(elementalPlane, units);
        etherealPlane.determine_units(units,this);
    }

    public float avg_of_compatible(int x, int y, float[][] table){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(
                    //(50.0f > Math.abs(units[x][y] - units[nx][ny])) /* Only reach out only for the same solidity */
                    (Material.compatibility.get(elementalPlane.elementAt(x,y)).contains(elementalPlane.elementAt(nx,ny)))
                ){
                    average_val += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    public void switch_elements(Util.MyCell from, Util.MyCell to){
        etherealPlane.switch_values(from.get_i_x(),from.get_i_y(),to.get_i_x(),to.get_i_y());
        elementalPlane.switch_values(from.get_i_x(),from.get_i_y(),to.get_i_x(),to.get_i_y());

        float tmp_val = units[to.get_i_x()][to.get_i_y()];
        units[to.get_i_x()][to.get_i_y()] = units[from.get_i_x()][from.get_i_y()];
        units[from.get_i_x()][from.get_i_y()] = tmp_val;
    }

    public void main_loop(float step){
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

    public void doAction(SpellAction action){
        if(action.active()){
            if(action.aetherActive())
                addAetherTo((int)action.pos.x,(int)action.pos.y, action.usedAether);
            if(action.netherActive())
                addNetherTo((int)action.pos.x,(int)action.pos.y, action.usedNether);
            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
                getEtherealPlane().setTargetRatio(
                    (int)action.pos.x,(int)action.pos.y, Material.ratioOf(action.targetElement)
                );
                getElementalPlane().setElement(
                    (int)action.pos.x, (int)action.pos.y, action.targetElement
                );
            }
        }
    }
    private void addAetherTo(int x, int y, float value){
        etherealPlane.addAetherTo(x,y,value);
        etherealPlane.determine_units(units,this);
        elementalPlane.define_by(etherealPlane);
    }

    private void addNetherTo(int x, int y, float value){
        etherealPlane.addNetherTo(x,y,value);
        etherealPlane.determine_units(units,this);
        elementalPlane.define_by(etherealPlane);
    }

    private void tryToEqualize(int x, int y, float aetherToUse, float netherToUse, Material.Elements target) {
        etherealPlane.tryToEqualize(x,y,aetherToUse,netherToUse, Material.netherRatios[target.ordinal()]);
        etherealPlane.determine_units(units,this);
        elementalPlane.define_by(etherealPlane);
    }

    public float get_weight(int posX, int posY){
        return getElementalPlane().get_weight(posX,posY, units);
    }
    public float unit_at(int posX, int posY){
        return units[posX][posY];
    }
    public Pixmap getWorldImage(){
        Pixmap worldImage = new Pixmap(sizeX,sizeY, Pixmap.Format.RGB888);
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                Color finalColor;
                finalColor = elementalPlane.getColor(x,(sizeY - 1 - y),units);
                worldImage.drawPixel(x,y, Color.rgba8888(finalColor));
            }
        }
        return worldImage;
    }

}
