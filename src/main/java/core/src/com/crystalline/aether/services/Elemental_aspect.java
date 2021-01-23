package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.models.Reality_aspect;
import com.crystalline.aether.Util;

import java.util.*;

/**TODO:
 * - TODO: Make Air catch Flame
 */
public class Elemental_aspect implements Reality_aspect {
    protected final int sizeX;
    protected final int sizeY;
    Materials.Names[][] blocks;

    private final Random rnd = new Random();

    public Elemental_aspect(int sizeX_, int sizeY_){
        sizeX = sizeX_;
        sizeY = sizeY_;
        blocks = new Materials.Names[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = Materials.Names.Air;
            }
        }
    }

    public void define_by(Ethereal_aspect plane){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = plane.element_at(x,y);
            }
        }
    }

    private float avg_of_block(int x, int y, float[][] table, Materials.Names type){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                if(blocks[nx][ny] == type){
                    average_val += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private float avg_of_block_within_distance(int x, int y, float[][] table, Materials.Names type, float distance){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                if((blocks[nx][ny] == type) && (distance > Math.abs(table[x][y] - table[nx][ny]))){
                    average_val += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private Materials.Names dominant_element_at(int x, int y, float[][] table){
        float cnt[] = new float[Materials.Names.values().length];
        Materials.Names dominant = Materials.Names.Air;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                for(Materials.Names mat : Materials.Names.values()){
                    if(blocks[x][y] == mat){
                        cnt[mat.ordinal()] += table[x][y];
                        if(cnt[mat.ordinal()] > cnt[dominant.ordinal()])
                            dominant = mat;
                    }
                }
            }
        }
        return dominant;
    }

    @Override
    public void determine_units(float[][] units, World parent) {

    }

    @Override
    public void switch_values(int fromX, int fromY, int toX, int toY) {
        Materials.Names tmp_bloc = blocks[toX][toY];
        blocks[toX][toY] = blocks[fromX][fromY];
        blocks[fromX][fromY] = tmp_bloc;
    }

    @Override
    public void process_units(float[][] units, Vector2[][] velocity, World parent){
        float[][] avgs = new float[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                avgs[x][y] = avg_of_block_within_distance(x,y,units,blocks[x][y], 10.0f);
            }
        }

        for(int x = 0;x < sizeX; ++x) { /* Calculate dilution */
            for (int y = 0; y < sizeY; ++y) {
                units[x][y] = avgs[x][y];
            }
        }
    }

    @Override
    public void process_types(float[][] units, Vector2[][] velocity, World parent) {
        for(int x = sizeX - 1;x < 0; --x){
            for(int y = sizeY - 1 ; y < 0; --y) {
                blocks[x][y] = parent.ethereal_plane.element_at(x,y);

                /* TODO: Move averages to before the step for consistent behavior for context dependent stuff */
                if(Materials.Names.Water == blocks[x][y]){ /* TODO: This will be ill-defined in a multi-threaded environment */
                    if(avg_of_block(x,y,units,Materials.Names.Water) < avg_of_block(x,y,units,Materials.Names.Fire)){ /* TODO: Make Air catch Flame */
                        blocks[x][y] = Materials.Names.Air;
                    }
                    if(blocks[x][y-1] == Materials.Names.Air){
                        // switch the 2 elements
                    }
                }

                if(Materials.Names.Air == blocks[x][y]){

                }

                if(Materials.Names.Fire == blocks[x][y]){
                    if(avg_of_block(x,y,units,Materials.Names.Water) >= avg_of_block(x,y,units, Materials.Names.Water)){
                        blocks[x][y] = Materials.Names.Earth;
                    }
                    if(2.0f > units[x][y]){
                        blocks[x][y] = Materials.Names.Air;
                    }
                }

                if(Materials.Names.Earth == blocks[x][y]){
                    if(00.f < avg_of_block(x,y,units, Materials.Names.Fire)){
                        if(avg_of_block(x,y,units, Materials.Names.Earth) < avg_of_block(x,y,units, Materials.Names.Fire)){
                            blocks[x][y] = Materials.Names.Fire;
                        }else units[x][y] *= 0.95f;
                    }
                }
            }
        }

    }

    private float get_weight(int x, int y, float[][] units){
        /* TODO: Weight to include pressure somehow? or at least the same materials on top */
        return (units[x][y] * Materials.type_pressure_scales[blocks[x][y].ordinal()][1]);
    }

    @Override
    public void process_mechanics(float[][] units, Vector2[][] velocity, World parent) {
        HashMap<Integer, Integer> surface_block_depths = new HashMap<>();
        int surface_bloc_depth;
        int loop_var;
        float[][] pressure = new float[sizeX][sizeY];
        for(int x = 1; x < sizeX-1; ++x){ /* TODO: Do we really need this? */
            pressure[x][sizeY-1] = units[x][sizeY-1];
        }
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y >= 0; --y){
                if(Materials.movable(blocks[x][y+1], units[x][y+1])){
                    pressure[x][y] = pressure[x][y+1];
                    pressure[x][y] += units[x][y+1];
                }
                if(blocks[x+1][y] == blocks[x][y]){
                    pressure[x][y] += units[x+1][y];
                }
                if(blocks[x-1][y] == blocks[x][y]){
                    pressure[x][y] += units[x-1][y];
                }
                velocity[x][y].add(Util.gravity);
                velocity[x][y].y -= pressure[x][y];
                if(
                    Materials.movable(blocks[x][y], units[x][y])
                    &&((blocks[x][y] != blocks[x+1][y])||(blocks[x][y] != blocks[x-1][y]))
                ){
                    surface_bloc_depth = 0;
                    loop_var = 0;
                    while( /* look to the left for surfaces*/
                        (loop_var < 5) &&(0 < (y - loop_var))
                    ){
                        if(blocks[x][y] == blocks[x][(y - loop_var)]){
                            ++surface_bloc_depth;
                        }
                        ++loop_var;
                    }
                    surface_block_depths.put(Util.coordinate_to_hash(x,y,sizeX),surface_bloc_depth);
                }
            }
        }

        /* TODO: First come first served to be replaced by a better system */
        HashMap<Util.MyCell, Util.MyCell> proposed_changes = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
