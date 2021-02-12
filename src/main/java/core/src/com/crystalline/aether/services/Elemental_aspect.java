package com.crystalline.aether.services;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.models.Reality_aspect;
import com.crystalline.aether.Util;

import java.util.*;

public class Elemental_aspect extends Reality_aspect {
    Util myUtil;
    protected final int sizeX;
    protected final int sizeY;
    Materials.Names[][] blocks;
    Vector2 [][] forces;
    private final float[][] gravity_correction_amount;
    private final float[][] velocity_ticks;

    private final Random rnd = new Random();

    private final int velocity_max_ticks = 3;

    /* Debug variables */
    private final float[][] touched_by_mechanics;

    public Elemental_aspect(Config conf_){
        super(conf_);
        sizeX = conf.world_block_number[0];
        sizeY = conf.world_block_number[1];
        myUtil = new Util(conf_);
        blocks = new Materials.Names[sizeX][sizeY];
        forces = new Vector2[sizeX][sizeY];
        gravity_correction_amount = new float[sizeX][sizeY];
        velocity_ticks = new float[sizeX][sizeY];
        touched_by_mechanics = new float[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = Materials.Names.Air;
                forces[x][y] = new Vector2();
                touched_by_mechanics[x][y] = 0;
                gravity_correction_amount[x][y] = 0;
                velocity_ticks[x][y] = velocity_max_ticks;
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

    private float avg_of_not_block(int x, int y, float[][] table, Materials.Names type){
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
    public void process_units(float[][] units, World parent){
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
    public void process_types(float[][] units, World parent) {
        for(int x = sizeX - 1;x > 0; --x){
            for(int y = sizeY - 1 ; y > 0; --y) {
                blocks[x][y] = parent.ethereal_plane.element_at(x,y);
                /* TODO: Move averages to before the process step for consistent behavior for context dependent stuff */
                if(Materials.Names.Water == blocks[x][y]){ /* TODO: This will be ill-defined in a multi-threaded environment */
                    if(y > sizeY * 0.9){ /* TODO: Make rain based on steam */
                        units[x][y] *= 0.9;
                        forces[x][y].y = Math.min(forces[x][y].y, forces[x][y].y*-1.6f);
                    }
                    if(avg_of_block(x,y,units,Materials.Names.Water) < avg_of_block(x,y,units,Materials.Names.Fire)){
                        blocks[x][y] = Materials.Names.Air;
                    }
                }

                if(Materials.Names.Air == blocks[x][y]) {
                    if(
                        (0.2 < units[x][y])
                        &&(0 < avg_of_block(x,y,units, Materials.Names.Earth))
                        &&(0 == avg_of_block(x,y,units, Materials.Names.Water))
                        &&(avg_of_block(x,y,units, Materials.Names.Air) < avg_of_block(x,y,units, Materials.Names.Fire))
                    ){
                        blocks[x][y] = Materials.Names.Fire;
                    }
                }

                /* TODO: Store Flammability */
                /* TODO: Make fire springing out from Earth */
                if(Materials.Names.Fire == blocks[x][y]){
                    if(
                        (Materials.Mecha_properties.Plasma == Materials.get_state(blocks[x][y], units[x][y]))
                        && (units[x][y] <= avg_of_block(x,y,units, Materials.Names.Fire))
                    ){
                        units[x][y] *= 0.8f;
                    }

                    /* TODO: Make lava cool off to earth by heat */
//                    if(avg_of_block(x,y,units,Materials.Names.Water) > avg_of_block(x,y,units, Materials.Names.Fire)){
//                        blocks[x][y] = Materials.Names.Earth;
//                    }
                    if(0.2f > units[x][y]){
                        blocks[x][y] = Materials.Names.Air;
                    }
                }

                if(Materials.Names.Earth == blocks[x][y]){
                    /* TODO: Make Earth keep track of heat instead of units */
                    if((avg_of_block(x,y,units, Materials.Names.Earth) < avg_of_block(x,y,units, Materials.Names.Fire))){
                        if( /* TODO: Make sand melt "into" lava */
                            Materials.Mecha_properties.Solid.ordinal() > Materials.get_state(Materials.Names.Earth, units[x][y]).ordinal()
                            ||Materials.Mecha_properties.Plasma.ordinal() < Materials.get_state(Materials.Names.Fire, units[x][y]).ordinal()
                        ){
                            units[x][y] *= 0.8f;
                            if(0.2f < units[x][y])blocks[x][y] = Materials.Names.Fire;
                        }
                    }

                }
                if(0.2 > units[x][y])
                    units[x][y] = Math.abs(units[x][y] * 2.0f);
            }
        }

    }

    public float get_weight(int x, int y, float[][] units){
        /* TODO: Weight to include pressure somehow? or at least the same materials on top */
//        return (units[x][y] * Materials.type_specific_gravity[blocks[x][y].ordinal()][1]);
        return (
            units[x][y] * Materials.type_specific_gravity[blocks[x][y].ordinal()][Util.index_in(
                Materials.type_unit_selector[blocks[x][y].ordinal()], units[x][y]
            )]
        );
    }

    public Vector2 get_force(int x, int y){
        return forces[x][y];
    }

    @Override
    public void take_over_unit_changes(int x, int y, float[][] units) {

    }

    @Override
    public void merge_a_to_b(int ax, int ay, int bx, int by) {
        if((blocks[ax][ay] == blocks[bx][by]) || (Materials.Names.Nothing == blocks[bx][by])){
            forces[bx][by].add(forces[ax][ay]); /* TODO: Is this really how it should be? maybe use inelastic collision?? */
            blocks[bx][by] = blocks[ax][ay];
            forces[bx][by].set(0,0);
        }
    }

    @Override
    public void split_a_to_b(int ax, int ay, int bx, int by) {
        if((blocks[ax][ay] == blocks[bx][by]) || (Materials.Names.Nothing == blocks[bx][by])) {
            blocks[bx][by] = blocks[ax][ay];
            forces[ax][ay].add(forces[bx][by]); /* TODO: Inelastic collision here! */
            forces[bx][by].set(0,0);
        }
    }

    @Override
    public void process_mechanics(float[][] units, World parent) {
        HashMap<Util.MyCell, Util.MyCell> remaining_proposed_changes = new HashMap<>();

        /* Pre-process: Add gravity, and nullify forces on discardable objects; */
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(!Materials.discardable(blocks[x][y], units[x][y])){
                    gravity_correction_amount[x][y] = get_weight(x,y,units);
                    velocity_ticks[x][y] = velocity_max_ticks;
                } else{
                    forces[x][y].set(0,0); /* If the cell is not air */
                    gravity_correction_amount[x][y] = 0;
                }
                touched_by_mechanics[x][y] = 0;
            }
        }

        for(int i = 0;i <3;++i){
            process_mechanics_backend(units,parent,remaining_proposed_changes);
        }

        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(Materials.movable(blocks[x][y], units[x][y])){
                    forces[x][y].add(
                    myUtil.getGravity(x,y).x * gravity_correction_amount[x][y],
                    myUtil.getGravity(x,y).y * gravity_correction_amount[x][y]
                    );
                }
            }
        }
        /* TODO: Refine velocity to be based on "ticks" of movements, (rather than)/(in cooperation) with multiple processing loops  */
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    public void process_mechanics_backend(float[][] units, World parent, HashMap<Util.MyCell, Util.MyCell> previously_left_out_proposals){
        /* update forces based on context, calculate intended velocities based on them */
        for(int x = 1; x < sizeX-2; ++x){
            for(int y = 1; y < sizeY-2; ++y){
                if(Materials.Names.Ether == blocks[x][y]){
                    for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                        if ( /* in the bounds of the chunk */
                            (0 <= nx)&&(sizeX > nx)&&(0 <= ny)&&(sizeY > ny)
                            &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* after the not immediate neighbourhood */
                        ){ /* Calculate forces from surplus ethers */
                            float aether_diff = Math.max(-10.5f, Math.min(10.5f, (
                                parent.get_eth_plane().aether_value_at(x,y) - parent.get_eth_plane().aether_value_at(nx,ny)
                            )));
                            float nether_diff = Math.max(-10.5f, Math.min(10.5f, (
                                parent.get_eth_plane().nether_value_at(x,y) - parent.get_eth_plane().nether_value_at(nx,ny)
                            ))); /* TODO: Forces to consume surplus ether */
                            forces[x][y].add(
                            (nx - x) * nether_diff + (nx - x) * aether_diff,
                            (ny - y) * nether_diff + (ny - y) * aether_diff
                            );
                        }
                    }
                }

                if(Materials.Mecha_properties.Fluid == Materials.get_state(blocks[x][y], units[x][y])){
                    if(/* the cells next to the current one are of different material  */
                        !Materials.is_same_mat(x, y,x+1,y, blocks, units)
                        ||!Materials.is_same_mat(x, y,x-1,y, blocks, units)
                    ) { /* the cell is a liquid on top of another liquid, so it must move. */
                        for (int nx = (x - 1); nx < (x + 2); ++nx) for (int ny = (y - 1); ny < (y + 2); ++ny) {
                            if ((x != nx) && (y != ny)&&(Materials.movable(blocks[nx][ny], units[nx][ny]))){
                                float weight_difference = Math.max(-1.5f, Math.min(1.5f, (get_weight(x, y, units) - get_weight(nx, ny, units))));
                                forces[x][y].add( /* TODO: Eliminate the possibility of columns of water */
                                ((nx - x) + (ny - y) * (x % 3) * (y % 3)) * weight_difference, -forces[x][y].y
                                );
                            }
                        }
                    }
                }else if(Materials.Mecha_properties.Plasma == Materials.get_state(blocks[x][y], units[x][y])){
                    for (int nx = (x - 1); nx < (x + 2); ++nx) for (int ny = (y - 1); ny < (y + 2); ++ny) {
                        if ((x != nx) && (y != ny)&&(Materials.is_same_mat(x, y,nx,ny, blocks, units))){
                            float weight_difference = Math.max(-1.5f, Math.min(1.5f, (get_weight(x, y, units) - get_weight(nx, ny, units))));
                            forces[x][y].add(-(nx - x) * weight_difference, -(ny - y) * weight_difference);
                        }
                    }
                }
            }
        }

        /* TODO: First come first served to be replaced by a better system */
        /*!Note: Proposed changes are of the structure: key/source block array --> value/target block array
         * */
        HashMap<Util.MyCell, Util.MyCell> proposed_changes = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
        HashMap<Util.MyCell, Util.MyCell> remaining = new HashMap<>();

        /* Take over proposals left out from the previous process loop */
        for(Map.Entry<Util.MyCell, Util.MyCell> curr_change : previously_left_out_proposals.entrySet()){
            if(!evaluate_for_mechanics(units, curr_change.getKey(), curr_change.getValue(),proposed_changes,already_changed)){
                remaining.put(curr_change.getKey(),curr_change.getValue());
            }else{ /* Able to evaluate the cells in this loop, gravity shall be applied directly */
                gravity_correction_amount[curr_change.getKey().get_i_x()][curr_change.getKey().get_i_y()] = 0;
                gravity_correction_amount[curr_change.getValue().get_i_x()][curr_change.getValue().get_i_y()] = 0;
            }
        }
        previously_left_out_proposals.clear();
        previously_left_out_proposals.putAll(remaining);

        /* process proposals for the current loop */
        if(rnd.nextInt(2) == 0){
            if(rnd.nextInt(2) == 0){
                for(int x = 1; x < sizeX-1; ++x) for(int y = 1; y < sizeY-1; ++y)
                    create_proposal_for_cell(x, y, units, previously_left_out_proposals, proposed_changes, already_changed);
            }else{
                for(int x = 1; x < sizeX-1; ++x) for(int y = sizeY-2; y > 0; --y)
                    create_proposal_for_cell(x, y, units, previously_left_out_proposals, proposed_changes, already_changed);
            }
        }else{
            if(rnd.nextInt(2) == 0){
                for(int x = sizeX-2; x > 0; --x) for(int y = 1; y < sizeY-1; ++y)
                    create_proposal_for_cell(x, y, units, previously_left_out_proposals, proposed_changes, already_changed);
            }else{
                for(int x = sizeX-2; x > 0; --x) for(int y = sizeY-2; y > 0; --y)
                    create_proposal_for_cell(x, y, units, previously_left_out_proposals, proposed_changes, already_changed);
            }
        }

        /* apply changes */
        for(Map.Entry<Util.MyCell, Util.MyCell> curr_change : proposed_changes.entrySet()){
            int source_x = curr_change.getKey().get_i_x();
            int source_y = curr_change.getKey().get_i_y();
            int target_x = curr_change.getValue().get_i_x();
            int target_y = curr_change.getValue().get_i_y();
            if(
                Materials.Names.Ether == blocks[source_x][source_y]
                &&Materials.Names.Ether != blocks[target_x][target_y]
                &&(500.0 < forces[source_x][source_y].len())
            ){ /* In case the material to move is Ether, and it has a relaitvely big force */ /* TODO: Make force transfer depending on the state of the matter, to make crystals stable */
                forces[target_x][target_y].add(forces[source_x][source_y]);
                blocks[source_x][source_y] = Materials.Names.Air;
                forces[target_x][target_y].scl(units[source_x][source_y]);
//                units[target_x][target_y] += units[source_x][source_y];
                units[source_x][source_y] = 0.01f;
            }else
            if(
                Materials.discardable(blocks[target_x][target_y],units[target_x][target_y])
                ||(
                    (get_weight(source_x,source_y,units) > get_weight(target_x,target_y, units))
                    &&Materials.movable(blocks[target_x][target_y],units[target_x][target_y])
                )
            ){
                /* swap the 2 cells, decreasing the forces on both */
                forces[source_x][source_y].add(
                -forces[source_x][source_y].x * (Math.abs(get_weight(source_x,source_y,units)) / Math.max(0.00001f, Math.max(Math.abs(get_weight(source_x,source_y,units)), forces[source_x][source_y].x))),
                -forces[source_x][source_y].y * (Math.abs(get_weight(source_x,source_y,units)) / Math.max(0.00001f, Math.max(Math.abs(get_weight(source_x,source_y,units)), forces[source_x][source_y].y)))
                );
                forces[source_x][source_y].add(
                myUtil.getGravity(source_x,source_y).x * get_weight(source_x,source_y,units),
                myUtil.getGravity(source_x,source_y).y * get_weight(source_x,source_y,units)
                );

                parent.switch_elements(curr_change.getKey(),curr_change.getValue());
            }else{ /* The cells collide, updating forces, but no swapping */
                float m1 = get_weight(source_x, source_y, units);
                Vector2 u1 = forces[source_x][source_y].cpy().nor();
                float m2 = get_weight(target_x, target_y, units);
                Vector2 u2 = forces[target_x][target_y].cpy().nor();
                Vector2 result_speed = new Vector2();
                result_speed.set( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                    ((m1 - m2)/(m1+m2)*u1.x) + (2.0f*m2/(m1+m2))*u2.x,
                    ((m1 - m2)/(m1+m2)*u1.y) + (2.0f*m2/(m1+m2))*u2.y
                );
                forces[source_x][source_y].set( /* F = m*a --> `a` is the delta v, which is the change in the velocity */
                    m1 * (result_speed.x - u1.x),
                    m1 * (result_speed.y - u1.y)
                );
                forces[source_x][source_y].add(
                myUtil.getGravity(source_x,source_y).x * get_weight(source_x,source_y,units),
                myUtil.getGravity(source_x,source_y).y * get_weight(source_x,source_y,units)
                );
                gravity_correction_amount[source_x][source_y] = 0;
                if(Materials.movable(blocks[target_x][target_y],units[target_x][target_y])){
                    result_speed.set( /*!Note: it is supposed, that non-movable cells do not initiate movement */
                        (2.0f*m1/(m1+m2))*u1.x + ((m2-m1)/(m1+m2)*u2.x),
                        (2.0f*m1/(m1+m2))*u1.y + ((m2-m1)/(m1+m2)*u2.y)
                    );
                    forces[target_x][target_y].set( m2 * (result_speed.x - u2.x), m2 * (result_speed.y - u2.y) );
                    forces[target_x][target_y].add( /* Since forces are changed, gravity correction shall be done in-place */
                    myUtil.getGravity(target_x,target_y).x * get_weight(target_x,target_y,units),
                    myUtil.getGravity(target_x,target_y).y * get_weight(target_x,target_y,units)
                    );
                    gravity_correction_amount[target_x][target_y] = 0;
                } /* do not update the force for unmovable objects */
            }
            touched_by_mechanics[source_x][source_y] = 1;
            touched_by_mechanics[target_x][target_y] = 1;
        }
    }

