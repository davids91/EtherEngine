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
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(blocks[nx][ny] == type){
                    average_val += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private float avg_of_block_within_distance(int x, int y, float[][] table, Materials.Names[][] types, float[][] units){
        float average_val = 0.0f;
        float division = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(Materials.is_same_mat(x,y,nx,ny,types,units)){
                    average_val += table[nx][ny];
                    division += 1.0f;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
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
                if(Materials.movable(blocks[x][y],units[x][y])){
                    avgs[x][y] = avg_of_block_within_distance(x,y,units,blocks,units);
                }
            }
        }

        for(int x = 0;x < sizeX; ++x) { /* Calculate dilution */
            for (int y = 0; y < sizeY; ++y) {
                if(Materials.movable(blocks[x][y],units[x][y])) {
                    units[x][y] = avgs[x][y];
                }
            }
        }
    }

    @Override
    public void process_types(float[][] units, Vector2[][] velocity, World parent) {
        for(int x = sizeX - 1;x > 0; --x){
            for(int y = sizeY - 1 ; y > 0; --y) {
                blocks[x][y] = parent.ethereal_plane.element_at(x,y);
                /* TODO: Move averages to before the process step for consistent behavior for context dependent stuff */
                if(Materials.Names.Water == blocks[x][y]){ /* TODO: This will be ill-defined in a multi-threaded environment */
                    if(avg_of_block(x,y,units,Materials.Names.Water) < avg_of_block(x,y,units,Materials.Names.Fire)){ /* TODO: Make Air catch Flame */
                        blocks[x][y] = Materials.Names.Air;
                    }
                }

                if(Materials.Names.Fire == blocks[x][y]){
                    if(avg_of_block(x,y,units,Materials.Names.Water) >= avg_of_block(x,y,units, Materials.Names.Fire)){
                        blocks[x][y] = Materials.Names.Earth;
                    }
//                    if(2.0f > units[x][y]){
//                        blocks[x][y] = Materials.Names.Air;
//                    }
                }

                if(Materials.Names.Earth == blocks[x][y]){
                    if(00.f < avg_of_block(x,y,units, Materials.Names.Fire)){
                        if(avg_of_block(x,y,units, Materials.Names.Earth) <= avg_of_block(x,y,units, Materials.Names.Fire)){
                            units[x][y] *= (
                                avg_of_block(x,y,units, Materials.Names.Earth) / avg_of_block(x,y,units, Materials.Names.Fire)
                            ); /* TODO: Make Earth keep track of heat instead of units */
                        }
                        if(units[x][y] <= Materials.type_color_scale[Materials.Names.Earth.ordinal()][2]){
                            blocks[x][y] = Materials.Names.Fire;
                        }
                    }
                }
            }
        }

    }

    private float get_weight(int x, int y, float[][] units){
        /* TODO: Weight to include pressure somehow? or at least the same materials on top */
        return (units[x][y] * Materials.type_specific_gravity[blocks[x][y].ordinal()][1]);
    }

    public Vector2 get_force(int x, int y){
        return forces[x][y];
    }

    @Override
    public void merge_a_to_b(int ax, int ay, int bx, int by) {
        if(blocks[ax][ay] == blocks[bx][by]){
            blocks[bx][by] = Materials.Names.Nothing;
            forces[ax][ax].add(forces[bx][by]); /* TODO: Is this really how it should be? maybe use inelastic collision?? */
        }
    }

    @Override
    public void process_mechanics(float[][] units, Vector2[][] velocity, World parent) {
        for(int i = 0;i < 3;++i){
            process_mechanics_backend(units,velocity,parent);
        }
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = 1; y < sizeY-1; ++y){
                velocity[x][y].set(0,0);
            }
        }
    }

    /* TODO: Make movable objects, depending of the solidess "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    public void process_mechanics_backend(float[][] units, Vector2[][] velocity, World parent) {

        /* update forces based on context and calculate velocities based on forces */
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(!Materials.discardable(blocks[x][y], units[x][y])){
                    if(Materials.movable(blocks[x][y], units[x][y])){
                        forces[x][y].set(Util.gravity.x * get_weight(x,y,units),Util.gravity.y * get_weight(x,y,units));
                    }else{
                        forces[x][y].set(0,0);
                        velocity[x][y].set(0,0);
                    }
                    float unit_difference;
                    if(
                        Materials.is_same_mat(x,y,x,y-1,blocks, units)
                        &&(Materials.movable(blocks[x][y], units[x][y]))
                    ){
                        for (int nx = (x - 1); nx < (x + 2); ++nx) {
                            for (int ny = (y - 1); ny < (y + 2); ++ny) {
                                if(
                                    Materials.movable(blocks[nx][ny], units[nx][ny])
                                &&!Materials.is_same_mat(x,y,nx,ny,blocks, units)
                                    //&&!Materials.discardable(blocks[nx][ny], units[nx][ny])
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
        /*!Note: Proposed changes are of the structure:
         * - key --> value: source block array --> target block array
         * - the last element of the source block array is called the source cell
         * - the first element of the target array is called the target cell
         * - the goal is to free up the target cell by moving its contents, or if not possible, merge from the source cell
         * - the source cell is moved/merged into the target cell, and the remaining void is filled in by the source block array
         * - in case there is only one element in either array: the value element and the last element of the key array are switched
         * - if there are multiple elements in the key array, then no swaps will occur,
         *   instead the last cell in the key array will be "pushed" to the first cell of the value array
         *   - the elements in value are pushed up in a FIFO way leaving the first element as a vacuum
         *     to be overwritten by the last element of the first array
         *   - the last element in the key array is moved to the first element of the value array
         *   - the other elements in the key array are being divided evenly in the cells to "fill up the place" of the last element
         *  */
        HashMap<Util.MyCell[], Util.MyCell[]> proposed_changes = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
        Util.MyCell intended_change = new Util.MyCell(sizeX);
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = 1; y < sizeY-1; ++y){
//        for(int x = sizeX-2; x > 0; --x){
//            for(int y = sizeY-2; y > 0; --y){
                if(!Materials.discardable(blocks[x][y], units[x][y])){
                    intended_change.set(
                    (float)(x + Math.max(-1.0,Math.min(velocity[x][y].x,1.0))),
                    (float)(y + Math.max(-1.0,Math.min(velocity[x][y].y,1.0)))
                    );
                    if(
                        (1.0f <= velocity[x][y].len())
                        &&(!already_changed.contains(intended_change.hashCode())) /* TODO: In case the target is already changed don't collide */
                        &&(!already_changed.contains(Util.coordinate_to_hash(x,y,sizeX)))
                    ){
                        already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                        boolean merged = false;
                        if( /* try to merge into */
                            Materials.movable(blocks[x][y], units[x][y])
                            &&!Materials.is_hard(blocks[x][y], units[x][y])
                            &&Materials.is_same_mat(x,y,intended_change.get_i_x(),intended_change.get_i_y(), blocks, units)
                        ){
                            /* look for a neighbour to help with the merge */
                            int nx = (int)(x - Math.max(-1.0,Math.min(velocity[x][y].x,1.0)));
                            int ny = (int)(y - Math.max(-1.0,Math.min(velocity[x][y].y,1.0)));
                            if(
                                !already_changed.contains(Util.coordinate_to_hash(nx, ny, sizeX))
                                &&!Materials.is_same_mat(x, y, nx, ny, blocks, units)
                            ){
//                                put it here!
                                proposed_changes.put(
                                    new Util.MyCell[]{new Util.MyCell(nx, ny, sizeX), new Util.MyCell(x, y, sizeX)},
                                    new Util.MyCell[]{new Util.MyCell(intended_change.get_i_x(),intended_change.get_i_y(),sizeX)}
                                ); /* propose to merge the cell into it's target */
                                already_changed.add(Util.coordinate_to_hash(nx, ny, sizeX));
                                already_changed.add(intended_change.hashCode());
                                merged = true;
                            }else{
                                out_of_the_double_cycle:
                                for (nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
                                    for (ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                                        if(
                                            !((x == nx) && (y == ny))
                                            &&!already_changed.contains(Util.coordinate_to_hash(nx, ny, sizeX))
                                            &&!Materials.is_same_mat(x,y,x,ny, blocks, units)
                                        ){
                                            proposed_changes.put(
                                                new Util.MyCell[]{new Util.MyCell(nx,ny,sizeX), new Util.MyCell(x,y,sizeX)},
                                                new Util.MyCell[]{new Util.MyCell(intended_change.get_i_x(),intended_change.get_i_y(),sizeX)}
                                            ); /* propose to merge the cell into it's target */
                                            already_changed.add(Util.coordinate_to_hash(nx, ny, sizeX));
                                            already_changed.add(intended_change.hashCode());
                                            merged = true;
                                            break out_of_the_double_cycle; /* this will step after the loop */
                                        }
                                    }
                                }
                            }
                        }

                        if(
                        (!merged)
                          &&(get_weight(intended_change.get_i_x(),intended_change.get_i_y(),units) < get_weight(x,y,units))
//                          &&Materials.movable(blocks[intended_change.get_i_x()][intended_change.get_i_y()], units[intended_change.get_i_x()][intended_change.get_i_y()])
                        ){
                            proposed_changes.put(
                                new Util.MyCell[]{new Util.MyCell(x,y,sizeX)},
                                new Util.MyCell[]{new Util.MyCell(intended_change.get_i_x(),intended_change.get_i_y(),sizeX)}
                            ); /* propose to switch the 2 */
                            already_changed.add(intended_change.hashCode());
                        }
                    }
                    /* TODO: Pressure system to split/merge materials */
                }
            }
        }

        /* apply changes */
        for(Map.Entry<Util.MyCell[], Util.MyCell[]> curr_change : proposed_changes.entrySet()){
            if(
                ((curr_change.getKey().length > 0)&&(curr_change.getValue().length > 0))
                &&(curr_change.getKey().length == 1)||(curr_change.getValue().length == 1)
            ){ /* if swapping happens */ /* TODO: equalize materials in case the cells in the middle are of the same type */
                /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                float m1 = get_weight(curr_change.getKey()[0].get_i_x(),curr_change.getKey()[0].get_i_y(), units);
                Vector2 u1 = velocity[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()].cpy();

                float m2 = get_weight(curr_change.getValue()[0].get_i_x(),curr_change.getValue()[0].get_i_y(), units);
                Vector2 u2 = velocity[curr_change.getValue()[0].get_i_x()][curr_change.getValue()[0].get_i_y()].cpy();

                /* TODO: Decide if collision is inelastic or not: When a collision puts two cells to the same trajectory */
                if(Materials.movable(
                        blocks[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()],
                        units[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()]
                )){
                    velocity[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()].set(
                        ((m1 - m2)/(m1+m2)*u1.x) + 2*m2/(m1+m2)*u2.x,
                        ((m1 - m2)/(m1+m2)*u1.y) + 2*m2/(m1+m2)*u2.x
                    );
                }
                if(Materials.movable(
                        blocks[curr_change.getValue()[0].get_i_x()][curr_change.getValue()[0].get_i_y()],
                        units[curr_change.getValue()[0].get_i_x()][curr_change.getValue()[0].get_i_y()]
                )){
                    velocity[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()].set(
                            2*m1/(m1+m2)*u1.x + ((m2-m1)/(m1+m2)*u2.x),
                            2*m1/(m1+m2)*u1.y + ((m2-m1)/(m1+m2)*u2.y)
                    );
                }
                if(
                    Materials.movable(
                        blocks[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()],
                        units[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()]
                    )
                    &&
                    Materials.movable(
                        blocks[curr_change.getValue()[0].get_i_x()][curr_change.getValue()[0].get_i_y()],
                        units[curr_change.getValue()[0].get_i_x()][curr_change.getValue()[0].get_i_y()]
                    )
                )parent.switch_elements(curr_change.getKey()[0],curr_change.getValue()[0]);
            }else if(
                ((curr_change.getKey().length > 0)&&(curr_change.getValue().length > 0))
                /*!Note: some of the array lengths should be greater, than 1 */
            ){ /* not swapping, trying to move cells */
                /* try to make space in the target block array */
                int spaces_freed_up = 0;
                for(int i = curr_change.getValue().length-1; i > 0; --i){
                    /* merge the cell[i-1] into cell[i] if possible */
                    if(
                        Materials.a_can_be_merged_into_b(
                            curr_change.getValue()[i-1].get_i_x(), curr_change.getValue()[i-1].get_i_y(),
                            curr_change.getValue()[i].get_i_x(), curr_change.getValue()[i].get_i_y(), blocks,units
                        )
                    ){
                        parent.merge_a_into_b(curr_change.getValue()[i-1], curr_change.getValue()[i]);
                        if(i == 1) ++spaces_freed_up; /* If the first element is successfully freed up */
                    }
                }

                /* move source block arrays last element into the first element of the target array */
                if(
                    (0 < spaces_freed_up) /* there is space available to move the last element of the source to the first element of the target */
                    ||(
                        Materials.a_can_be_merged_into_b(
                            curr_change.getKey()[curr_change.getKey().length-1].get_i_x(), curr_change.getKey()[curr_change.getKey().length-1].get_i_y(),
                            curr_change.getValue()[0].get_i_x(), curr_change.getValue()[0].get_i_y(), blocks,units
                        )
                    )
                ){
                    /* if(0 == spaces_freed_up){ // the source cell is merged into the target cell */
                    /* - it doesn't matter in this case, as the source cell will be left for nothing, and the target cell will
                    *    have its units updated by it; While thy type is overwritten by the source cell ( the same type or type "Nothing" is overwritten ) */
                    parent.merge_a_into_b(curr_change.getKey()[curr_change.getKey().length-1], curr_change.getValue()[0]);

                    /* trying to equalize the pressure in the source block array */ /* TODO: use more, than just the first element */
                    units[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()] /= 2.0f;
                    units[curr_change.getKey()[curr_change.getKey().length-1].get_i_x()][curr_change.getKey()[curr_change.getKey().length-1].get_i_y()] +=
                        units[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()];
                    blocks[curr_change.getKey()[curr_change.getKey().length-1].get_i_x()][curr_change.getKey()[curr_change.getKey().length-1].get_i_y()] =
                        blocks[curr_change.getKey()[0].get_i_x()][curr_change.getKey()[0].get_i_y()];
                } /* else: no movement shall take place */
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