//        for(int x = 1; x < sizeX-1; ++x){
//            for(int y = 1; y < sizeY-1; ++y){
        for(int x = sizeX-2; x > 0; --x){
            for(int y = sizeY-2; y > 0; --y){
                if( /* see if the material can fall */
                    (blocks[x][y] != Materials.Names.Air) /* If the current material is not air */
                    &&(!Materials.is_same_mat(x,y,x,y-1,blocks, units)) /* but only if the block below them is the same */
                    /* The top one is lighter */
                    &&(get_weight(x,y-1,units) < get_weight(x,y,units)) /* TODO: speed (?) Threshold for start of movement */
                    /* and the cell, and the cell below is movable */
                    &&(Materials.movable(blocks[x][y], units[x][y]) && Materials.movable(blocks[x][y-1], units[x][y-1]))
                    /* and they are not touched previously */
                    &&(!already_changed.contains(Util.coordinate_to_hash(x,y,sizeX)))
                    &&(!already_changed.contains(Util.coordinate_to_hash(x,y-1,sizeX)))
                ){
                    proposed_changes.put(new Util.MyCell(x,y,sizeX),new Util.MyCell(x,y-1,sizeX)); /* propose to switch the 2 */
                    already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                    already_changed.add(Util.coordinate_to_hash(x,y-1,sizeX));
                }else if( /* If it's not falling, it might crawl to the side */
                    (blocks[x][y] != Materials.Names.Air) /* If the current material is not air */
                    &&Materials.movable(blocks[x][y], units[x][y]) /* and movable */
                    &&(Materials.is_same_mat(x,y,x,y-1,blocks, units)) /* but only if the block below them is the same */
                    &&(
                        Materials.get_if_unstable_by_pressure(blocks[x][y], pressure[x][y])
                        &&(surface_block_depths.containsKey(Util.coordinate_to_hash(x,y,sizeX)))
                        &&(surface_block_depths.get(Util.coordinate_to_hash(x,y,sizeX)) >= 1)
                    )
                ){
                    /* decide the direction of the water */
                    int go_left = 0;
                    loop_var = 0;
                    while( /* look to the left for surfaces*/
                            (loop_var < 6) &&(0 < (x - loop_var))
                    ){
                        if(blocks[x][y] == blocks[x - loop_var][y]){
                            --go_left;
                        }
                        ++loop_var;
                    }

                    loop_var = 0;
                    while( /* look to the left for surfaces*/
                            (loop_var < 6) &&(sizeX > (x + loop_var))
                    ){
                        if(blocks[x][y] == blocks[x + loop_var][y]){
                            ++go_left;
                        }
                        ++loop_var;
                    }

                    if(
                        (get_weight(x-1,y,units) <= get_weight(x,y,units))
                        &&(0 < go_left)
                        &&(get_weight(x-1,y,units) <= get_weight(x+1,y,units))
                        &&Materials.movable(blocks[x-1][y], units[x-1][y])
                        &&(!Materials.is_same_mat(x,y,x-1,y,blocks, units))
                        &&(!already_changed.contains(Util.coordinate_to_hash(x-1,y,sizeX)))
                    ){
                        proposed_changes.put(new Util.MyCell(x,y,sizeX),new Util.MyCell(x-1,y,sizeX)); /* propose to switch the 2 */
                        already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                        already_changed.add(Util.coordinate_to_hash(x-1,y,sizeX));
                    }else if(
                        (Materials.movable(blocks[x+1][y], units[x+1][y]))
                        &&(!Materials.is_same_mat(x,y,x+1,y,blocks, units))
                        &&(get_weight(x+1,y,units) <= get_weight(x,y,units))
                        &&(!already_changed.contains(Util.coordinate_to_hash(x+1,y,sizeX)))
                    ){
                        proposed_changes.put(new Util.MyCell(x,y,sizeX),new Util.MyCell(x+1,y,sizeX)); /* propose to switch the 2 */
                        already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                        already_changed.add(Util.coordinate_to_hash(x+1,y,sizeX));
                    }
                }
                /* TODO: Pressure system to split/merge materials */
                /* TODO: speed of material */
            }
        }

        /* apply changes */
        for(Map.Entry<Util.MyCell, Util.MyCell> curr_change : proposed_changes.entrySet()){
            parent.switch_elements(curr_change.getKey(),curr_change.getValue());
        }
    }

    @Override
    public void post_process(float[][] units, Vector2[][] velocity, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = parent.ethereal_plane.element_at(x,y);
            }
        }
    }

    /**
     * Create a simple pond with some fire on one side
     * @param floorHeight - the height of the ground floor
     */
    public void pond_with_grill(float[][] units, int floorHeight){
        for(int x = 0;x < sizeX; ++x){ /* create the ground floor */
            for(int y = 0; y < sizeY; ++y){
                if(y <= floorHeight){
                    blocks[x][y] = Materials.Names.Earth;
                    if(y <= (floorHeight/2.0f)) units[x][y] = Math.min(100.0f,rnd.nextFloat() * 500.0f);
                    else units[x][y] = Math.min(15.0f,rnd.nextFloat() * 50.0f);
                }else{
                    blocks[x][y] = Materials.Names.Air;
                    units[x][y] = Math.min(9.0f,rnd.nextFloat() * 10.0f);
                }
            }
        }

        int posX; int posY; /* Create the pond */
        for(float radius = 0.0f; radius < (floorHeight/2.0f); radius += 1.0f){
            for(float sector = (float)Math.PI * 0.95f; sector < Math.PI * 2.05f; sector += Math.PI / 180.0f){
                posX = (int)(sizeX/2.0f + (float)Math.cos(sector) * radius);
                posY = (int)(floorHeight + (float)Math.sin(sector) * radius);
                /* TODO: units *= 2.0f will put implausible values... why??? */
                if(posY <= (floorHeight - (floorHeight/4.0f)))units[posX][posY] = Math.min(100.0f,rnd.nextFloat() * 500.0f);
                blocks[posX][posY] = Materials.Names.Water;
            }
        }

        /* Create a fire */
        posX = (int)(sizeX/2.0f - floorHeight/2.0f) - 2;
        posY = floorHeight + 1;
        blocks[posX][posY] = Materials.Names.Fire;
        blocks[posX-1][posY] = Materials.Names.Fire;
        blocks[posX+1][posY] = Materials.Names.Fire;
        blocks[posX][posY+1] = Materials.Names.Fire;
    }

    public Materials.Names element_at(int x, int y){
        return blocks[x][y];
    }

}
