package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.Util;
import com.crystalline.aether.models.Materials;


/**TODO:
 *  - Heat gate
 *  - Speed gate ( to block or enchance the speed of objects )
 *  - Speed to be a vector like unit
 */
public class World {
    protected final int sizeX;
    protected final int sizeY;

    Ethereal_aspect ethereal_plane;
    Elemental_aspect elemental_plane;
    private final float [][] units;
    private final Vector2 [][] velocity;

    public World(int sizeX_, int sizeY_){
        sizeX = sizeX_;
        sizeY = sizeY_;
        units = new float[sizeX][sizeY];
        velocity = new Vector2[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                velocity[x][y] = new Vector2();
            }
        }
        ethereal_plane = new Ethereal_aspect(sizeX, sizeY);
        elemental_plane = new Elemental_aspect(sizeX, sizeY);
        ethereal_plane.determine_units(units,this);
    }

    public void pond_with_grill(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                velocity[x][y].set(0,0);
            }
        }
        elemental_plane.pond_with_grill(units,(int)(sizeY/2.0f));
        elemental_plane.determine_units(units, this);

        ethereal_plane.define_by(elemental_plane, units);
        ethereal_plane.determine_units(units,this);
    }

    public float avg_of_compatible(int x, int y, float[][] table){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
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

        Vector2 tmp_vec = velocity[to.get_i_x()][to.get_i_y()];
        velocity[to.get_i_x()][to.get_i_y()] = velocity[from.get_i_x()][from.get_i_y()];
        velocity[from.get_i_x()][from.get_i_y()] = tmp_vec;
    }

    public void main_loop(float step){
        /** ============= PROCESS UNITS ============= **/
        /* Ethereal plane decides the units */
        ethereal_plane.process_units(units, velocity,this);
        ethereal_plane.determine_units(units, this);

        /* Elemental plane decides types */
        elemental_plane.process_units(units, velocity, this);

        /** ============= PROCESS MECHANICS ============= **/
        /* Elemental calculates pressures and forces */
        elemental_plane.process_mechanics(units, velocity, this);
        ethereal_plane.process_mechanics(units, velocity, this); /* <-- This is currently empty.. */

        /** ============= PROCESS TYPES ============= **/
        elemental_plane.process_types(units, velocity, this);

        /* Ethereal tries to take over type changes from Elemental */
        ethereal_plane.process_types(units, velocity,this);

        /** ============= POST PROCESS ============= **/
        ethereal_plane.post_process(units, velocity, this);

        /* Elemental takes over finalised type changes from Ethereal */
        elemental_plane.post_process(units, velocity, this);
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
        elemental_plane.define_by(ethereal_plane);
        ethereal_plane.determine_units(units,this);
    }

    public float unit_at(int posX, int posY){
        return units[posX][posY];
    }
    public Vector2 get_velo(int posX, int posY){
        return velocity[posX][posY];
    }

    public Pixmap getWorldImage(Vector2 focus, float radius, Ethereal_aspect plane){
        Pixmap worldImage = new Pixmap(sizeX,sizeY, Pixmap.Format.RGB888);
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                float interpol = 0.3f;
                if(
//                        (1.0f >= Math.abs(x - focus.x))
//                        &&(1.0f >= Math.abs(y - focus.y))
                        (x == (int)focus.x) && (y == (int)focus.y)
                ) interpol = 0.0f;
                else if(
                        (radius >= Math.abs(x - focus.x))
                                &&(radius >= Math.abs(y - focus.y))
                ) interpol = 0.2f;
                Color finalColor = Materials.get_color(elemental_plane.element_at(x,y),units[x][y]).cpy().lerp(Color.GRAY, interpol);
                float hsvv[] = new float[3];
                finalColor.toHsv(hsvv);
                float sat = plane.aether_value_at(x,y) / Math.max(plane.aether_value_at(x,y),plane.nether_value_at(x,y));
                float val = plane.nether_value_at(x,y) / Math.max(plane.aether_value_at(x,y),plane.nether_value_at(x,y));
                //finalColor.fromHsv(hsvv[0], (sat+hsvv[1])/2.0f, (val+hsvv[2])/2.0f);
                finalColor.fromHsv(hsvv[0], hsvv[1], hsvv[2]);
                worldImage.drawPixel(
                        x,y, Color.rgba8888(finalColor)
                );
            }
        }
        return worldImage;
    }

}