    private void create_proposal_for_cell(
        int x, int y, float[][] units,
        HashMap<Util.MyCell, Util.MyCell> previously_left_out_proposals,
        HashMap<Util.MyCell, Util.MyCell> proposed_changes, HashSet<Integer> already_changed
    ){
        Util.MyCell intended_source_cell = new Util.MyCell(sizeX);
        Util.MyCell intended_target_cell = new Util.MyCell(sizeX);
        if( !Materials.discardable(blocks[x][y], units[x][y]) && (1.0f <= forces[x][y].len()) ){
            intended_source_cell.set(x,y);
            intended_target_cell.set(x,y);
            if(1.0 <= Math.abs(forces[x][y].x))intended_target_cell.set(
                    (x + Math.max(-1.0f, Math.min(forces[x][y].x,1.0f))), intended_target_cell.y
            );
            if(1.0 <= Math.abs(forces[x][y].y))intended_target_cell.set(
                    intended_target_cell.x, (y + Math.max(-1.0f,Math.min(forces[x][y].y,1.0f)))
            );
            if(!evaluate_for_mechanics(units, intended_source_cell,intended_target_cell,proposed_changes,already_changed)){
                previously_left_out_proposals.put(new Util.MyCell(intended_source_cell),new Util.MyCell(intended_target_cell));
                /* Since these cells are left out, add no gravity to them! */
                gravity_correction_amount[intended_source_cell.get_i_x()][intended_source_cell.get_i_y()] = 0;
                gravity_correction_amount[intended_target_cell.get_i_x()][intended_target_cell.get_i_y()] = 0;
            }
        }
    }

