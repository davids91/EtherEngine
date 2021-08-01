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

    /* TODO: Make one slot free ( for force z ) */
    /**
     * A texture image representing the dynamism of a cell
     * - R: x of the force vector active on the block
     * - G: y of the force vector active on the block
     * - B: the velocity tick of the cell ( 0 means the cell would move )
     * - A: gravity correction amount ( helps to not add gravity in the intermediary steps of the mechanics evaluation )
     */
    private FloatBuffer dynamics;

    /**
     * A texture image representing each cells intention to switch to another cell
     * - R: the offset code for the target, which is to be used in accordance with the coordinates of the source cell
     * - G:
     * - B: acquired velocity tick (to be used to correct it in dynamics buffer when taking over changes)
     * - A: toApply bit --> whether or not to apply this change, or to try again in the next iteration
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

    /* TODO: Weight to include pressure somehow? or at least the same materials on top */
    public static float getWeight(int x, int y, int sizeX, FloatBuffer elements, FloatBuffer scalars){
        return (
            World.getUnit(x,y, sizeX, scalars)
            * Material.TYPE_SPECIFIC_GRAVITY
                [(int)getElement(x,y, sizeX, elements)]
                [MiscUtils.indexIn(
                    Material.TYPE_UNIT_SELECTOR[(int)getElement(x,y, sizeX, elements)],
                    World.getUnit(x,y, sizeX, scalars)
                )]
        );
    }

    public static float getOffsetCode(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer);
    }

    public static void setOffsetCode(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX,Config.bufferCellSize,0, buffer, value);
    }

    public static float getToApply(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX,Config.bufferCellSize,0, buffer);
    }

    public static void setToApply(int x, int y, int sizeX, FloatBuffer buffer, float value){
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

    /**
     * Returns a code for the hardcoded directions based on the arguments
     * @param ox offset x -
     * @param oy offset y - the direction to which the direction should point
     * @return a code unique for any direction, generated by the given offsets
     */
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
     * A function to propose force updates based on material properties
     * @param inputs: [0]: elements; [1]: dynamics; [2]: scalars; [3]: ethereal
     * @param output the resulting updated dynamics
     */
    private void proposeForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < sizeX; ++x){ for(int y = 1; y < sizeY; ++y){
            float forceX = 0.0f;
            float forceY = 0.0f;
            float gravityCorrection = 0.0f;
            if(
                !Material.discardable(
                    getElementEnum(x,y, sizeX, inputs[0]),
                    World.getUnit(x,y, sizeX, inputs[2])
                )
            ){
                gravityCorrection = getWeight(x,y,sizeX, inputs[0], inputs[2]);
                forceX = getForceX(x,y, sizeX, inputs[1]);
                forceY = getForceY(x,y, sizeX, inputs[1]);
            }
            if(Material.Elements.Ether == getElementEnum(x,y, sizeX, inputs[0])){
                for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                    if ( /* in the bounds of the chunk.. */
                        (0 <= nx)&&(sizeX > nx)&&(0 <= ny)&&(sizeY > ny)
                        &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* ..after the not immediate neighbourhood.. */
                        &&(World.getUnit(x,y, sizeX, inputs[2]) <= World.getUnit(x,y, sizeX, inputs[2]))
                        &&(Material.Elements.Ether == getElementEnum(nx,ny, sizeX, inputs[0]))
                    ){ /* ..Calculate forces from surplus ethers */
                        float aether_diff = Math.max(-10.5f, Math.min(10.5f, (
                            EtherealAspect.aetherValueAt(x,y, sizeX, inputs[3])
                            - EtherealAspect.aetherValueAt(nx,ny, sizeX, inputs[3])
                        )));
                        addToForceX(x, y, sizeX, inputs[1], ((nx - x) * aether_diff));
                        addToForceY(x, y, sizeX, inputs[1], ((ny - y) * aether_diff));
                    }
                }
            }

            if(
                Material.MechaProperties.Fluid
                == Material.getState(
                    getElementEnum(x,y, sizeX, inputs[0]),
                    World.getUnit(x,y, sizeX, inputs[2])
                )
            ){
                if(Material.isSameMat(
                    getElementEnum(x,y, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2]),
                    getElementEnum(x,y-1, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2])
                )){
                    /* the cell is a liquid on top of another liquid, so it must move. */
                    if(0.0f < getForceX(x,y, sizeX, inputs[1])) setForceX(x,y, sizeX, inputs[1],getForceX(x,y, sizeX, dynamics) * 4);
                    else setForce(x,y, sizeX, inputs[1], (rnd.nextInt(6) - 3), 1);
                }
            }else if(Material.MechaProperties.Plasma == Material.getState(getElement(x,y), World.getUnit(x,y, sizeX, inputs[2]))){
                addToForce(x,y, sizeX, inputs[1],(rnd.nextInt(4)-2),0);
            }/* TODO: Make gases loom, instead of staying still ( move about a bit maybe? )  */

            setForce(x,y, sizeX, output,forceX,forceY);
            setGravityCorrection(x,y, sizeX, output,gravityCorrection);
            setVelocityTick(x,y, sizeX, output, velocityMaxTicks);
        } }
    }

    /**
     * Provides proposed cell switches based on Forces
     * @param inputs [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output proposed switches, toApply set all 0
     */
    private void proposeChangesFromForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            int newVelocityTick = getVelocityTick(x,y, sizeX, inputs[2]);
            int targetX = getTargetX(x,y, sizeX, inputs[0]);
            int targetY = getTargetY(x,y, sizeX, inputs[0]);

            if(x != targetX || y != targetY){ /* if no switch was arbitrated in the previously proposed changes */
                /* a target was not proposed previously for this cell, which would overwrite any switch proposed from forces */
                if(
                    !Material.discardable(getElement(x,y), World.getUnit(x,y, sizeX, inputs[3]))
                    && (1 <= getForce(x,y, sizeX, inputs[2]).len())
                ){
                    /* propose to change to the direction of the force */
                    if(1 < Math.abs(getForceX(x,y, sizeX, inputs[2])))targetX = (int)(x + Math.max(-1, Math.min(getForceX(x,y, sizeX, inputs[2]),1)));
                    if(1 < Math.abs(getForceY(x,y, sizeX, inputs[2])))targetY = (int)(y + Math.max(-1, Math.min(getForceY(x,y, sizeX, inputs[2]),1)));
                    targetX = Math.max(0, Math.min(sizeX, targetX));
                    targetY = Math.max(0, Math.min(sizeY, targetY));

                    /* calculate the final position of the intended target cell */
                    int targetFinalPosX = targetX;
                    int targetFinalPosY = targetY;
                    if(1 < Math.abs(getForceX(targetX,targetY, sizeX, inputs[2])))
                        targetFinalPosX = (int)(targetX + Math.max(-1.1f, Math.min(getForceX(targetX, targetY, sizeX, inputs[2]),1.1f)));
                    if(1 < Math.abs(getForceY(targetX,targetY, sizeX, inputs[2])))
                        targetFinalPosY = (int)(targetY + Math.max(-1.1f, Math.min(getForceY(targetX,targetY, sizeX, inputs[2]),1.1f)));

                    /* see if the two cells still intersect with forces included */
                    if(2 > MiscUtils.distance(x,y,targetFinalPosX,targetFinalPosY)){
                        if(
                            !((x == targetX) && (y == targetY))
                            &&( /* In case both is discardable, then no operations shall commence */
                                !Material.discardable(getElement(x,y),World.getUnit(x,y, sizeX, inputs[3]))
                                ||!Material.discardable(getElement(targetX,targetY),World.getUnit(targetX,targetY, sizeX, inputs[3]))
                            )
                        ){ /* propose a switch with the target */
                            if (velocityMaxTicks > getVelocityTick(x, y, sizeX, inputs[2])){
                                ++newVelocityTick;
                                targetX = x;
                                targetY = y;
                            }
                        }

                    }
                }
            }
            setVelocityTick(x,y, sizeX, output,newVelocityTick);
            setOffsetCode(x,y,sizeX,output,getOffsetCode((targetX - x), (targetY - y)));
            setToApply(x,y, sizeX,output, 0);
        }}

    }

    /**
     * Decides which changes are to be applied, and which would not
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output the arbitrated changes, with the toApply part set
     */
    private void arbitrateChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        final int index_radius = 2;
        int[][] priority = new int[index_radius][index_radius];
        int[][] changed = new int[index_radius][index_radius];
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            float offsetCode = ElementalAspect.getOffsetCode(x,y, sizeX, inputs[0]);
            float toApply = getToApply(x,y, sizeX, inputs[0]);

            int minIndexX = Math.max((x-index_radius), 0);
            int maxIndexX = Math.min((x+index_radius), sizeX);
            int minIndexY = Math.max((y-index_radius), 0);
            int maxIndexY = Math.min((y+index_radius), sizeY);
            /* Initialize local data */
            for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                int sx = ix - x + (index_radius);
                int sy = iy - y + (index_radius);
                priority[sx][sy] = (int)( /* The priority of the given cell consist of..  */
                    getPriority(x,y,sizeX,inputs[1]) /* ..the provided value.. */
                    * getForce(x,y,sizeX,inputs[2]).len() /* ..the power of the force on it.. */
                    * World.getUnit(x,y,sizeX,inputs[3]) /* .. and its weight */
                );
                changed[sx][sy] = 0;
            }}

            int highestPrioX;
            int highestPrioY;
            int highestTargetX;
            int highestTargetY;

            while(true){ /* Go through all the high priority switch requests in the previously proposed changes */
                highestPrioX = -1;
                highestPrioY = -1;
                highestTargetX = -1;
                highestTargetY = -1;
                for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                    int sx = ix - x + (index_radius);
                    int sy = iy - y + (index_radius);
                    int tx = getTargetX(x,y,sizeX,inputs[0]);
                    int ty = getTargetY(x,y,sizeX,inputs[0]);
                    if(
                        ( /* where both the target and the source of the change are free.. */
                            (0 == changed[sx][sy])&&(0 == changed[tx][ty])
                        )&&( /* ..decide the highest priority swap request */
                            ((-1 == highestPrioX)||(-1 == highestPrioY))
                            ||(priority[highestPrioX][highestPrioY] < priority[sx][sy])
                        )
                    ){
                        highestPrioX = sx;
                        highestPrioY = sy;
                        highestTargetX = tx;
                        highestTargetY = ty;
                    }
                }}
                /* Simulate the highest priority change */
                if(
                    ((-1 != highestPrioX)&&(-1 != highestPrioY))
                    &&((-1 != highestTargetX)&&(-1 != highestTargetY))
                ){
                    changed[highestPrioX][highestPrioY] = 1;
                    changed[highestTargetX][highestTargetY] = 1;
                }
                /* If c was reached; or no changes are proposed; break! */
                if(
                    ((x == highestPrioX)&&(y == highestPrioY))
                    ||((x == highestTargetX)&&(y == highestTargetY))
                    ||((-1 == highestPrioX)&&(-1 == highestPrioY))
                    ||((-1 == highestTargetX)&&(-1 == highestTargetY))
                ){
                    if(
                        ((x == highestPrioX)&&(y == highestPrioY))
                        ||((x == highestTargetX)&&(y == highestTargetY))
                    ){
                        toApply = 1; /* Also set the toApply bit so the change would be actuated */
                        if((x == highestPrioX)&&(y == highestPrioY)){
                            offsetCode = getOffsetCode(highestTargetX, highestTargetY);
                        }else{
                            offsetCode = getOffsetCode(highestPrioX, highestPrioY);
                        }
                    }
                    break;
                }
            }

            /*!Note: At this point highestPrio* and highestTarget*
             * should contain either -1 or the highest priority switch request involving c
             * */
            setOffsetCode(x,y, sizeX,output, offsetCode);
            setToApply(x,y, sizeX,output, toApply);
        } }
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output dynamics buffer
     */
    private void applyChangesDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            if(0 != getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = getTargetX(x,y,sizeX, inputs[0]);
                int targetY = getTargetY(x,y,sizeX, inputs[0]);
                float forceX = getForceX(x,y, sizeX, inputs[2]);
                float forceY = getForceY(x,y, sizeX, inputs[2]);
                float weight = getWeight(x,y, sizeX, inputs[1], inputs[3]);
                float gravityCorrection = 1;

                if(0 < getOffsetCode(x,y, sizeX, inputs[0])){
                    gravityCorrection = 0; /* Gravity is being added at forces update, so no need to re-add it at the end of the loop */
                    if( /* The cells swap, decreasing forces on both */
                        Material.discardable(getElementEnum(targetX, targetY, sizeX, inputs[1]), World.getUnit(targetX, targetY, sizeX, inputs[3]))
                        ||(
                            (getWeight(x,y, sizeX, inputs[1], inputs[3]) > getWeight(targetX, targetY, sizeX, inputs[1], inputs[3]))
                            && Material.movable(getElementEnum(targetX, targetY, sizeX, inputs[1]), World.getUnit(targetX, targetY, sizeX, inputs[3]))
                        )
                    ){ /* TODO: Also decrease the force based on the targets weight */
                        forceX += - forceX * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceX))) );
                        forceY += - forceY * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceY))) );
                        forceX += (myMiscUtils.getGravity(x,y).x * weight);
                        forceY += (myMiscUtils.getGravity(x,y).y * weight);
                    }else{ /* The cells collide, updating forces, but no swapping */
                        Vector2 u1 = getForce(x,y, sizeX, dynamics).cpy().nor();
                        float m2 = getWeight(targetX, targetY, sizeX, inputs[1], inputs[3]);
                        Vector2 u2 = getForce(targetX, targetY, sizeX, dynamics).cpy().nor();
                        Vector2 result_speed = new Vector2();
                        result_speed.set( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                            ((weight - m2)/(weight+m2)*u1.x) + (2*m2/(weight+m2))*u2.x,
                            ((weight - m2)/(weight+m2)*u1.y) + (2*m2/(weight+m2))*u2.y
                        );

                        /* F = m*a --> `a` is the delta v, which is the change in the velocity */
                        forceX = (weight * (result_speed.x - u1.x));
                        forceY = (weight * (result_speed.y - u1.y));
                        forceX += (myMiscUtils.getGravity(x,y).x * weight);
                        forceY += (myMiscUtils.getGravity(x,y).y * weight);
                    }
                }

                setForceX(x,y, sizeX, output, forceX);
                setForceY(x,y, sizeX, output, forceY);
                setVelocityTick(x,y, sizeX, output, getVelocityTick(x,y, sizeX, inputs[0]));
                setGravityCorrection(x,y, sizeX, output, gravityCorrection);
            }
        }}
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements
     * @param output elements buffer
     */
    private void applyChangesElementsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            if(0 != getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = getTargetX(x,y,sizeX, inputs[0]);
                int targetY = getTargetY(x,y,sizeX, inputs[0]);
                setElement(x,y, sizeX, output, getElementEnum(targetX,targetY,sizeX, inputs[1]));
                setPriority(x,y, sizeX, output, getPriority(targetX,targetY,sizeX, inputs[1]));
            }
        }}
    }

    /**
     * Post processing with the dynamics:basically corrects with the gravity based on GravityCorrection
     * @param inputs [0]: elements; [1]: dynamics; [2]: scalars
     * @param output the post-processed dynamics buffer
     */
    private void mechanicsPostProcessDynamicsPhase(FloatBuffer inputs[], FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            float gravityCorrection = getGravityCorrection(x,y, sizeX, inputs[0]);
            float forceX = getForceX(x,y, sizeX, inputs[1]);
            float forceY = getForceY(x,y, sizeX, inputs[1]);
            if(
                (0 < gravityCorrection)
                && Material.movable(getElementEnum(x,y, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2]))
            ){
                forceX += gravityCorrection * myMiscUtils.getGravity(x,y).x;
                forceY += gravityCorrection * myMiscUtils.getGravity(x,y).y;
            }
            setForceX(x,y, sizeX, output, forceX);
            setForceY(x,y, sizeX, output, forceY);
            setVelocityTick(x,y, sizeX, output, getVelocityTick(x,y, sizeX, inputs[1]));
            setGravityCorrection(x,y, sizeX, output, 0);
        }}
    }

    @Override
    public void processMechanics(World parent) {
        /* Init Mechanics phase */
        /* TODO: initialize previously left out proposals as well to 0 */
        for(int x = 1; x < sizeX-1; ++x){ /* Pre-process: Add gravity, and nullify forces on discardable objects; */
            for(int y = sizeY-2; y > 0; --y){
                touchedByMechanics[x][y] = 0;
            }
        }

        /* Main Mechanic phase */
        for(int i = 0; i < velocityMaxTicks; ++i){
            /* Apply changes not applied, but arbitrated from the last loop */
            //
            /* Propose forces */
            /* Propose changes from forces */
            /*!Note: Simply put: forces of the cell generate the target whom it wants to switch with */
            //proposeChangesFromForces
            /* Arbitrate changes */
            //
            /* Apply arbitrated changes */
            //dynamics
            //elements

            /* Also apply swaps on scalars and Ether! */
        }

        /* Mechanic post-process phase */
        //postProcessMechanics
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
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
