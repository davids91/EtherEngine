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
import java.util.Arrays;
import java.util.Random;

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
    private final int switchElementsPhaseIndex;
    private final int switchDynamicsPhaseIndex;
    private final int initChangesPhaseIndex;
    private final int proposeForcesPhaseIndex;
    private final int proposeChangesFromForcesPhaseIndex;
    private final int arbitrateChangesPhaseIndex;
    private final int applyChangesDynamicsPhaseIndex;
    private final int mechanicsPostProcessDynamicsPhaseIndex;

    private final FloatBuffer[] processUnitsPhaseInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] processTypeUnitsPhaseInputs;
    private final FloatBuffer[] defineByEtherealPhaseInputs;
    private final FloatBuffer[] switchElementsPhaseInputs;
    private final FloatBuffer[] switchDynamicsPhaseInputs;
    private final FloatBuffer[] proposeForcesPhaseInputs;
    private final FloatBuffer[] proposeChangesFromForcesPhaseInputs;
    private final FloatBuffer[] arbitrateChangesPhaseInputs;
    private final FloatBuffer[] applyChangesDynamicsPhaseInputs;
    private final FloatBuffer[] mechanicsPostProcessDynamicsPhaseInputs;

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
        switchElementsPhaseIndex = backend.addPhase(this::switchElementsPhase, (Config.bufferCellSize * sizeX * sizeY));
        switchElementsPhaseInputs = new FloatBuffer[2];
        switchDynamicsPhaseIndex = backend.addPhase(this::switchDynamicsPhase, (Config.bufferCellSize * sizeX * sizeY));
        switchDynamicsPhaseInputs = new FloatBuffer[2];
        initChangesPhaseIndex = backend.addPhase(this::initChangesPhase, (Config.bufferCellSize * sizeX * sizeY));
        proposeForcesPhaseIndex = backend.addPhase(this::proposeForcesPhase, (Config.bufferCellSize * sizeX * sizeY));
        proposeForcesPhaseInputs = new FloatBuffer[4];
        proposeChangesFromForcesPhaseIndex = backend.addPhase(this::proposeChangesFromForcesPhase, (Config.bufferCellSize * sizeX * sizeY));
        proposeChangesFromForcesPhaseInputs = new FloatBuffer[4];
        arbitrateChangesPhaseIndex = backend.addPhase(this::arbitrateChangesPhase, (Config.bufferCellSize * sizeX * sizeY));
        arbitrateChangesPhaseInputs = new FloatBuffer[4];
        applyChangesDynamicsPhaseIndex = backend.addPhase(this::applyChangesDynamicsPhase, (Config.bufferCellSize * sizeX * sizeY));
        applyChangesDynamicsPhaseInputs = new FloatBuffer[4];
        mechanicsPostProcessDynamicsPhaseIndex = backend.addPhase(this::mechanicsPostProcessDynamicsPhase, (Config.bufferCellSize * sizeX * sizeY));
        mechanicsPostProcessDynamicsPhaseInputs = new FloatBuffer[3];

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

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements
     * @param output elements buffer
     */
    private void switchElementsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            if(0 != getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = getTargetX(x,y,sizeX, inputs[0]);
                int targetY = getTargetY(x,y,sizeX, inputs[0]);
                int toApply = (int)RealityAspect.getToApply(x,y, sizeX, inputs[0]);
                if(
                    (0 < x)&&(sizeX-1 > x)&&(0 < y)&&(sizeY-1 > y)
                    &&(0 < toApply)
                    &&(targetX >= 0)&&(targetX < sizeX)
                    &&(targetY >= 0)&&(targetY < sizeY)
                ){
                    setElement(x,y, sizeX, output, getElementEnum(targetX,targetY, sizeX, inputs[1]));
                    setPriority(x,y, sizeX, output, getPriority(targetX,targetY, sizeX, inputs[1]));
                }else{
                    setElement(x,y, sizeX, output, getElementEnum(x,y, sizeX, inputs[1]));
                    setPriority(x,y, sizeX, output, getPriority(x,y, sizeX, inputs[1]));
                }
            }
        }}
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: dynamics
     * @param output elements buffer
     */
    private void switchDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            if(0 != getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = getTargetX(x,y,sizeX, inputs[0]);
                int targetY = getTargetY(x,y,sizeX, inputs[0]);
                int toApply = (int)RealityAspect.getToApply(x,y, sizeX, inputs[0]);
                if(
                    (0 < x)&&(sizeX-1 > x)&&(0 < y)&&(sizeY-1 > y)
                    &&(0 < toApply)
                    &&(targetX >= 0)&&(targetX < sizeX)
                    &&(targetY >= 0)&&(targetY < sizeY)
                ){
                    setForce(x,y, sizeX, output, getForce(targetX,targetY, sizeX, inputs[1]));
                    setVelocityTick(x,y, sizeX, output, getVelocityTick(targetX,targetY, sizeX, inputs[1]));
                    setGravityCorrection(x,y, sizeX, output, getGravityCorrection(targetX,targetY, sizeX, inputs[1]));
                }else{
                    setForce(x,y, sizeX, output, getForce(x,y, sizeX, inputs[1]));
                    setVelocityTick(x,y, sizeX, output, getVelocityTick(x,y, sizeX, inputs[1]));
                    setGravityCorrection(x,y, sizeX, output, getGravityCorrection(x,y, sizeX, inputs[1]));
                }
            }
        }}
    }

    @Override
    public void switchValues(FloatBuffer proposals) {
        switchElementsPhaseInputs[0] = proposals;
        switchElementsPhaseInputs[1] = elements;
        backend.setInputs(switchElementsPhaseInputs);
        backend.runPhase(switchElementsPhaseIndex);
        BufferUtils.copy(backend.getOutput(switchElementsPhaseIndex), elements);

        switchDynamicsPhaseInputs[0] = proposals;
        switchDynamicsPhaseInputs[1] = dynamics;
        backend.setInputs(switchDynamicsPhaseInputs);
        backend.runPhase(switchDynamicsPhaseIndex);
        BufferUtils.copy(backend.getOutput(switchDynamicsPhaseIndex), dynamics);
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

    /**
     * A function to propose force updates based on material properties
     * @param inputs: none
     * @param output the initialized proposed changes
     */
    private void initChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < sizeX; ++x){ for(int y = 1; y < sizeY; ++y){
            setOffsetCode(x,y, sizeX, output, 0);
            setVelocityTick(x,y, sizeX, output, 0);
            setToApply(x,y, sizeX, output, 0);
        }}
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
                gravityCorrection = getWeight(x,y, sizeX, inputs[0], inputs[2]);
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
                        float aether_diff = Math.max(-10.5f, Math.min(10.5f,
                            ( EtherealAspect.aetherValueAt(x,y, sizeX, inputs[3]) - EtherealAspect.aetherValueAt(nx,ny, sizeX, inputs[3]))
                        ));
                        forceX += ((nx - x) * aether_diff);
                        forceY += ((ny - y) * aether_diff);
                    }
                }
            }

            if(Material.MechaProperties.Fluid == Material.getState(getElementEnum(x,y, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2]))){
                if(Material.isSameMat(
                    getElementEnum(x,y, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2]),
                    getElementEnum(x,y-1, sizeX, inputs[0]), World.getUnit(x,y, sizeX, inputs[2])
                )){
                    /* the cell is a liquid on top of another liquid, so it must move. */
                    if(0.0f < forceX) forceX *= 4;
                    else{
                        forceX = rnd.nextInt(6) - 3;
                        forceY = 1;
                    }
                }
            }else if(Material.MechaProperties.Plasma == Material.getState(getElement(x,y), World.getUnit(x,y, sizeX, inputs[2]))){
                forceX += rnd.nextInt(4) - 2;
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
            int toApply = (int)getToApply(x,y, sizeX, inputs[0]);

            if((0 == toApply)||(x == targetX && y == targetY)){ /* if no switch was arbitrated in the previously proposed changes */
                /* a target was not proposed previously for this cell, which would overwrite any switch proposed from forces */
                if(
                    !Material.discardable(getElementEnum(x,y, sizeX, inputs[1]), World.getUnit(x,y, sizeX, inputs[3]))
                    && (1 <= getForce(x,y, sizeX, inputs[2]).len())
                ){
                    /* propose to change to the direction of the force */
                    if(1 < Math.abs(getForceX(x,y, sizeX, inputs[2])))targetX = (int)(x + Math.max(-1, Math.min(getForceX(x,y, sizeX, inputs[2]),1)));
                    if(1 < Math.abs(getForceY(x,y, sizeX, inputs[2])))targetY = (int)(y + Math.max(-1, Math.min(getForceY(x,y, sizeX, inputs[2]),1)));
                    targetX = Math.max(0, Math.min(sizeX-1, targetX));
                    targetY = Math.max(0, Math.min(sizeY-1, targetY));

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
                            if (velocityMaxTicks > newVelocityTick){
                                ++newVelocityTick;
                                targetX = x;
                                targetY = y;
                                System.out.println("!");
                            }
                        }

                    }
                }
            }
            setVelocityTick(x,y, sizeX, output, newVelocityTick);
            /* TODO: Try to have everything go TOP RIGHT direction: Bottom left corner will duplicate cells somehow... */
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
        final int index_table_size = (index_radius * 2) + 1;
        int[][] priority = new int[index_table_size][index_table_size];
        int[][] changed = new int[index_table_size][index_table_size];
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            float offsetCode = 0;
            float toApply = 0;

            /* Initialize local data */
            int minIndexX = Math.max((x-index_radius), 0);
            int maxIndexX = Math.min((x+index_radius), sizeX);
            int minIndexY = Math.max((y-index_radius), 0);
            int maxIndexY = Math.min((y+index_radius), sizeY);
            for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                int sx = ix - x + (index_radius);
                int sy = iy - y + (index_radius);
                priority[sx][sy] = (int)( /* The priority of the given cell consist of..  */
                    getPriority(x,y, sizeX, inputs[1]) /* ..the provided value.. */
                    * getForce(x,y, sizeX, inputs[2]).len() /* ..the power of the force on it.. */
                    * getWeight(x,y, sizeX, inputs[1], inputs[3]) /* .. and its weight */
                );
                changed[sx][sy] = 0;
            }}

            int highestPrioX;
            int highestPrioY;
            int highestTargetX;
            int highestTargetY;
            while(true){ /* Until all requests with priority above are found  */
                highestPrioX = -2;
                highestPrioY = -2;
                highestTargetX = -2;
                highestTargetY = -2;
                int localSourceX; /* The highest priority change in the local vicinity.. */
                int localSourceY; /* ..corresponding to highestPrio*, which is of global index scope */
                int localTargetOfCX;
                int localTargetOfCY;
                int highestPrioLocalX = -2;
                int highestPrioLocalY = -2;
                int highestPrioTargetLocalX = -2;
                int highestPrioTargetLocalY = -2;
                for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                    int targetOfCX = getTargetX(ix,iy,sizeX,inputs[0]);
                    int targetOfCY = getTargetY(ix,iy,sizeX,inputs[0]);
                    localSourceX = ix - x + index_radius;
                    localSourceY = iy - y + index_radius;
                    localTargetOfCX = targetOfCX - x + index_radius;
                    localTargetOfCY = targetOfCY - y + index_radius;
                    if( /* The highest priority swap request is.. */
                        ( /* ..the one which isn't changed yet (only higher priority changes occurred prior to this loop )  */
                            (0 == changed[localSourceX][localSourceY])
                            &&( /* And either the target is out of bounds.. */
                                (localTargetOfCX < 0)||(localTargetOfCX >= index_table_size)
                                ||(localTargetOfCY < 0)||(localTargetOfCY >= index_table_size)
                                ||(0 == changed[localTargetOfCX][localTargetOfCY]) /* ..or not changed yet */
                            )
                        )&&( /* ..and of course the currently examined index has to has a higher target, then the previous highest one */
                            ((-2 == highestPrioLocalX)||(-2 == highestPrioLocalY))
                            ||(priority[highestPrioLocalX][highestPrioLocalY] < priority[localSourceX][localSourceY])
                        )
                    ){
                        highestPrioX = ix;
                        highestPrioY = iy;
                        highestPrioLocalX = localSourceX;
                        highestPrioLocalY = localSourceY;
                        highestTargetX = targetOfCX;
                        highestTargetY = targetOfCY;
                        highestPrioTargetLocalX = localTargetOfCX;
                        highestPrioTargetLocalY = localTargetOfCY;
                    }
                }}

                /* Simulate the highest priority change */
                if((-2 != highestPrioX)&&(-2 != highestPrioY)){
                    changed[highestPrioLocalX][highestPrioLocalY] = 1;
                    if(
                        (highestPrioTargetLocalX >= 0)&&(highestPrioTargetLocalX < index_table_size)
                        &&(highestPrioTargetLocalY >= 0)&&(highestPrioTargetLocalY < index_table_size)
                    ){
                        changed[highestPrioTargetLocalX][highestPrioTargetLocalY] = 1;
                    }
                }

                /* If c was reached; or no changes are proposed; break! */
                if(
                    ((x == highestPrioX)&&(y == highestPrioY))
                    ||((x == highestTargetX)&&(y == highestTargetY))
                    ||((-2 == highestPrioX)&&(-2 == highestPrioY))
                    ||((-2 == highestTargetX)&&(-2 == highestTargetY))
                ){
                    if(
                        ((x == highestPrioX)&&(y == highestPrioY))
                        ||((x == highestTargetX)&&(y == highestTargetY))
                    ){
                        toApply = 1; /* Also set the toApply bit so the change would be actuated */
                        if((x == highestPrioX)&&(y == highestPrioY)){
                            offsetCode = getOffsetCode((highestTargetX - x), (highestTargetY - y));
                        }else{ /* if((x == highestTargetX)&&(y == highestTargetY)){ /* This is always true here.. */
                            offsetCode = getOffsetCode((highestPrioX - x), (highestPrioY - y));
                        }
                    }
                    break;
                }
            }

            /*!Note: At this point highestPrio* and highestTarget*
             * should contain either -2 or the highest priority switch request involving c
             * */
            setOffsetCode(x,y, sizeX, output, offsetCode);
            setToApply(x,y, sizeX,output, toApply);
        }}
    }

    /**
     * Applies the changes to forces proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output dynamics buffer updated with proper forces
     */
    private void applyChangesDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){ /* TODO: Define edges as connection point to other chuks*/
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            if(0 != getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = getTargetX(x,y,sizeX, inputs[0]);
                int targetY = getTargetY(x,y,sizeX, inputs[0]);
                float forceX = getForceX(x,y, sizeX, inputs[2]);
                float forceY = getForceY(x,y, sizeX, inputs[2]);
                float weight = getWeight(x,y, sizeX, inputs[1], inputs[3]);
                float gravityCorrection = 1;

                if( /* Update the forces on a cell.. */ /* TODO: Handle chunk border interactions */
                    (0 < getOffsetCode(x,y, sizeX, inputs[0])) /* ..when a cells wants to switch..  */
                    &&(targetX >= 0)&&(targetX < sizeX)&&(targetY >= 0)&&(targetY < sizeY) /* ..but only if the target is also inside the bounds of the chunk */
                ){
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
     * Post processing with the dynamics:basically corrects with the gravity based on GravityCorrection
     * @param inputs [0]: elements; [1]: dynamics; [2]: scalars
     * @param output the post-processed dynamics buffer
     */
    private void mechanicsPostProcessDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
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
        backend.runPhase(initChangesPhaseIndex);
        BufferUtils.copy(backend.getOutput(initChangesPhaseIndex), proposedChanges);

        for(int x = 1; x < sizeX-1; ++x){
            for(int y = sizeY-2; y > 0; --y){
                touchedByMechanics[x][y] = 0;
            }
        }

        /* Main Mechanic phase */
        for(int i = 0; i < velocityMaxTicks; ++i){
            /* Propose forces, and apply changes not yet applied from the previous iteration */
            /* Propose changes from forces */
            proposeForcesPhaseInputs[0] = elements;
            proposeForcesPhaseInputs[1] = dynamics;
            parent.provideScalarsTo(proposeForcesPhaseInputs, 2);
            parent.getEtherealPlane().provideEtherTo(proposeForcesPhaseInputs, 3);
            backend.setInputs(proposeForcesPhaseInputs);
            backend.runPhase(proposeForcesPhaseIndex); /* output: dynamics */

            proposeChangesFromForcesPhaseInputs[0] = proposedChanges;
            proposeChangesFromForcesPhaseInputs[1] = elements;
            proposeChangesFromForcesPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
            parent.provideScalarsTo(proposeChangesFromForcesPhaseInputs, 3);
            backend.setInputs(proposeChangesFromForcesPhaseInputs);
            backend.runPhase(proposeChangesFromForcesPhaseIndex); /* output: newly proposed changes */

            arbitrateChangesPhaseInputs[0] = backend.getOutput(proposeChangesFromForcesPhaseIndex);
            arbitrateChangesPhaseInputs[1] = elements;
            arbitrateChangesPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
            parent.provideScalarsTo(arbitrateChangesPhaseInputs, 3);
            backend.setInputs(arbitrateChangesPhaseInputs);
            backend.runPhase(arbitrateChangesPhaseIndex); /* output: newly proposed changes */

            applyChangesDynamicsPhaseInputs[0] = backend.getOutput(arbitrateChangesPhaseIndex);
            applyChangesDynamicsPhaseInputs[1] = elements;
            applyChangesDynamicsPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
            parent.provideScalarsTo(applyChangesDynamicsPhaseInputs, 3);
            backend.setInputs(applyChangesDynamicsPhaseInputs);
            backend.runPhase(applyChangesDynamicsPhaseIndex); /* output: dynamics before the swaps in light of the proposed ones */
            BufferUtils.copy(backend.getOutput(applyChangesDynamicsPhaseIndex), dynamics); /* TODO: maybe copies can be avoided here? */

            parent.switchValues(backend.getOutput(arbitrateChangesPhaseIndex));
        }

        mechanicsPostProcessDynamicsPhaseInputs[0] = elements;
        mechanicsPostProcessDynamicsPhaseInputs[1] = dynamics;
        parent.provideScalarsTo(mechanicsPostProcessDynamicsPhaseInputs, 2);
        backend.setInputs(mechanicsPostProcessDynamicsPhaseInputs);
        backend.runPhase(mechanicsPostProcessDynamicsPhaseIndex);
        BufferUtils.copy(backend.getOutput(applyChangesDynamicsPhaseIndex), dynamics);
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
                if(
                    (y <= floorHeight)
                    &&(0 < x)&&(sizeX-1 > x)&&(0 < y)&&(sizeY-1 > y)
                ){
                    setElement(x,y, Material.Elements.Earth);
                    if(y <= (floorHeight/2)) parent.setUnit(x,y, Math.min(100,rnd.nextInt(500)));
                    else {
                        if(rnd.nextInt(8) > (y%8)) parent.setUnit(x,y,Math.max(10,rnd.nextInt(30)));
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
            /* Red : x;Blue: y --> for the resulting offset */
            float offsetR = (getTargetX(x,y, sizeX, proposedChanges) - x + 1) / 2.0f;
            float offsetB = (getTargetY(x,y, sizeX, proposedChanges) - y + 1) / 2.0f;
            Color debugColor = new Color(
                    offsetR,
//                netherDebugVal(parent,x,y)/parent.getEtherealPlane().netherValueAt(x,y),//Math.max(1.0f, Math.min(0.0f, forces[x][y].x)),
                //-Math.max(0.0f, Math.min(-5.0f, forces[x][y].y))/5.0f,
//                    (0 == touchedByMechanics[x][y])?0.0f:1.0f,
                    0f,//unitsDiff,
//                aetherDebugVal(parent,x,y)/parent.getEtherealPlane().aetherValueAt(x,y),
                    offsetB,
                1.0f
            );
            defColor.lerp(debugColor,0.5f);
//        }
        return defColor;
    }

}
