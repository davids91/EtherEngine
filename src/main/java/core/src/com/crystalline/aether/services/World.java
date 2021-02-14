package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.Util;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Materials;


/**TODO:
 *  - Heat gate
 *  - Speed gate ( to block or enchance the speed of objects )
 */
public class World {
    Config conf;
    protected final int sizeX;
    protected final int sizeY;

    Ethereal_aspect ethereal_plane;
    Elemental_aspect elemental_plane;
    private final float [][] units;

    public World(Config conf_){
        conf = conf_;
        sizeX = conf.world_block_number[0];
        sizeY = conf.world_block_number[1];
        units = new float[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = 0;
            }
        }
        ethereal_plane = new Ethereal_aspect(conf);
        elemental_plane = new Elemental_aspect(conf);
        ethereal_plane.determine_units(units,this);
    }

    public void pond_with_grill(){
        elemental_plane.pond_with_grill(units,(int)(sizeY/2.0f));
        elemental_plane.determine_units(units, this);

        ethereal_plane.define_by(elemental_plane, units);
        ethereal_plane.determine_units(units,this);
    }

    public float avg_of_compatible(int x, int y, float[][] table){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(
                    //(50.0f > Math.abs(units[x][y] - units[nx][ny])) /* Only reach out only for the same solidity */
                    (Materials.compatibility.get(elemental_plane.element_at(x,y)).contains(elemental_plane.element_at(nx,ny)))
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
        ethereal_plane.switch_values(from.get_i_x(),from.get_i_y(),to.get_i_x(),to.get_i_y());
        elemental_plane.switch_values(from.get_i_x(),from.get_i_y(),to.get_i_x(),to.get_i_y());

        float tmp_val = units[to.get_i_x()][to.get_i_y()];
        units[to.get_i_x()][to.get_i_y()] = units[from.get_i_x()][from.get_i_y()];
        units[from.get_i_x()][from.get_i_y()] = tmp_val;
    }

    public void main_loop(float step){
        /** ============= PROCESS UNITS ============= **/
        ethereal_plane.process_units(units,this);
        elemental_plane.process_units(units, this);

        /** ============= PROCESS MECHANICS ============= **/
        /* Elemental calculates pressures and forces */
        elemental_plane.process_mechanics(units, this);
        ethereal_plane.process_mechanics(units, this);

        /** ============= PROCESS TYPES ============= **/
        elemental_plane.process_types(units, this);
        ethereal_plane.process_types(units,this); /* Ethereal tries to take over type changes from Elemental */

        /** ============= POST PROCESS ============= **/
        ethereal_plane.post_process(units, this);

        /* Elemental takes over finalised type changes from Ethereal */
        elemental_plane.post_process(units, this);
    }

    public Ethereal_aspect get_eth_plane(){
        return ethereal_plane;
    }
    public Elemental_aspect get_elm_plane(){ return  elemental_plane; }
    public void add_aether_to(int x, int y, float value){
        ethereal_plane.add_aether_to(x,y,value);
        ethereal_plane.determine_units(units,this);
        elemental_plane.define_by(ethereal_plane);
    }

    public void add_nether_to(int x, int y, float value){
        ethereal_plane.add_nether_to(x,y,value);
        ethereal_plane.determine_units(units,this);
        elemental_plane.define_by(ethereal_plane);
    }

    public void try_to_equalize(int x, int y, float value) {
        ethereal_plane.try_to_equalize(x,y,value);
        ethereal_plane.determine_units(units,this);
        elemental_plane.define_by(ethereal_plane);
    }

    public float get_weight(int posX, int posY){
        return get_elm_plane().get_weight(posX,posY, units);
    }
    public float unit_at(int posX, int posY){
        return units[posX][posY];
    }
    public Pixmap getWorldImage(Vector2 focus, Ethereal_aspect plane){
        Pixmap worldImage = new Pixmap(sizeX,sizeY, Pixmap.Format.RGB888);
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                Color finalColor = elemental_plane.getColor(x,(sizeY - 1 - y),units);
                worldImage.drawPixel(x,y, Color.rgba8888(finalColor));
            }
        }
        return worldImage;
    }

}
