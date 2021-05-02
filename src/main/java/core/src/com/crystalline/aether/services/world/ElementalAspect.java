package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.services.utils.MathUtils;
import com.crystalline.aether.services.utils.MiscUtil;
import com.crystalline.aether.services.utils.Vector2i;

import java.util.*;

public class ElementalAspect extends RealityAspect {
    MiscUtil myMiscUtil;
    protected final int sizeX;
    protected final int sizeY;
    private final int velocityMaxTicks = 3;
    private final Random rnd = new Random();

    private Material.Elements[][] blocks;
    private Vector2i[][] forces;
    private int[][] velocityTicks;
    private int[][] gravityCorrectionAmount;

    private int[][] touchedByMechanics; /* Debug purposes */

    public ElementalAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        myMiscUtil = new MiscUtil();
        blocks = new Material.Elements[sizeX][sizeY];
        forces = new Vector2i[sizeX][sizeY];
        gravityCorrectionAmount = new int[sizeX][sizeY];
        velocityTicks = new int[sizeX][sizeY];
        touchedByMechanics = new int[sizeX][sizeY];
        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
            Arrays.copyOf(blocks, blocks.length),
            Arrays.copyOf(forces, forces.length),
            Arrays.copyOf(gravityCorrectionAmount, gravityCorrectionAmount.length),
            Arrays.copyOf(velocityTicks, velocityTicks.length),
            Arrays.copyOf(touchedByMechanics, touchedByMechanics.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        blocks = (Material.Elements[][]) state[0];
        forces = (Vector2i[][]) state[1];
        gravityCorrectionAmount = (int[][]) state[2];
        velocityTicks = (int[][]) state[3];
        touchedByMechanics = (int[][]) state[4];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = Material.Elements.Air;
                forces[x][y] = new Vector2i();
                touchedByMechanics[x][y] = 0;
                gravityCorrectionAmount[x][y] = 0;
                velocityTicks[x][y] = velocityMaxTicks;
            }
        }
    }

    public void defineBy(EtherealAspect plane){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = plane.elementAt(x,y);
            }
        }
    }

    private float avgOfBlock(int x, int y, int[][] table, Material.Elements type){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(blocks[nx][ny] == type){
                    average_val += table[nx][ny];
                    division += 1;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private int avgOfBlockWithinDistance(int x, int y, int[][] table, Material.Elements[][] types, int[][] units){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(Material.isSameMat(x,y,nx,ny,types,units)){
                    average_val += table[nx][ny];
                    division += 1;
                }
            }
        }
        if(0 < division)average_val /= division;
        return (int)average_val;
    }

    @Override
    public void determineUnits(int[][] units, World parent) {

    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        Material.Elements tmp_bloc = blocks[toX][toY];
        blocks[toX][toY] = blocks[fromX][fromY];
        blocks[fromX][fromY] = tmp_bloc;

        Vector2i tmp_vec = forces[toX][toY];
        forces[toX][toY] = forces[fromX][fromY];
        forces[fromX][fromY] = tmp_vec;
    }

    @Override
    public void processUnits(int[][] units, World parent){
        int[][] avgs = new int[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(Material.movable(blocks[x][y],units[x][y])){
                    avgs[x][y] = avgOfBlockWithinDistance(x,y,units,blocks,units);
                }
            }
        }

        for(int x = 0;x < sizeX; ++x) { /* Calculate dilution */
            for (int y = 0; y < sizeY; ++y) {
                if(Material.movable(blocks[x][y],units[x][y])) {
                    units[x][y] = avgs[x][y];
                }
            }
        }
    }

    @Override
    public void processTypes(int[][] units, World parent) {
        for(int x = sizeX - 1;x > 0; --x){
            for(int y = sizeY - 1 ; y > 0; --y) {
                blocks[x][y] = parent.etherealPlane.elementAt(x,y);
                /* TODO: Move averages to before the process step for consistent behavior for context dependent stuff */
                if(Material.Elements.Water == blocks[x][y]){ /* TODO: This will be ill-defined in a multi-threaded environment */
                    if(y > sizeY * 0.9){ /* TODO: Make rain based on steam */
                        units[x][y] *= 0.9;
                        forces[x][y].y = (int)Math.min(forces[x][y].y, forces[x][y].y*-1.6f);
                    }
                    if(avgOfBlock(x,y,units, Material.Elements.Water) < avgOfBlock(x,y,units, Material.Elements.Fire)){
                        blocks[x][y] = Material.Elements.Air;
                    }
                }

                if(Material.Elements.Air == blocks[x][y]) {
                    if(
                        (0.2 < units[x][y])
                        &&(0 < avgOfBlock(x,y,units, Material.Elements.Earth))
                        &&(0 == avgOfBlock(x,y,units, Material.Elements.Water))
                        &&(avgOfBlock(x,y,units, Material.Elements.Air) < avgOfBlock(x,y,units, Material.Elements.Fire))
                    ){
                        blocks[x][y] = Material.Elements.Fire;
                    }
                }

                /* TODO: Store Flammability */
                /* TODO: Make fire springing out from Earth */
                if(Material.Elements.Fire == blocks[x][y]){
                    if(
                        (Material.MechaProperties.Plasma == Material.getState(blocks[x][y], units[x][y]))
                        && (units[x][y] <= avgOfBlock(x,y,units, Material.Elements.Fire))
                    ){
                        units[x][y] *= 0.8f;
                    }

                    /* TODO: Make lava cool off to earth by heat */
//                    if(avg_of_block(x,y,units,Materials.Names.Water) > avg_of_block(x,y,units, Materials.Names.Fire)){
//                        blocks[x][y] = Materials.Names.Earth;
//                    }
                    if(0.2f > units[x][y]){
                        blocks[x][y] = Material.Elements.Air;
                    }
                }

                if(Material.Elements.Earth == blocks[x][y]){
                    /* TODO: Make Earth keep track of heat instead of units */
                    if((avgOfBlock(x,y,units, Material.Elements.Earth) < avgOfBlock(x,y,units, Material.Elements.Fire))){
                        if( /* TODO: Make sand melt "into" glass */
                            Material.MechaProperties.Solid.ordinal() > Material.getState(Material.Elements.Earth, units[x][y]).ordinal()
                            || Material.MechaProperties.Plasma.ordinal() < Material.getState(Material.Elements.Fire, units[x][y]).ordinal()
                        ){
                            units[x][y] *= 0.8f;
                            if(0.2f < units[x][y])blocks[x][y] = Material.Elements.Fire;
                        }
                    }

                }
                if(0.2 > units[x][y]) units[x][y] = Math.abs(units[x][y] * 2);
            }
        }

    }

    public float getWeight(int x, int y, int[][] units){
        /* TODO: Weight to include pressure somehow? or at least the same materials on top */
        return (
            units[x][y] * Material.TYPE_SPECIFIC_GRAVITY[blocks[x][y].ordinal()][MiscUtil.indexIn(
                Material.TYPE_UNIT_SELECTOR[blocks[x][y].ordinal()], units[x][y]
            )]
        );
    }

    public Vector2i getForce(int x, int y){
        return forces[x][y];
    }

    @Override
    public void takeOverUnitChanges(int x, int y, int[][] units) {

    }

    @Override
    public void processMechanics(int[][] units, World parent) {
        HashMap<MiscUtil.MyCell, MiscUtil.MyCell> remaining_proposed_changes = new HashMap<>();
        for(int x = 1; x < sizeX-1; ++x){ /* Pre-process: Add gravity, and nullify forces on discardable objects; */
            for(int y = sizeY-2; y > 0; --y){
                touchedByMechanics[x][y] = 0;
            }
        }

        for(int i = 0; i < velocityMaxTicks; ++i){
            processMechanicsBackend(units,parent,remaining_proposed_changes);
        }

        for(Map.Entry<MiscUtil.MyCell, MiscUtil.MyCell> missedCells : remaining_proposed_changes.entrySet()){
            gravityCorrectionAmount[missedCells.getKey().getIX()][missedCells.getKey().getIY()] =
                    (int)(getWeight(missedCells.getKey().getIX(),missedCells.getKey().getIY(),units));
            gravityCorrectionAmount[missedCells.getValue().getIX()][missedCells.getValue().getIY()] =
                    (int)(getWeight(missedCells.getValue().getIX(),missedCells.getValue().getIY(),units));
        }

        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(Material.movable(blocks[x][y], units[x][y])){
                    forces[x][y].add(
                        myMiscUtil.getGravity(x,y).x * gravityCorrectionAmount[x][y],
                        myMiscUtil.getGravity(x,y).y * gravityCorrectionAmount[x][y]
                    );
                }
            }
        }
        /* TODO: Refine velocity to be based on "ticks" of movements, (rather than)/(in cooperation) with multiple processing loops  */
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    public void processMechanicsBackend(int[][] units, World parent, HashMap<MiscUtil.MyCell, MiscUtil.MyCell> previouslyLeftOutProposals){
        /* update forces based on context, calculate intended velocities based on them */
        for(int x = 1; x < sizeX-2; ++x){
            for(int y = 1; y < sizeY-2; ++y){
                if(!Material.discardable(blocks[x][y], units[x][y])){
                    gravityCorrectionAmount[x][y] = (int)(getWeight(x,y,units));
                    velocityTicks[x][y] = velocityMaxTicks;
                } else{
                    forces[x][y].set(0,0); /* If the cell is not air */
                    gravityCorrectionAmount[x][y] = 0;
                }
                if(Material.Elements.Ether == blocks[x][y]){
                    for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                        if ( /* in the bounds of the chunk */
                            (0 <= nx)&&(sizeX > nx)&&(0 <= ny)&&(sizeY > ny)
                            &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* after the not immediate neighbourhood */
                            &&(units[x][y] <= units[nx][ny])
                            &&(Material.Elements.Ether == blocks[nx][ny])
                        ){ /* Calculate forces from surplus ethers */
                            float aether_diff = Math.max(-10.5f, Math.min(10.5f, (
                                parent.getEtherealPlane().aetherValueAt(x,y) - parent.getEtherealPlane().aetherValueAt(nx,ny)
                            )));
                            forces[x][y].add(((nx - x) * aether_diff), ((ny - y) * aether_diff));
                        }
                    }
                }

                if(Material.MechaProperties.Fluid == Material.getState(blocks[x][y], units[x][y])){
                    if(Material.isSameMat(x, y,x,y-1, blocks, units)) /* the cell is a liquid on top of another liquid, so it must move. */
                        forces[x][y].set((rnd.nextInt(6)-3),1);
                }else if(Material.MechaProperties.Plasma == Material.getState(blocks[x][y], units[x][y])){
                    forces[x][y].add((rnd.nextInt(4)-2),0);
                }
            }
        }

        /* TODO: First come first served to be replaced by a better system */
        /*!Note: Proposed changes are of the structure: key/source block array --> value/target block array
         * */
        HashMap<MiscUtil.MyCell, MiscUtil.MyCell> proposedChanges = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
        HashMap<MiscUtil.MyCell, MiscUtil.MyCell> remaining = new HashMap<>();

        /* Take over proposals left out from the previous process loop */
        for(Map.Entry<MiscUtil.MyCell, MiscUtil.MyCell> currChange : previouslyLeftOutProposals.entrySet()){
            if(!evaluateForMechanics(units, currChange.getKey(), currChange.getValue(),proposedChanges,already_changed)){
                remaining.put(currChange.getKey(),currChange.getValue());
            }
        }
        previouslyLeftOutProposals.clear();
        previouslyLeftOutProposals.putAll(remaining);

        /* process proposals for the current loop */
        if(rnd.nextInt(2) == 0){
            if(rnd.nextInt(2) == 0){
                for(int x = 1; x < sizeX-1; ++x) for(int y = 1; y < sizeY-1; ++y)
                    createProposalForCell(x, y, units, previouslyLeftOutProposals, proposedChanges, already_changed);
            }else{
                for(int x = 1; x < sizeX-1; ++x) for(int y = sizeY-2; y > 0; --y)
                    createProposalForCell(x, y, units, previouslyLeftOutProposals, proposedChanges, already_changed);
            }
        }else{
            if(rnd.nextInt(2) == 0){
                for(int x = sizeX-2; x > 0; --x) for(int y = 1; y < sizeY-1; ++y)
                    createProposalForCell(x, y, units, previouslyLeftOutProposals, proposedChanges, already_changed);
            }else{
                for(int x = sizeX-2; x > 0; --x) for(int y = sizeY-2; y > 0; --y)
                    createProposalForCell(x, y, units, previouslyLeftOutProposals, proposedChanges, already_changed);
            }
        }

        /* apply changes */
        for(Map.Entry<MiscUtil.MyCell, MiscUtil.MyCell> curr_change : proposedChanges.entrySet()){
            int source_x = curr_change.getKey().getIX();
            int source_y = curr_change.getKey().getIY();
            int target_x = curr_change.getValue().getIX();
            int target_y = curr_change.getValue().getIY();
            if(
                Material.Elements.Ether == blocks[source_x][source_y]
                && Material.Elements.Ether != blocks[target_x][target_y]
                &&!Material.discardable(blocks[target_x][target_y], units[target_x][target_y])
            ){ /* In case the material to move is Ether, and it has a relaitvely big force */ /* TODO: Make force transfer depending on the state of the matter, to make crystals stable */
                forces[target_x][target_y].add(forces[source_x][source_y]);
                blocks[source_x][source_y] = Material.Elements.Air;
                forces[target_x][target_y].scl(units[source_x][source_y]);
//                units[target_x][target_y] += units[source_x][source_y];
//                units[source_x][source_y] = 0.01f;
            }else
            if(
                Material.Elements.Ether == blocks[target_x][target_y]
                && Material.Elements.Ether != blocks[source_x][source_y]
            ){
                forces[source_x][source_y].add(forces[target_x][target_y]);
                blocks[target_x][target_y] = Material.Elements.Air;
                forces[source_x][source_y].scl(units[target_x][target_y]);
            }else
            if(
                Material.discardable(blocks[target_x][target_y],units[target_x][target_y])
                ||(
                    (getWeight(source_x,source_y,units) > getWeight(target_x,target_y, units))
                    && Material.movable(blocks[target_x][target_y],units[target_x][target_y])
                )
            ){
                /* swap the 2 cells, decreasing the forces on both */
                forces[source_x][source_y].add(
                    (-forces[source_x][source_y].x * (Math.abs(getWeight(source_x,source_y,units)) / Math.max(0.00001f, Math.max(Math.abs(getWeight(source_x,source_y,units)), forces[source_x][source_y].x)))),
                    (-forces[source_x][source_y].y * (Math.abs(getWeight(source_x,source_y,units)) / Math.max(0.00001f, Math.max(Math.abs(getWeight(source_x,source_y,units)), forces[source_x][source_y].y))))
                );
                forces[source_x][source_y].add(
                    (int)(myMiscUtil.getGravity(source_x,source_y).x * getWeight(source_x,source_y,units)),
                    (int)(myMiscUtil.getGravity(source_x,source_y).y * getWeight(source_x,source_y,units))
                );

                parent.switchElements(curr_change.getKey(),curr_change.getValue());
            }else{ /* The cells collide, updating forces, but no swapping */
                float m1 = getWeight(source_x, source_y, units);
                Vector2 u1 = forces[source_x][source_y].fCpy().nor();
                float m2 = getWeight(target_x, target_y, units);
                Vector2 u2 = forces[target_x][target_y].fCpy().nor();
                Vector2 result_speed = new Vector2();
                result_speed.set( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                    ((m1 - m2)/(m1+m2)*u1.x) + (2*m2/(m1+m2))*u2.x,
                    ((m1 - m2)/(m1+m2)*u1.y) + (2*m2/(m1+m2))*u2.y
                );
                forces[source_x][source_y].set( /* F = m*a --> `a` is the delta v, which is the change in the velocity */
                    (m1 * (result_speed.x - u1.x)),
                    (m1 * (result_speed.y - u1.y))
                );
                forces[source_x][source_y].add(
                myMiscUtil.getGravity(source_x,source_y).x * getWeight(source_x,source_y,units),
                myMiscUtil.getGravity(source_x,source_y).y * getWeight(source_x,source_y,units)
                );
                gravityCorrectionAmount[source_x][source_y] = 0;
                if(Material.movable(blocks[target_x][target_y],units[target_x][target_y])){
                    result_speed.set( /*!Note: it is supposed, that non-movable cells do not initiate movement */
                        (2*m1/(m1+m2))*u1.x + ((m2-m1)/(m1+m2)*u2.x),
                        (2*m1/(m1+m2))*u1.y + ((m2-m1)/(m1+m2)*u2.y)
                    );
                    forces[target_x][target_y].set( m2 * (result_speed.x - u2.x), m2 * (result_speed.y - u2.y) );
                    forces[target_x][target_y].add( /* Since forces are changed, gravity correction shall be done in-place */
                    myMiscUtil.getGravity(target_x,target_y).x * getWeight(target_x,target_y,units),
                    myMiscUtil.getGravity(target_x,target_y).y * getWeight(target_x,target_y,units)
                    );
                    gravityCorrectionAmount[target_x][target_y] = 0;
                } /* do not update the force for unmovable objects */
            }
            touchedByMechanics[source_x][source_y] = 1;
            touchedByMechanics[target_x][target_y] = 1;
        }
    }

    private void createProposalForCell(
            int x, int y, int[][] units,
            HashMap<MiscUtil.MyCell, MiscUtil.MyCell> previously_left_out_proposals,
            HashMap<MiscUtil.MyCell, MiscUtil.MyCell> proposed_changes, HashSet<Integer> already_changed
    ){
        MiscUtil.MyCell intendedSourceCell = new MiscUtil.MyCell(sizeX);
        MiscUtil.MyCell intendedTargetCell = new MiscUtil.MyCell(sizeX);
        Vector2 target_final_position = new Vector2();
        if( !Material.discardable(blocks[x][y], units[x][y]) && (1 <= forces[x][y].len()) ){
            intendedSourceCell.set(x,y);
            intendedTargetCell.set(x,y);
            if(1 < Math.abs(forces[x][y].x))intendedTargetCell.set(
                x + Math.max(-1, Math.min(forces[x][y].x,1)), intendedTargetCell.y
            );
            if(1 < Math.abs(forces[x][y].y))intendedTargetCell.set(
                intendedTargetCell.x, y + Math.max(-1,Math.min(forces[x][y].y,1))
            );

            /* calculate the final position of the intended target cell */
            target_final_position.set(intendedTargetCell.x,intendedTargetCell.y);
            if(1 < Math.abs(forces[intendedTargetCell.getIX()][intendedTargetCell.getIY()].x))
                target_final_position.set(
                    intendedTargetCell.x + Math.max(-1.1f, Math.min(forces[intendedTargetCell.getIX()][intendedTargetCell.getIY()].x,1.1f)),
                    intendedTargetCell.y
                );
            if(1 < Math.abs(forces[intendedTargetCell.getIX()][intendedTargetCell.getIY()].y))
                target_final_position.set(
                    intendedTargetCell.x,
                    intendedTargetCell.y + Math.max(-1.1f,Math.min(forces[intendedTargetCell.getIX()][intendedTargetCell.getIY()].y,1.1f))
                );

            /* see if the two cells still intersect with forces included */
            if(2 > intendedSourceCell.dst(target_final_position)){
                if(!evaluateForMechanics(units, intendedSourceCell,intendedTargetCell,proposed_changes,already_changed)){
                    previously_left_out_proposals.put(new MiscUtil.MyCell(intendedSourceCell),new MiscUtil.MyCell(intendedTargetCell));
                    /* Since these cells are left out, add no gravity to them! */
                    gravityCorrectionAmount[intendedSourceCell.getIX()][intendedSourceCell.getIY()] = 0;
                    gravityCorrectionAmount[intendedTargetCell.getIX()][intendedTargetCell.getIY()] = 0;
                }
            }
        }
    }

    /**
     *  A Function to try and propose cell pairs to change in this mechanics iteration
     * @param units
     * @param source_cell
     * @param targetCell
     * @param alreadyProposedChanges
     * @param alreadyChanged
     * @return whether or not the cells could be placed into the already proposed changes
     */
    private boolean evaluateForMechanics(
            int[][] units, MiscUtil.MyCell source_cell, MiscUtil.MyCell targetCell,
            HashMap<MiscUtil.MyCell, MiscUtil.MyCell> alreadyProposedChanges, HashSet<Integer> alreadyChanged
    ){
        int x = source_cell.getIX();
        int y = source_cell.getIY();
        int intendedX = targetCell.getIX();
        int intendedY = targetCell.getIY();
        if(
            !((x == intendedX) && (y == intendedY))
            &&( /* In case both is discardable, then no operations shall commence */
                !Material.discardable(blocks[x][y],units[x][y])
                ||!Material.discardable(blocks[intendedX][intendedY],units[intendedX][intendedY])
            )
            &&(!alreadyChanged.contains(MathUtils.coordinateToHash(x,y,sizeX)))
            &&(!alreadyChanged.contains(MathUtils.coordinateToHash(intendedX,intendedY,sizeX)))
        ){
            if(velocityMaxTicks == velocityTicks[x][y]){
                alreadyProposedChanges.put(new MiscUtil.MyCell(x,y,sizeX),new MiscUtil.MyCell(intendedX,intendedY,sizeX));
                alreadyChanged.add(MathUtils.coordinateToHash(x,y,sizeX));
                alreadyChanged.add(MathUtils.coordinateToHash(intendedX,intendedY,sizeX));
                velocityTicks[x][y] = 0;
                return true;
            }else ++velocityTicks[x][y];
        } /* Able to process mechanics on the 2 blocks */
        return false;
    }

    @Override
    public void postProcess(int[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                blocks[x][y] = parent.etherealPlane.elementAt(x,y);
            }
        }
    }

    /**
     * Create a simple pond with some fire on one side
     * @param floorHeight - the height of the ground floor
     */
    public void pondWithGrill(int[][] units, int floorHeight){
        for(int x = 0;x < sizeX; ++x){ /* create the ground floor */
            for(int y = 0; y < sizeY; ++y){
                forces[x][y].set(0,0);
                if(y <= floorHeight){
                    blocks[x][y] = Material.Elements.Earth;
                    if(y <= (floorHeight/2)) units[x][y] = Math.min(100,rnd.nextInt(500));
                    else units[x][y] = Math.min(15,rnd.nextInt(50));
                }else{
                    blocks[x][y] = Material.Elements.Air;
                    units[x][y] = Math.min(3,rnd.nextInt(10));
                }
            }
        }

        int posX; int posY; /* Create the pond */
        for(float radius = 0; radius < (floorHeight/2.0f); radius += 0.5f){
            for(float sector = (float)Math.PI * 0.99f; sector < Math.PI * 2.01f; sector += Math.PI / 180){
                posX = (int)(sizeX/2 + (float)Math.cos(sector) * radius);
                posX = Math.max(0, Math.min(sizeX, posX));
                posY = (int)(floorHeight + (float)Math.sin(sector) * radius);
                posY = Math.max(0, Math.min(sizeY, posY));
                if(posY <= (floorHeight - (floorHeight/4)) && (0 == rnd.nextInt(3)))
                    units[posX][posY] *= 2.5f;
                blocks[posX][posY] = Material.Elements.Water;
            }
        }

        /* Create a fire */
        posX = sizeX/4;
        posY = floorHeight + 1;
        blocks[posX][posY] = Material.Elements.Fire;
        blocks[posX-1][posY] = Material.Elements.Fire;
        blocks[posX+1][posY] = Material.Elements.Fire;
        blocks[posX][posY+1] = Material.Elements.Fire;
    }

    public Material.Elements elementAt(int x, int y){
        return blocks[x][y];
    }

    public Color getColor(int x, int y, int[][] units){
        return Material.getColor(blocks[x][y], units[x][y]).cpy();
    }

    public Color getDebugColor(int x, int y, int[][] units){
        Color defColor = getColor(x,y, units).cpy();
        if(0 < touchedByMechanics[x][y]){ /* it was modified.. */
//            defColor.lerp(Color.GREEN, 0.5f); /* to see if it was touched by the mechanics */
            defColor.lerp(new Color(
                0.0f,//Math.max(1.0f, Math.min(0.0f, forces[x][y].x)),
                //-Math.max(0.0f, Math.min(-5.0f, forces[x][y].y))/5.0f,
                    (0 == touchedByMechanics[x][y])?0.0f:1.0f,
                0.0f,
                1.0f
            ), 0.5f);
        }
        return defColor;
    }

}