    /**
     *  A Function to try and propose cell pairs to change in this mechanics iteration
     * @param units
     * @param source_cell
     * @param target_cell
     * @param already_proposed_changes
     * @param already_changed
     * @return whether or not the cells could be placed into the already proposed changes
     */
    private boolean evaluate_for_mechanics(
        float[][] units, Util.MyCell source_cell, Util.MyCell target_cell,
        HashMap<Util.MyCell, Util.MyCell> already_proposed_changes, HashSet<Integer> already_changed
    ){
        int x = source_cell.get_i_x();
        int y = source_cell.get_i_y();
        int intended_x = target_cell.get_i_x();
        int intended_y = target_cell.get_i_y();
        if(
            !((x == intended_x) && (y == intended_y))
            &&( /* In case both is discardable, then no operations shall commence */
                !Materials.discardable(blocks[x][y],units[x][y])
                ||!Materials.discardable(blocks[intended_x][intended_y],units[intended_x][intended_y])
            )
            &&(!already_changed.contains(Util.coordinate_to_hash(x,y,sizeX)))
            &&(!already_changed.contains(Util.coordinate_to_hash(intended_x,intended_y,sizeX)))
        ){
            if(velocity_max_ticks == velocity_ticks[x][y]){
                /* see if the two cells still intersect with forces included */
                if( /* TODO: more accurate intention, taking both forces into consideration */
                    2.0f > source_cell.cpy().sub(target_cell.cpy().add(forces[intended_x][intended_y].cpy().nor())).len()
                ){
                    already_proposed_changes.put(
                            new Util.MyCell(x,y,sizeX),new Util.MyCell(intended_x,intended_y,sizeX)
                    ); /* propose to switch the 2 */
                    already_changed.add(Util.coordinate_to_hash(x,y,sizeX));
                    already_changed.add(Util.coordinate_to_hash(intended_x,intended_y,sizeX));
                    velocity_ticks[x][y] = 0;
                    return true;
                }
            }else ++velocity_ticks[x][y];
        } /* Able to process mechanics on the 2 blocks */
        return false;
    }

