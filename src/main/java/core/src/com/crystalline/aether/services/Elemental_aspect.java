package com.crystalline.aether.services;

import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.models.Reality_aspect;
import com.crystalline.aether.Util;

import java.util.*;

public class Elemental_aspect implements Reality_aspect {
    protected final int sizeX;
    protected final int sizeY;
    Materials.Names[][] blocks;
    Vector2 [][] forces;

    private final Random rnd = new Random();

    public Elemental_aspect(int sizeX_, int sizeY_){
        sizeX = sizeX_;
        sizeY = sizeY_;
        blocks = new Materials.Names[sizeX][sizeY];
        forces = new Vector2[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = Materials.Names.Air;
                forces[x][y] = new Vector2();
            }
        }
    }

    public void define_by(Ethereal_aspect plane){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = plane.element_at(x,y);
                forces[x][y].set(0,0);
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

        Vector2 tmp_vec = forces[toX][toY];
        forces[toX][toY] = forces[fromX][fromY];
        forces[fromX][fromY] = tmp_vec;
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

    public Vector2 get_force(int x, int y){
        return forces[x][y];
    }

    @Override
    public void process_mechanics(float[][] units, Vector2[][] velocity, World parent) {
        for(int i = 0;i < 4;++i){
            process_mechanics_backend(units,velocity,parent);
        }
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = 1; y < sizeY-1; ++y){
                velocity[x][y].set(0,0);
            }
        }
    }

    public void process_mechanics_backend(float[][] units, Vector2[][] velocity, World parent) {
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(blocks[x][y] != Materials.Names.Air){
                    if(Materials.movable(blocks[x][y], units[x][y])){
                        forces[x][y].set(Util.gravity.x * get_weight(x,y,units),Util.gravity.y * get_weight(x,y,units));
                    }else{
                        forces[x][y].set(0,0);
                        velocity[x][y].set(0,0);
                    }
                    float unit_difference;
                    if(
                        (blocks[x][y] != Materials.Names.Air)
                        &&Materials.is_same_mat(x,y,x,y-1,blocks, units)
                        &&(Materials.movable(blocks[x][y], units[x][y]))
                    ){
                        for (int nx = (x - 1); nx < (x + 2); ++nx) {
                            for (int ny = (y - 1); ny < (y + 2); ++ny) {
                                if(
                                    Materials.movable(blocks[nx][ny], units[nx][ny])
                                &&!Materials.is_same_mat(x,y,nx,ny,blocks, units)
                                    //&&(blocks[nx][ny] != Materials.Names.Air)
                                ){//blocks[x][y] == blocks[nx][ny]){
                                    unit_difference = get_weight(x,y,units) - get_weight(nx,ny,units);
                                    forces[x][y].add(
                                    (nx-x)*unit_difference / get_weight(nx,ny,units),
                                    0.01f * (ny-y)*unit_difference / get_weight(nx,ny,units)
                                    );
                                }
                            }
                        }
                    }
                    velocity[x][y].add(
                    forces[x][y].x / get_weight(x,y,units),
                    forces[x][y].y / get_weight(x,y,units)
                    );
                } /* If the cell is not air */
            }
        }

        /* TODO: First come first served to be replaced by a better system */
        /* TODO: Multiple iterations to handle different speeds */
        /* TODO: Merge and split to be handled by "Nothing" material i.e. vacuum  */
        HashMap<Util.MyCell, Util.MyCell> proposed_changes = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
        Util.MyCell intended_change = new Util.MyCell(sizeX);
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = 1; y < sizeY-1; ++y){
//        for(int x = sizeX-2; x > 0; --x){
//            for(int y = sizeY-2; y > 0; --y){
                if(blocks[x][y] != Materials.Names.Air){
                    intended_change.set(
                        (float)(x + Math.max(-1.0,Math.min(velocity[x][y].x,1.0))),
                        (float)(y + Math.max(-1.0,Math.min(velocity[x][y].y,1.0)))
                    );

                    if(
                        (1.0f < velocity[x][y].len())
                        &&(!already_changed.contains(Util.coordinate_to_hash(x,y,sizeX)))
                    ){
                        /* TODO: handle velocity changes based on movable and elastic collision formulae */
                        if(
                                (!already_changed.contains(intended_change.hashCode()))  /* TODO: In case the target is already change don't collide */
//                        Materials.movable(blocks[intended_change.get_i_x()][intended_change.get_i_y()], units[intended_change.get_i_x()][intended_change.get_i_y()])
                            &&(get_weight(intended_change.get_i_x(),intended_change.get_i_y(),units) < get_weight(x,y,units))
//                        &&
                        ){
                            proposed_changes.put(new Util.MyCell(x,y,sizeX),new Util.MyCell(intended_change.get_i_x(),intended_change.get_i_y(),sizeX)); /* propose to switch the 2 */
                            already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                            already_changed.add(intended_change.hashCode());
                        }
                    }
                    /* TODO: Pressure system to split/merge materials */
                }
            }
        }

        /* apply changes */
        for(Map.Entry<Util.MyCell, Util.MyCell> curr_change : proposed_changes.entrySet()){
            /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
            float m1 = get_weight(curr_change.getKey().get_i_x(),curr_change.getKey().get_i_y(), units);
            Vector2 u1 = velocity[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()].cpy();

            float m2 = get_weight(curr_change.getValue().get_i_x(),curr_change.getValue().get_i_y(), units);
            Vector2 u2 = velocity[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()].cpy();

            /* TODO: Decide if collision is inelastic or not: When a collision puts two cells to the same trajectory */
            if(Materials.movable(
                blocks[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()],
                units[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()]
            )){
                velocity[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()].set(
                    ((m1 - m2)/(m1+m2)*u1.x) + 2*m2/(m1+m2)*u2.x,
                    ((m1 - m2)/(m1+m2)*u1.y) + 2*m2/(m1+m2)*u2.x
                );
            }
            if(Materials.movable(
                blocks[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()],
                units[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()]
            )){
                velocity[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()].set(
                    2*m1/(m1+m2)*u1.x + ((m2-m1)/(m1+m2)*u2.x),
                    2*m1/(m1+m2)*u1.y + ((m2-m1)/(m1+m2)*u2.y)
                );
            }
            if(
                Materials.movable(
                    blocks[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()],
                    units[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()]
                )
                &&
                Materials.movable(
                    blocks[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()],
                    units[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()]
                )
            ){
                parent.switch_elements(curr_change.getKey(),curr_change.getValue());
            }
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
                forces[x][y].set(0,0);
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
