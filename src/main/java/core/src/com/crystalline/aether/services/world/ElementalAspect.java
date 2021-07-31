package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.services.CPUBackend;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

public class ElementalAspect extends RealityAspect {
    MiscUtils myMiscUtils;
    protected final int sizeX;
    protected final int sizeY;
    private final int velocityMaxTicks = 3;
    private final Random rnd = new Random();

    /**
     * A texture image representing the elemental properties of reality
     * - R: block type --> Material.Elements
     * - G:
     * - B:
     * - A: priority --> A unique number to decide arbitration while switching cells
     */
    private FloatBuffer elements;

    /* TODO: Make one slot free ( for force z ) --> evaluate buffer places based on dependencies */
    /**
     * A texture image representing the dynamism of a cell
     * - R: x of the force vector active on the block
     * - G: y of the force vector active on the block
     * - B:
     * - A: gravity correction amount ( helps to not add gravity in the intermediary steps of the mechanics evaluation )
     */
    private FloatBuffer dynamics;

    /**
     * A texture image representing each cells intention to switch to another cell
     * - R: the offset code for the target, which is to be used in accordance with the coordinates of the source cell
     * - G: A byte deciding whether or not to apply this change in the next round
     * - B: the velocity tick of the cell ( 0 means the cell would move )
     * - A:
     */
    private final FloatBuffer proposedChanges;

    private float[][] touchedByMechanics; /* Debug purposes */
    private float[][] unitDebugVariable; /* Debug purposes */

    private final CPUBackend backend;
    private final int processUnitsPhaseIndex;
    private final int processTypesPhaseIndex;
    private final int processTypeUnitsPhaseIndex;
    private final int defineByEtherealPhaseIndex;