    @Override
    public void post_process(float[][] units, World parent) {
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
                    units[x][y] = Math.min(1.0f,rnd.nextFloat() * 10.0f);
                }
            }
        }

        int posX; int posY; /* Create the pond */
        for(float radius = 0.0f; radius < (floorHeight/2.0f); radius += 0.5f){
            for(float sector = (float)Math.PI * 0.99f; sector < Math.PI * 2.01f; sector += Math.PI / 180.0f){
                posX = (int)(sizeX/2.0f + (float)Math.cos(sector) * radius);
                posX = Math.max(0, Math.min(sizeX, posX));
                posY = (int)(floorHeight + (float)Math.sin(sector) * radius);
                posY = Math.max(0, Math.min(sizeY, posY));
                /* TODO: units *= 2.0f will put implausible values... why??? */
                if(posY <= (floorHeight - (floorHeight/4.0f)))
//                && (0 == (posX%2))){
//                    if(units[posX][posY] > 500) System.out.println("Unplausible values at: " + posX + "," + posY + "!");
//                    units[posX][posY] *= 2.0f;
//                }
                units[posX][posY] = Math.min(100.0f,rnd.nextFloat() * 500.0f);
                else units[posX][posY] = Math.min(1.0f,rnd.nextFloat() * 15.0f);
                blocks[posX][posY] = Materials.Names.Water;
            }
        }

        /* Create a fire */
        posX = (int)(sizeX/4.0f);
        posY = floorHeight + 1;
        blocks[posX][posY] = Materials.Names.Fire;
        blocks[posX-1][posY] = Materials.Names.Fire;
        blocks[posX+1][posY] = Materials.Names.Fire;
        blocks[posX][posY+1] = Materials.Names.Fire;
    }

    public Materials.Names element_at(int x, int y){
        return blocks[x][y];
    }

    public Color getColor(int x, int y, float[][] units){
        return Materials.get_color(blocks[x][y], units[x][y]).cpy();
    }

    public Color getDebugColor(int x, int y, float[][] units){
        /*TODO: Debug the diagonal red line */
        Color defColor = getColor(x,y, units).cpy();
        if(0 < touched_by_mechanics[x][y]){ /* it was modified.. */
            defColor.lerp(Color.GREEN, 0.5f);
        }
        return defColor;
    }

}