    private final FloatBuffer[] processUnitsPhaseInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] processTypeUnitsPhaseInputs;
    private final FloatBuffer[] defineByEtherealPhaseInputs;

    public ElementalAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        myMiscUtils = new MiscUtils();
        elements = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        dynamics = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        proposedChanges = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        touchedByMechanics = new float[sizeX][sizeY];
        unitDebugVariable = new float[sizeX][sizeY];
        backend = new CPUBackend();
        processUnitsPhaseInputs = new FloatBuffer[]{elements, null};
        processUnitsPhaseIndex = backend.addPhase(this::processUnitsPhase, (Config.bufferCellSize * sizeX * sizeY));
        processTypesPhaseInputs = new FloatBuffer[]{elements,null,null};
        processTypesPhaseIndex = backend.addPhase(this::processTypesPhase, (Config.bufferCellSize * sizeX * sizeY));
        processTypeUnitsPhaseInputs = new FloatBuffer[]{elements,null};
        processTypeUnitsPhaseIndex = backend.addPhase(this::processTypeUnitsPhase, (Config.bufferCellSize * sizeX * sizeY));
        defineByEtherealPhaseInputs = new FloatBuffer[1];
        defineByEtherealPhaseIndex = backend.addPhase(this::defineByEtherealPhase, (Config.bufferCellSize * sizeX * sizeY));

        calculatePriority();
        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
            BufferUtils.clone(elements),
            BufferUtils.clone(dynamics),
            Arrays.copyOf(touchedByMechanics, touchedByMechanics.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        elements = (FloatBuffer) state[0];
        dynamics = (FloatBuffer) state[1];
        touchedByMechanics = (float[][]) state[2];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            setElement(x,y,Material.Elements.Air);
            setForce(x,y, sizeX, dynamics,0,0);
            setGravityCorrection(x,y, sizeX, dynamics,0);
            setVelocityTick(x,y, sizeX, proposedChanges, velocityMaxTicks);
            touchedByMechanics[x][y] = 0;
        } }
    }

    public static void setPriority(int x, int y, int sizeX, FloatBuffer buffer, float prio){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,3, buffer, prio);
    }

    public static float getPriority(int x, int y, int sizeX, FloatBuffer elements){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,3, elements);
    }

    public static boolean aWinsOverB(int ax, int ay, int bx, int by, int sizeX, FloatBuffer elements, FloatBuffer dynamics){
        if(getForce(ax,ay,sizeX,dynamics).len() == getForce(bx,by,sizeX,dynamics).len()){
            return (getPriority(ax,ay,sizeX,elements) > getPriority(bx,by,sizeX,elements));
        }else return (getForce(ax,ay,sizeX,dynamics).len() > getForce(bx,by,sizeX,dynamics).len());
    }

    /**
     * To tell that each cell shall have a priority calculated from the formulae:
     *     y + x%4 + (y%4)*2 + (x%8)*2 + (y%8)*2 + (x%8)/2 + (y%8)/2
     */
    private void calculatePriority(){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            setPriority(x,y,sizeX,elements,(float)(
                (y + x%4 + (y%4)*2 + (x%8)*2 + (y%8)*2 + (x%8)/2 + (y%8)/2)
            ));
        } }
    }

    private void defineByEtherealPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            ElementalAspect.setElement(x,y, sizeX, output, EtherealAspect.getElementEnum(x,y, sizeX, inputs[0]));
        } }
    }

    public void defineBy(EtherealAspect plane){
        plane.provideEtherTo(defineByEtherealPhaseInputs, 0);
        backend.setInputs(defineByEtherealPhaseInputs);
        backend.runPhase(defineByEtherealPhaseIndex);
        BufferUtils.copy(backend.getOutput(defineByEtherealPhaseIndex), elements);
    }

    private float avgOfUnit(int x, int y, FloatBuffer elements, FloatBuffer scalars, Material.Elements type){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(getElementEnum(nx,ny, sizeX, elements) == type){
                    average_val += World.getUnit(nx,ny,sizeX,scalars);
                    division += 1;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private int numOfElements(int x, int y, FloatBuffer elements, Material.Elements type){
        int num = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(getElementEnum(nx,ny, sizeX, elements) == type){
                    ++num;
                }
            }
        }
        return num;
    }

    private float avgOfUnitsWithinDistance(int x, int y, FloatBuffer elements, FloatBuffer scalars){
        float average_val = World.getUnit(x,y, sizeX, scalars);
        float division = 1;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeY, y + 2); ++ny) {
                if(
                    (x != nx) && (y != ny)
                    &&Material.isSameMat(
                        getElementEnum(x,y, sizeX, elements), World.getUnit(x,y, sizeX, scalars),
                        getElementEnum(nx,ny, sizeX, elements), World.getUnit(nx,ny, sizeX, scalars)
                    )
                ){
                    average_val += World.getUnit(nx,ny, sizeX, scalars);
                    division += 1;
                }
            }
        }
        average_val /= division;
        return average_val;
    }

    @Override
    public FloatBuffer determineUnits(World parent) {
        return null; /* Don't modify anything */
    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        Material.Elements tmpBloc = getElement(toX,toY);
        setElement(toX,toY, getElement(fromX,fromY));
        setElement(fromX,fromY,tmpBloc);
        Vector2 tmpVec = getForce(toX,toY, sizeX, dynamics).cpy();
        setForce(toX,toY, sizeX, dynamics,getForce(fromX,fromY, sizeX, dynamics));
        setForce(fromX,fromY, sizeX, dynamics,tmpVec);
    }

    private void processUnitsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < sizeX; ++x) { for (int y = 0; y < sizeY; ++y) { /* Calculate dilution */
            if(Material.movable(getElementEnum(x,y, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[1]))) {
                World.setUnit(x,y,sizeX, output, avgOfUnitsWithinDistance(x,y,inputs[0], inputs[1]));
            }else{
                World.setUnit(x,y, sizeX, output, World.getUnit(x,y, sizeX, inputs[1]));
            }
        } }
    }

    @Override
    public void processUnits(World parent){
        processUnitsPhaseInputs[0] = elements;
        parent.provideScalarsTo(processUnitsPhaseInputs,1);
        backend.setInputs(processUnitsPhaseInputs);
        backend.runPhase(processUnitsPhaseIndex);
        parent.setScalars(backend.getOutput(processUnitsPhaseIndex));
    }

    private void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = sizeX - 1;x >= 0; --x)for(int y = sizeY - 1 ; y >= 0; --y) {
            Material.Elements currentElement = EtherealAspect.getElementEnum(x,y,sizeX,inputs[1]);
            float currentUnit = World.getUnit(x,y,sizeX, inputs[2]);
            if(Material.Elements.Water == currentElement){ /* TODO: This will be ill-defined in a multi-threaded environment */
                if(avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water) < avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire)){
                    currentElement = Material.Elements.Air;
                }
            }

            if(Material.Elements.Air == currentElement) { /* TODO: make air catch fire! */
                if(
                    (numOfElements(x,y,inputs[0], Material.Elements.Air) < numOfElements(x,y,inputs[0], Material.Elements.Fire))
                    &&(0 == avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water))
                ){
                    currentElement = Material.Elements.Fire;
                }
            }

            /* TODO: Store Flammability */
            /* TODO: Make fire springing out from Earth */
            if(Material.Elements.Fire == currentElement){
                 /* TODO: Make lava cool off to earth by heat */
                if(avgOfUnit(x,y,inputs[0],inputs[2],Material.Elements.Water) > avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Fire)){
                    currentElement = Material.Elements.Earth;
                }
            }

            if(Material.Elements.Earth == currentElement){
                /* TODO: Make Earth keep track of heat instead of units */
                if((avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Earth) < avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire))){
                    if( /* TODO: Make sand melt "into" glass */
                        Material.MechaProperties.Solid.ordinal() > Material.getState(Material.Elements.Earth, currentUnit).ordinal()
                        || Material.MechaProperties.Plasma.ordinal() < Material.getState(Material.Elements.Fire, currentUnit).ordinal()
                    )currentElement = Material.Elements.Fire;
                }
            }
            setElement(x,y,sizeX,output,currentElement);
        }
    }

    private void processTypeUnitsPhase(FloatBuffer[] inputs, FloatBuffer output) {
        for(int x = sizeX - 1;x >= 0; --x) for(int y = sizeY - 1 ; y >= 0; --y) {
            Material.Elements currentElement = getElementEnum(x,y,sizeX,inputs[0]);
            float currentUnit = World.getUnit(x,y,sizeX, inputs[1]);
            if(Material.Elements.Water == currentElement){
                if(y > (sizeY * 0.9)){
                    currentUnit -= currentUnit * 0.02f;
                }
            }

            if(Material.Elements.Fire == currentElement){
                if(
                    (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                    && (currentUnit < avgOfUnit(x,y,inputs[0],inputs[1], Material.Elements.Fire))
                ){
                    currentUnit -= currentUnit * 0.5f;
                }else
                if(
                    (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                ){
                    currentUnit -= currentUnit * 0.3f;
                }
            }

            /* TODO: Make nearby fire consume compatible Earth */
            currentUnit = Math.max(0.1f,currentUnit);
            World.setUnit(x,y,sizeX,output,currentUnit);
        }
    }

    @Override
    public void processTypes(World parent) {
        processTypesPhaseInputs[0] = elements;
        parent.getEtherealPlane().provideEtherTo(processTypesPhaseInputs,1);
        parent.provideScalarsTo(processTypesPhaseInputs,2);
        backend.setInputs(processTypesPhaseInputs);
        backend.runPhase(processTypesPhaseIndex);
        BufferUtils.copy(backend.getOutput(processTypesPhaseIndex),elements);

        processTypeUnitsPhaseInputs[0] = elements;
        parent.provideScalarsTo(processTypeUnitsPhaseInputs,1);
        backend.setInputs(processTypeUnitsPhaseInputs);
        backend.runPhase(processTypeUnitsPhaseIndex);
        parent.setScalars(backend.getOutput(processTypeUnitsPhaseIndex));
    }

    public float getWeight(int x, int y, World parent){
        /* TODO: Weight to include pressure somehow? or at least the same materials on top */
        return (
            parent.getUnit(x,y) * Material.TYPE_SPECIFIC_GRAVITY[getElement(x,y).ordinal()][MiscUtils.indexIn(
                Material.TYPE_UNIT_SELECTOR[getElement(x,y).ordinal()], parent.getUnit(x,y)
            )]
        );
    }

    public static float getOffsetCode(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer);
    }

    public static void setOffsetCode(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0, buffer, value);
    }

    public static int getXFromOffsetCode(int x, int code){
        switch(code){
            case 1: case 8: case 7: return (x-1);
            case 2: case 0: case 6: return (x);
            case 3: case 4: case 5: return (x+1);
        }
        return x;
    }
    public static int getYFromOffsetCode(int y, int code){
        switch(code){
            case 7: case 6: case 5: return (y+1);
            case 8: case 0: case 4: return (y);
            case 1: case 2: case 3: return (y-1);
        }
        return y;
    }

    public static int getOffsetCode(int ox, int oy){
        if((ox < 0)&&(oy < 0)) return 1;
        if((ox == 0)&&(oy < 0)) return 2;
        if((ox > 0)&&(oy < 0)) return 3;
        if((ox > 0)&&(oy == 0)) return 4;
        if((ox > 0)/*&&(oy > 0)*/) return 5;
        if((ox == 0)&&(oy > 0)) return 6;
        if((ox < 0)&&(oy > 0)) return 7;
        if((ox < 0)/*&&(oy == 0)*/) return 8;
        return 0;
    }

    public static int getTargetX(int x, int y, int sizeX, FloatBuffer buffer){
        return getXFromOffsetCode(x,(int)getOffsetCode(x,y,sizeX,buffer));
    }

    public static int getTargetY(int x, int y, int sizeX, FloatBuffer buffer){
        return getYFromOffsetCode(y,(int)getOffsetCode(x,y,sizeX,buffer));
    }

    /**
     * A function to calculate a target to switch to for each cell
     * @param inputs: [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output the resulting proposed changes, velocity tick values
     */
    private void proposeChangesFromForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            int intendedX = (int)Math.min( 1.0f, Math.max(-1.0f, getForceX(x,y,sizeX,output)) );
            int intendedY = (int)Math.min( 1.0f, Math.max(-1.0f, getForceY(x,y,sizeX,output)) );
            float offsetCode = 0;
            int newVelocityTick = getVelocityTick(x,y, sizeX, inputs[0]);
            if(
                !((x == intendedX) && (y == intendedY))
                &&( /* In case both is discardable, then no operations shall commence */
                    !Material.discardable(
                        getElementEnum(x,y, sizeX, inputs[1]),
                        World.getUnit(x,y, sizeX, inputs[3])
                    )||!Material.discardable(
                        getElementEnum(intendedX,intendedY, sizeX, inputs[1]),
                        World.getUnit(intendedX,intendedY, sizeX, inputs[3])
                    )
                )
                /* TODO: Handle if a movable material collides with a non-movable */
//                &&(isMovable(x,y,sizeX,inputs[2],inputs[3]))
//                &&(isMovable(intendedX,intendedY,sizeX,inputs[2],inputs[3]))
            ){
                if(velocityMaxTicks == getVelocityTick(x,y, sizeX, inputs[0]))
                    offsetCode = getOffsetCode(intendedX,intendedY);
                else increaseVelocityTick(x,y, sizeX, inputs[0]);
            } /* Able to process mechanics on the 2 blocks */

            setVelocityTick(x,y, sizeX, output,newVelocityTick);
            setOffsetCode(x,y,sizeX,output,offsetCode);
        } }
    }

    public static int getHighestPriorityNeighbourPointingToCell(
        int x, int y, int sizeX, FloatBuffer previousChanges, FloatBuffer elements, FloatBuffer dynamics
    ){
        int nx = x, ny = y;
        int currX, currY;
        for(int i = 0; i < 9; ++i){
            currX = getXFromOffsetCode(x,i);
            currY = getYFromOffsetCode(y,i);
            if( /* Each neighbour of c pointing to it shall be arbitrated based on priority */
                (x == getTargetX(currX,currY,sizeX,previousChanges))
                &&(y == getTargetY(currX,currY,sizeX,previousChanges))
                &&( /* Either no previously neighbour points to c */
                    ((nx == x)&&(ny == y)) /* or the current has higher priority */
                    ||aWinsOverB(currX,currY,nx,ny,sizeX,elements,dynamics)
                )
            ){
                nx = currX;
                ny = currY;
            }
        }
        return getOffsetCode((nx-x),(ny-y));
    }

    public static int getThreatCountForCellOrTarget(int x, int y, int sizeX, FloatBuffer previousChanges, FloatBuffer elements, FloatBuffer dynamics){
        int threatCount = 0;
        int targetX = getTargetX(x,y,sizeX,previousChanges);
        int targetY = getTargetY(x,y,sizeX,previousChanges);
        int ix,iy;
        for(int i = 0; i < 9; ++i){ /* Count threats for cell from cell */
            ix = getXFromOffsetCode(x,i);
            iy = getYFromOffsetCode(y,i);
            int targetOfTargetX = getTargetX(ix,iy,sizeX,previousChanges);
            int targetOfTargetY = getTargetY(ix,iy,sizeX,previousChanges);
            if(
                ((ix != x) && (iy != y))
                &&((targetX != x) && (targetY != y))
                &&(
                    ((targetOfTargetX == x) && (targetOfTargetY == y))
                    ||((targetOfTargetX == targetX) && (targetOfTargetY == targetY))
                )
                &&(aWinsOverB(ix,iy,x,y,sizeX,elements,dynamics))
            )++threatCount;
        }
        for(int i = 0; i < 9; ++i) { /* Count threats for target, compare to cell */
            ix = getXFromOffsetCode(targetX,i);
            iy = getYFromOffsetCode(targetY,i);
            int targetOfTargetX = getTargetX(ix,iy,sizeX,previousChanges);
            int targetOfTargetY = getTargetY(ix,iy,sizeX,previousChanges);
            if(
                    ((ix != x) && (iy != y))
                    &&((targetX != x) && (targetY != y))
                    &&(
                        ((targetOfTargetX == x) && (targetOfTargetY == y))
                        ||((targetOfTargetX == targetX) && (targetOfTargetY == targetY))
                    )
                    &&(aWinsOverB(ix,iy,x,y,sizeX,elements,dynamics))
            )++threatCount;
        }
        return threatCount;
    }

    /**
     * Decides which changes are to be applied, and which would not
     * @param inputs [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output the arbitrated changes, with the toApply part set
     */
    private void arbitrateChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int cx = 0; cx < sizeX; ++cx){ for(int cy = 0; cy < sizeY; ++cy) {
            int tx = getTargetX(cx,cy,sizeX,inputs[0]);
            int ty = getTargetY(cx,cy,sizeX,inputs[0]);
            float applyBit = 0; /* only apply changes approved by the algorithm ( setting the apply bit there ) */
            float offsetCodeForTarget = getOffsetCode(cx,cy,sizeX,inputs[0]);
            if(!(tx == cx)&&(ty == cy)) { /* c has a target other, than itself */
                /* count the number of cells targeting c or t; with higher priority */
                /* if target wins over source and target of target */

                if(0 == getThreatCountForCellOrTarget(cx,cy, sizeX, inputs[0], inputs[1], inputs[2])){
                    applyBit = 1;
                    /* if source wins over both target and target of target */
                    /* then cell will be target */
                    /* else if target of target is the source cell, cell will be target */
                    /* TODO:  what will be in its place???? */
                }
            }
            setOffsetCode(cx,cy,sizeX,output,applyBit);
            setOffsetCode(cx,cy,sizeX,output,offsetCodeForTarget);
        } }
    }

    @Override
    public void processMechanics(World parent) {
        /* Init Mechanics phase */
        /* TODO: initialize previously left out proposals as well to 0 */
        HashMap<MiscUtils.MyCell, MiscUtils.MyCell> remaining_proposed_changes = new HashMap<>();
        for(int x = 1; x < sizeX-1; ++x){ /* Pre-process: Add gravity, and nullify forces on discardable objects; */
            for(int y = sizeY-2; y > 0; --y){
                touchedByMechanics[x][y] = 0;
            }
        }

        /* Main Mechanic phase */
        for(int i = 0; i < velocityMaxTicks; ++i){
//            processMechanicsBackend(parent,remaining_proposed_changes);
            /* Apply changes not applied, but arbitrated from the last loop */
            //
            /* Propose changes from forces */
            /*!Note: Simply put: forces of the cell generate the target whom it wants to switch with */
            //proposeChangesFromForces
            /* Arbitrate changes */
            //
            /* Apply arbitrated changes */
            //
        }

        /* Mechanic post-process phase */
        for(Map.Entry<MiscUtils.MyCell, MiscUtils.MyCell> missedCells : remaining_proposed_changes.entrySet()){
            setGravityCorrection(
                missedCells.getKey().getIX(), missedCells.getKey().getIY(), sizeX, dynamics,
                getWeight(missedCells.getKey().getIX(),missedCells.getKey().getIY(),parent)
            );
            setGravityCorrection(
                missedCells.getValue().getIX(),missedCells.getValue().getIY(), sizeX, dynamics,
                getWeight(missedCells.getValue().getIX(),missedCells.getValue().getIY(),parent)
            );
        }

        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                if(Material.movable(getElement(x,y), parent.getUnit(x,y))){
                    addToForceX(x,y, sizeX, dynamics, (myMiscUtils.getGravity(x,y).x * getGravityCorrection(x,y, sizeX, dynamics)));
                    addToForceY(x,y, sizeX, dynamics, (myMiscUtils.getGravity(x,y).y * getGravityCorrection(x,y, sizeX, dynamics)));
                }
            }
        }
        /* TODO: Refine velocity to be based on "ticks" of movements, (rather than)/(in cooperation) with multiple processing loops  */
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    public void processMechanicsBackend(World parent, HashMap<MiscUtils.MyCell, MiscUtils.MyCell> previouslyLeftOutProposals){
        /* Adding forces phase */
        /* update forces based on context, calculate intended velocities based on them */
        for(int x = 1; x < sizeX-1; ++x){
            for(int y = 1; y < sizeY-1; ++y){
                if(!Material.discardable(getElement(x,y), parent.getUnit(x,y))){
                    setGravityCorrection(x,y, sizeX, dynamics,getWeight(x,y,parent));
                    setVelocityTick(x,y, sizeX, proposedChanges, velocityMaxTicks);
                }else{
                    setForce(x,y, sizeX, dynamics,0,0);
                    setGravityCorrection(x,y, sizeX, dynamics,0);
                }
                if(Material.Elements.Ether == getElement(x,y)){
                    for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                        if ( /* in the bounds of the chunk */
                            (0 <= nx)&&(sizeX > nx)&&(0 <= ny)&&(sizeY > ny)
                            &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* after the not immediate neighbourhood */
                            &&(parent.getUnit(x,y) <= parent.getUnit(nx,ny))
                            &&(Material.Elements.Ether == getElement(nx,ny))
                        ){ /* Calculate forces from surplus ethers */
                            float aether_diff = Math.max(-10.5f, Math.min(10.5f, (
                                parent.getEtherealPlane().aetherValueAt(x,y) - parent.getEtherealPlane().aetherValueAt(nx,ny)
                            )));
                            addToForceX(x, y, sizeX, dynamics, ((nx - x) * aether_diff));
                            addToForceY(x, y, sizeX, dynamics, ((ny - y) * aether_diff));
                        }
                    }
                }

                if(Material.MechaProperties.Fluid == Material.getState(getElement(x,y), parent.getUnit(x,y))){
                    if(Material.isSameMat(getElement(x,y), parent.getUnit(x,y), getElement(x,y-1), parent.getUnit(x,y-1))) {
                      /* The random method */
//                        forces[x][y].set((rnd.nextInt(7)-3),1);
                        /* The amplify method */
                         /* the cell is a liquid on top of another liquid, so it must move. */
                        if(0.0f < getForceX(x,y, sizeX, dynamics)) setForceX(x,y, sizeX, dynamics,getForceX(x,y, sizeX, dynamics) * 4);
                        else setForce(x,y, sizeX, dynamics, (rnd.nextInt(6) - 3), 1);
                        /* The particle method */
//                        float divisor = 1.0f;
//                        for (int nx = (x - 1); nx < (x + 2); ++nx)
//                            for (int ny = (y - 1); ny < (y + 2); ++ny) {
//                                /* B-A * d' == B'*/
//                                if (
//                                        (x != nx) && (y != ny)
//                                                && Material.isSameMat(x, y, nx, ny, blocks, units)
//                                ) {
//                                    forces[x][y].add(
//                                            (3.0f * (x - nx))
//                                                    ,//+ rnd.nextInt(4) - 2,
//                                            (5.0f * (y - ny))
//                                    );
//                                    divisor += 1.0f;
//                                }
//                            }
//                        if(1.0f < divisor){
//                            if(y%2 == 0)forces[x][y].add(4,0);
//                            else forces[x][y].add(-4,0);
//                        }
//                        forces[x][y].div(divisor);
//                        forces[x][y].add(
//                                myMiscUtil.getGravity(x, y).x * getWeight(x, y, units),
//                                myMiscUtil.getGravity(x, y).y * getWeight(x, y, units)
//                        );
                    }
                }else if(Material.MechaProperties.Plasma == Material.getState(getElement(x,y), parent.getUnit(x,y))){
                    addToForce(x,y, sizeX, dynamics,(rnd.nextInt(4)-2),0);
                }/* TODO: Make gases loom, instead of staying still ( move about a bit maybe? )  */
            }
        }

        /* TODO: First come first served to be replaced by a better system */
        /*!Note: Proposed changes are of the structure: key/source block array --> value/target block array
         * */
        HashMap<MiscUtils.MyCell, MiscUtils.MyCell> proposedChanges = new HashMap<>();
        HashSet<Integer> already_changed = new HashSet<>();
        HashMap<MiscUtils.MyCell, MiscUtils.MyCell> remaining = new HashMap<>();

        /* Create raw proposals phase */
        /*!Note: In this phase every cell proposes a neighbouring switch based on its forces */
        /*!Note: This phase contains the previously left our proposals too, which may get overwritten by different forces,
         * but in case there is no force or other interaction is present,
         * the previously left out proposals should not be overwritten by 0(==no cell switch).
         * */

        /* Apply proposals phase */


        /* Take over proposals left out from the previous process loop */
        for(Map.Entry<MiscUtils.MyCell, MiscUtils.MyCell> currChange : previouslyLeftOutProposals.entrySet()){
            if(!evaluateForMechanics(parent, currChange.getKey(), currChange.getValue(),proposedChanges,already_changed)){
                remaining.put(currChange.getKey(),currChange.getValue());
            }
        }
        previouslyLeftOutProposals.clear();
        previouslyLeftOutProposals.putAll(remaining);

        /* process proposals for the current loop */
        if(rnd.nextInt(2) == 0){
            if(rnd.nextInt(2) == 0){
                for(int x = 1; x < sizeX-1; ++x) for(int y = 1; y < sizeY-1; ++y)
                    createProposalForCell(x, y, parent, previouslyLeftOutProposals, proposedChanges, already_changed);
            }else{
                for(int x = 1; x < sizeX-1; ++x) for(int y = sizeY-2; y > 0; --y)
                    createProposalForCell(x, y, parent, previouslyLeftOutProposals, proposedChanges, already_changed);
            }
        }else{
            if(rnd.nextInt(2) == 0){
                for(int x = sizeX-2; x > 0; --x) for(int y = 1; y < sizeY-1; ++y)
                    createProposalForCell(x, y, parent, previouslyLeftOutProposals, proposedChanges, already_changed);
            }else{
                for(int x = sizeX-2; x > 0; --x) for(int y = sizeY-2; y > 0; --y)
                    createProposalForCell(x, y, parent, previouslyLeftOutProposals, proposedChanges, already_changed);
            }
        }

        /* apply changes */
        for(Map.Entry<MiscUtils.MyCell, MiscUtils.MyCell> curr_change : proposedChanges.entrySet()){
            int source_x = curr_change.getKey().getIX();
            int source_y = curr_change.getKey().getIY();
            int target_x = curr_change.getValue().getIX();
            int target_y = curr_change.getValue().getIY();
            if(
                Material.discardable(getElement(target_x,target_y),parent.getUnit(target_x,target_y))
                ||(
                    (getWeight(source_x,source_y,parent) > getWeight(target_x,target_y, parent))
                    && Material.movable(getElement(target_x,target_y),parent.getUnit(target_x,target_y))
                )
            ){
                addToForce(source_x,source_y, sizeX, dynamics, /* swap the 2 cells, decreasing the forces on both */
                    (-getForceX(source_x,source_y, sizeX, dynamics) * (
                        Math.abs(getWeight(source_x,source_y,parent))
                        / Math.max(0.00001f, Math.max(
                            Math.abs(getWeight(source_x,source_y,parent)), getForceX(source_x,source_y, sizeX, dynamics)
                        ))
                    )),
                    (-getForceY(source_x,source_y, sizeX, dynamics) * (
                        Math.abs(getWeight(source_x,source_y,parent))
                        / Math.max(0.00001f, Math.max(
                            Math.abs(getWeight(source_x,source_y,parent)), getForceY(source_x,source_y, sizeX, dynamics)
                        ))
                    ))
                );
                addToForce(source_x,source_y, sizeX, dynamics,
                    (myMiscUtils.getGravity(source_x,source_y).x * getWeight(source_x,source_y,parent)),
                    (myMiscUtils.getGravity(source_x,source_y).y * getWeight(source_x,source_y,parent))
                );
                parent.switchElements(curr_change.getKey(),curr_change.getValue());
            }else{ /* The cells collide, updating forces, but no swapping */
                float m1 = getWeight(source_x, source_y, parent);
                Vector2 u1 = getForce(source_x,source_y, sizeX, dynamics).cpy().nor();
                float m2 = getWeight(target_x, target_y, parent);
                Vector2 u2 = getForce(target_x,target_y, sizeX, dynamics).cpy().nor();
                Vector2 result_speed = new Vector2();
                result_speed.set( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                    ((m1 - m2)/(m1+m2)*u1.x) + (2*m2/(m1+m2))*u2.x,
                    ((m1 - m2)/(m1+m2)*u1.y) + (2*m2/(m1+m2))*u2.y
                );

                setForce(source_x,source_y, sizeX, dynamics, /* F = m*a --> `a` is the delta v, which is the change in the velocity */
                    (m1 * (result_speed.x - u1.x)),
                    (m1 * (result_speed.y - u1.y))
                );
                addToForce(source_x,source_y, sizeX, dynamics,
                    myMiscUtils.getGravity(source_x,source_y).x * getWeight(source_x,source_y,parent),
                    myMiscUtils.getGravity(source_x,source_y).y * getWeight(source_x,source_y,parent)
                );
                setGravityCorrection(source_x,source_y, sizeX, dynamics,0);
                if(Material.movable(getElement(target_x,target_y),parent.getUnit(target_x,target_y))){
                    result_speed.set( /*!Note: it is supposed, that non-movable cells do not initiate movement */
                        (2*m1/(m1+m2))*u1.x + ((m2-m1)/(m1+m2)*u2.x),
                        (2*m1/(m1+m2))*u1.y + ((m2-m1)/(m1+m2)*u2.y)
                    );

                    setForce(target_x,target_y, sizeX, dynamics, m2 * (result_speed.x - u2.x), m2 * (result_speed.y - u2.y) );
                    addToForce(target_x,target_y, sizeX, dynamics, /* Since forces are changed, gravity correction shall be done in-place */
                        myMiscUtils.getGravity(target_x,target_y).x * getWeight(target_x,target_y,parent),
                        myMiscUtils.getGravity(target_x,target_y).y * getWeight(target_x,target_y,parent)
                    );
                    setGravityCorrection(target_x,target_y, sizeX, dynamics,0);
                } /* do not update the force for unmovable objects */
            }
            touchedByMechanics[source_x][source_y] = 1;
            touchedByMechanics[target_x][target_y] = 1;
        }
    }

    private void createProposalForCell(
            int x, int y, World parent,
            HashMap<MiscUtils.MyCell, MiscUtils.MyCell> previously_left_out_proposals,
            HashMap<MiscUtils.MyCell, MiscUtils.MyCell> proposed_changes, HashSet<Integer> already_changed
    ){
        MiscUtils.MyCell intendedSourceCell = new MiscUtils.MyCell(sizeX);
        MiscUtils.MyCell intendedTargetCell = new MiscUtils.MyCell(sizeX);
        Vector2 target_final_position = new Vector2();
        if(
            !Material.discardable(getElement(x,y), parent.getUnit(x,y))
            && (1 <= getForce(x,y, sizeX, dynamics).len())
        ){
            intendedSourceCell.set(x,y);
            intendedTargetCell.set(x,y);
            if(1 < Math.abs(getForceX(x,y, sizeX, dynamics)))intendedTargetCell.set(
                x + Math.max(-1, Math.min(getForceX(x,y, sizeX, dynamics),1)), intendedTargetCell.y
            );
            if(1 < Math.abs(getForceY(x,y, sizeX, dynamics)))intendedTargetCell.set(
                intendedTargetCell.x, y + Math.max(-1,Math.min(getForceY(x,y, sizeX, dynamics),1))
            );

            /* calculate the final position of the intended target cell */
            target_final_position.set(intendedTargetCell.x,intendedTargetCell.y);
            if(1 < Math.abs(getForceX(intendedTargetCell.getIX(),intendedTargetCell.getIY(), sizeX, dynamics)))
                target_final_position.set(
                    intendedTargetCell.x + Math.max(-1.1f, Math.min(
                        getForceX(intendedTargetCell.getIX(),intendedTargetCell.getIY(), sizeX, dynamics),
                    1.1f)),
                    intendedTargetCell.y
                );
            if(1 < Math.abs(getForceY(intendedTargetCell.getIX(),intendedTargetCell.getIY(), sizeX, dynamics)))
                target_final_position.set(
                    intendedTargetCell.x,
                    intendedTargetCell.y + Math.max(-1.1f,Math.min(
                        getForceY(intendedTargetCell.getIX(),intendedTargetCell.getIY(), sizeX, dynamics)
                    ,1.1f))
                );

            /* see if the two cells still intersect with forces included */
            if(2 > intendedSourceCell.dst(target_final_position)){
                if(!evaluateForMechanics(parent, intendedSourceCell,intendedTargetCell,proposed_changes,already_changed)){
                    previously_left_out_proposals.put(new MiscUtils.MyCell(intendedSourceCell),new MiscUtils.MyCell(intendedTargetCell));
                    /* Since these cells are left out, add no gravity to them! */
                    setGravityCorrection(intendedSourceCell.getIX(),intendedSourceCell.getIY(), sizeX, dynamics,0);
                    setGravityCorrection(intendedTargetCell.getIX(),intendedTargetCell.getIY(), sizeX, dynamics,0);
                }
            }
        }
    }

    /**
     *  A Function to try and propose cell pairs to change in this mechanics iteration
     * @param parent the world encapsulating this elemental aspect
     * @param source_cell
     * @param targetCell
     * @param alreadyProposedChanges
     * @param alreadyChanged
     * @return whether or not the cells could be placed into the already proposed changes
     */
    private boolean evaluateForMechanics(
        World parent, MiscUtils.MyCell source_cell, MiscUtils.MyCell targetCell,
        HashMap<MiscUtils.MyCell, MiscUtils.MyCell> alreadyProposedChanges, HashSet<Integer> alreadyChanged
    ){
        int x = source_cell.getIX();
        int y = source_cell.getIY();
        int intendedX = targetCell.getIX();
        int intendedY = targetCell.getIY();
        if(
            !((x == intendedX) && (y == intendedY))
            &&( /* In case both is discardable, then no operations shall commence */
                !Material.discardable(getElement(x,y),parent.getUnit(x,y))
                ||!Material.discardable(getElement(intendedX,intendedY),parent.getUnit(intendedX,intendedY))
            )
            &&(!alreadyChanged.contains(BufferUtils.map2DTo1D(x,y,sizeX)))
            &&(!alreadyChanged.contains(BufferUtils.map2DTo1D(intendedX,intendedY,sizeX)))
        ){
            if(velocityMaxTicks == getVelocityTick(x,y, sizeX, proposedChanges)){
                alreadyProposedChanges.put(new MiscUtils.MyCell(x,y,sizeX),new MiscUtils.MyCell(intendedX,intendedY,sizeX));
                alreadyChanged.add(BufferUtils.map2DTo1D(x,y,sizeX));
                alreadyChanged.add(BufferUtils.map2DTo1D(intendedX,intendedY,sizeX));
                setVelocityTick(x,y, sizeX, proposedChanges,0);
                return true;
            }else increaseVelocityTick(x,y, sizeX, proposedChanges);
        } /* Able to process mechanics on the 2 blocks */
        return false;
    }

    @Override
    public void postProcess(World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                setElement(x,y, parent.etherealPlane.elementAt(x,y));
            }
        }
    }

    /**
     * Create a simple pond with some fire on one side
     * @param floorHeight - the height of the ground floor
     */
    public void pondWithGrill(World parent, int floorHeight){
        for(int x = 0;x < sizeX; ++x){ /* create the ground floor */
            for(int y = 0; y < sizeY; ++y){
                setForce(x,y, sizeX, dynamics,0,0);
                if(y <= floorHeight){
                    setElement(x,y, Material.Elements.Earth);
                    if(y <= (floorHeight/2)) parent.setUnit(x,y, Math.min(100,rnd.nextInt(500)));
                    else {
                        if(rnd.nextInt(8) > (y%8))
                            parent.setUnit(x,y,Math.max(10,rnd.nextInt(30)));
                            else parent.setUnit(x,y,Math.max(19,rnd.nextInt(50)));
                    }
                }else{
                    setElement(x,y, Material.Elements.Air);
                    parent.setUnit(x,y,Math.min(3,rnd.nextInt(10)));
                }
            }
        }

        int x; int y; /* Create the pond */
        for(float radius = 0; radius < (floorHeight/2.0f); radius += 0.5f){
            for(float sector = (float)Math.PI * 0.99f; sector < Math.PI * 2.01f; sector += Math.PI / 180){
                x = (int)(sizeX/2 + Math.cos(sector) * radius);
                x = Math.max(0, Math.min(sizeX, x));
                y = (int)(floorHeight + Math.sin(sector) * radius);
                y = Math.max(0, Math.min(sizeY, y));
                if(y <= (floorHeight - (floorHeight/4)) && (0 == rnd.nextInt(3)))
                    parent.setUnit(x,y,parent.getUnit(x,y) * 2.5f);
                setElement(x,y, Material.Elements.Water);
            }
        }

        /* Create a fire */
        x = sizeX/4;
        y = floorHeight + 1;
        setElement(x,y, Material.Elements.Fire);
        setElement(x-1,y, Material.Elements.Fire);
        setElement(x+1,y, Material.Elements.Fire);
        setElement(x,y+1, Material.Elements.Fire);
    }

    public void provideElementsTo(FloatBuffer[] inputs, int inputIndex){
        inputs[inputIndex] = elements;
    }

    public static boolean isMovable(int x, int y, int sizeX, FloatBuffer elements, FloatBuffer scalars){
        return Material.movable(getElementEnum(x,y,sizeX,elements),World.getUnit(x,y,sizeX,scalars));
    }

    public static Material.Elements getElementEnum(int x, int y, int sizeX, FloatBuffer buffer){
        return Material.Elements.get((int)getElement(x,y,sizeX,buffer));
    }

    public static float getElement(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer);
    }

    public Material.Elements getElement(int x, int y){
        return getElementEnum(x,y,sizeX, elements);
    }
    public static void setElement(int x, int y, int sizeX, FloatBuffer buffer, Material.Elements element){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer,(float)element.ordinal());
    }
    public void setElement(int x, int y, Material.Elements element){
        setElement(x,y,sizeX,elements,element);
    }

    private static final Vector2 tmpVec = new Vector2();
    public static Vector2 getForce(int x, int y, int sizeX, FloatBuffer buffer){
        tmpVec.set(
            BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer),
            BufferUtils.get(x,y,sizeX,Config.bufferCellSize,1, buffer)
        );
        return tmpVec;
    }
    public static float getForceX(int x, int y, int sizeX, FloatBuffer buffer){
        return getForce(x,y, sizeX, buffer).x;
    }
    public static float getForceY(int x, int y, int sizeX, FloatBuffer buffer){
        return getForce(x,y, sizeX, buffer).y;
    }
    public static void setForce(int x, int y, int sizeX, FloatBuffer buffer, Vector2 value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, value.x);
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, value.y);
    }
    public static void setForce(int x, int y, int sizeX, FloatBuffer buffer, float valueX, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, valueX);
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void setForceX(int x, int y, int sizeX, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, valueX);
    }
    public static void setForceY(int x, int y, int sizeX, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void addToForce(int x, int y, int sizeX, FloatBuffer buffer, float valueX, float valueY){
        addToForceX(x, y, sizeX, buffer, valueX);
        addToForceY(x, y, sizeX, buffer, valueY);
    }
    public static void addToForceX(int x, int y, int sizeX, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0,buffer, getForceX(x,y, sizeX, buffer) + valueX);
    }
    public static void addToForceY(int x, int y, int sizeX, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,1,buffer, getForceY(x,y, sizeX,  buffer) + valueY);
    }
    public static int getVelocityTick(int x, int y, int sizeX, FloatBuffer buffer){
        return (int)BufferUtils.get(x,y,sizeX,Config.bufferCellSize,2,buffer);
    }
    public static void setVelocityTick(int x, int y, int sizeX, FloatBuffer buffer, int value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,2,buffer, value);
    }
    public static void increaseVelocityTick(int x, int y, int sizeX, FloatBuffer buffer){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,2,buffer, getVelocityTick(x,y, sizeX, buffer)+1);
    }
    public static float getGravityCorrection(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,3,buffer);
    }
    public static void setGravityCorrection(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, sizeX, Config.bufferCellSize,3,buffer, value);
    }

    public Color getColor(int x, int y, World parent){
        return Material.getColor(getElement(x,y), parent.getUnit(x,y)).cpy();
    }

    private float netherDebugVal(World parent, int x, int y){
        return Math.max(0,
            parent.getEtherealPlane().netherValueAt(x,y)
            - parent.getEtherealPlane().getMaxNether(x,y)
        );
    }

    private float aetherDebugVal(World parent, int x, int y){
        return Math.max(0,
            parent.getEtherealPlane().aetherValueAt(x,y)
            - parent.getEtherealPlane().getMinAether(x,y)
        );
    }

    public void debugMeasure(World parent){
//        for(int x = 0;x < sizeX; ++x){ /* create the ground floor */
//            for(int y = 0; y < sizeY; ++y){
//                unitDebugVariable[x][y] = parent.getUnit(x,y);
//                if(getElement(x,y) == Material.Elements.Fire){
//                }
//            }
//        }
    }

    public void debugPrint(World parent){
//        System.out.println("middle units: " + parent.getUnit(sizeX/2, sizeY / 3));
    }

    float avgUnit = 0;
    float avgDivisor = 0;
    public Color getDebugColor(int x, int y, World parent){
        Color defColor = getColor(x,y, parent).cpy(); /*  TODO: Use spellUtil getColorOf */
//        if(0 < touchedByMechanics[x][y]){ /* it was modified.. */
//            defColor.lerp(Color.GREEN, 0.5f); /* to see if it was touched by the mechanics */
//            float aetherDebugVal = Math.abs(
//                parent.getEtherealPlane().getTargetAether(x,y)
//                - parent.getEtherealPlane().aetherValueAt(x,y)
//            ) / Math.max(0.001f, parent.getEtherealPlane().aetherValueAt(x,y));
        float unitsDiff = 0;
//        if( Material.ratioOf(Material.Elements.Fire) > parent.getUnit(x,y))
        if( x == sizeX/2 &&  y == sizeY / 3)
            unitsDiff = 0.8f;
            Color debugColor = new Color(
                netherDebugVal(parent,x,y)/parent.getEtherealPlane().netherValueAt(x,y),//Math.max(1.0f, Math.min(0.0f, forces[x][y].x)),
                //-Math.max(0.0f, Math.min(-5.0f, forces[x][y].y))/5.0f,
//                    (0 == touchedByMechanics[x][y])?0.0f:1.0f,
                    0f,//unitsDiff,
                aetherDebugVal(parent,x,y)/parent.getEtherealPlane().aetherValueAt(x,y),
                1.0f
            );
            defColor.lerp(debugColor,0.5f);
//        }
        return defColor;
    }

}
