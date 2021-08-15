package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.ElementalAspectStrategy;
import com.crystalline.aether.models.world.EtherealAspectStrategy;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.models.world.RealityAspectStrategy;
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
    private final int velocityMaxTicks = 3;
    private final Random rnd = new Random();

    /* TODO: Maybe prio doesn't need to be kept... It can be just calculated from coordinates */
    /**
     * A texture image representing the elemental properties of reality
     * - R: block type --> Material.Elements
     * - G:
     * - B:
     * - A: priority --> A unique number to decide arbitration while switching cells
     */
    private FloatBuffer elements;

    /* TODO: GravityCorrection to be moved into proposed changes */
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
     *          0 - don't apply; 1 - a cell will switch with his cell; 2 - this cell will switch with another cell
     */
    private final FloatBuffer proposedChanges;

    private float[][] touchedByMechanics; /* Debug purposes */

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
    private final int arbitrateInteractionsPhaseIndex;

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
    private final FloatBuffer[] arbitrateInteractionsPhaseInputs;

    public ElementalAspect(Config conf_){
        super(conf_);
        myMiscUtils = new MiscUtils();
        elements = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        dynamics = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        proposedChanges = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        touchedByMechanics = new float[conf.getChunkBlockSize()][conf.getChunkBlockSize()];
        backend = new CPUBackend();
        processUnitsPhaseInputs = new FloatBuffer[]{elements, null};
        processUnitsPhaseIndex = backend.addPhase(this::processUnitsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        processTypesPhaseInputs = new FloatBuffer[]{elements,null,null};
        processTypesPhaseIndex = backend.addPhase(this::processTypesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        processTypeUnitsPhaseInputs = new FloatBuffer[]{elements,null};
        processTypeUnitsPhaseIndex = backend.addPhase(this::processTypeUnitsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        defineByEtherealPhaseInputs = new FloatBuffer[2];
        defineByEtherealPhaseIndex = backend.addPhase(this::defineByEtherealPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        switchElementsPhaseIndex = backend.addPhase(this::switchElementsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        switchElementsPhaseInputs = new FloatBuffer[2];
        switchDynamicsPhaseIndex = backend.addPhase(this::switchDynamicsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        switchDynamicsPhaseInputs = new FloatBuffer[2];
        initChangesPhaseIndex = backend.addPhase(this::initChangesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        proposeForcesPhaseIndex = backend.addPhase(this::proposeForcesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        proposeForcesPhaseInputs = new FloatBuffer[4];
        proposeChangesFromForcesPhaseIndex = backend.addPhase(this::proposeChangesFromForcesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        proposeChangesFromForcesPhaseInputs = new FloatBuffer[4];
        arbitrateChangesPhaseIndex = backend.addPhase(this::arbitrateChangesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        arbitrateChangesPhaseInputs = new FloatBuffer[4];
        applyChangesDynamicsPhaseIndex = backend.addPhase(this::applyChangesDynamicsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        applyChangesDynamicsPhaseInputs = new FloatBuffer[4];
        mechanicsPostProcessDynamicsPhaseIndex = backend.addPhase(this::mechanicsPostProcessDynamicsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        mechanicsPostProcessDynamicsPhaseInputs = new FloatBuffer[3];
        arbitrateInteractionsPhaseIndex = backend.addPhase(this::arbitrateInteractionsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        arbitrateInteractionsPhaseInputs = new FloatBuffer[3];

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

    private void addWater(int ox, int oy, World parent){
        setElement(ox, oy, Material.Elements.Water);
        parent.setUnit(ox, oy,16);
    }

    private void addRock(int ox, int oy, World parent){
        setElement(ox, oy, Material.Elements.Earth);
        parent.setUnit(ox, oy,550);
    }

    public void addOneGrainOfSandForTestingPurposes(World parent){
        addRock((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2)+2, (conf.getChunkBlockSize()/2), parent);
        addRock((conf.getChunkBlockSize()/2)-2, (conf.getChunkBlockSize()/2), parent);
        addRock((conf.getChunkBlockSize()/2)+2, (conf.getChunkBlockSize()/2)+1, parent);
        addRock((conf.getChunkBlockSize()/2)-2, (conf.getChunkBlockSize()/2)+1, parent);

        addWater((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2)+1, parent);
        addWater((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2)+1, parent);
        addWater((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2)+1, parent);

    }

    private float maxPrio = 0;
    private void calculatePrio(){
        Random rnd = new Random();
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            RealityAspectStrategy.setPriority(x,y,conf.getChunkBlockSize(),elements,rnd.nextFloat());
        }}
        while(true){
            int similarities = 0;
            for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                int index_radius = 2;
                int minIndexX = Math.max((x-index_radius), 0);
                int maxIndexX = Math.min((x+index_radius), conf.getChunkBlockSize());
                int minIndexY = Math.max((y-index_radius), 0);
                int maxIndexY = Math.min((y+index_radius), conf.getChunkBlockSize());
                for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                    if(
                        ((x != ix)&&(y != iy))
                        &&(500 > Math.abs(RealityAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements) - RealityAspectStrategy.getPriority(ix,iy, conf.getChunkBlockSize(), elements)))
                    ){
                        RealityAspectStrategy.setPriority(x,y,conf.getChunkBlockSize(),elements,rnd.nextFloat() * Float.MAX_VALUE);
                        ++similarities;
                    }
                }}
            }}
            if(0 == similarities)break;
        }
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            if(maxPrio <  RealityAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements)){
                maxPrio = RealityAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements);
            }
        }}
    }
    public void reset(){
        calculatePrio();
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            setElement(x,y,Material.Elements.Air);
            ElementalAspectStrategy.setForce(x,y, conf.getChunkBlockSize(), dynamics,0,0);
            ElementalAspectStrategy.setGravityCorrection(x,y, conf.getChunkBlockSize(), dynamics,0);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), proposedChanges, velocityMaxTicks);
            touchedByMechanics[x][y] = 0;
        }}
    }



    /**
     * Defines the elemental phase based on the ethereal
     * @param inputs [0]: elements; [1]: ethereal
     * @param output the re-written elemental plane
     */
    private void defineByEtherealPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            ElementalAspectStrategy.setElement(x,y, conf.getChunkBlockSize(), output, EtherealAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[1]));
            RealityAspectStrategy.setPriority(x,y, conf.getChunkBlockSize(), output, RealityAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), inputs[0]));
        } }
    }

    public void defineBy(EtherealAspect plane){
        defineByEtherealPhaseInputs[0] = elements;
        plane.provideEtherTo(defineByEtherealPhaseInputs, 1);
        backend.setInputs(defineByEtherealPhaseInputs);
        backend.runPhase(defineByEtherealPhaseIndex);
        BufferUtils.copy(backend.getOutput(defineByEtherealPhaseIndex), elements);
    }

    private float avgOfUnit(int x, int y, FloatBuffer elements, FloatBuffer scalars, Material.Elements type){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(conf.getChunkBlockSize(), x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(conf.getChunkBlockSize(), y + 2); ++ny) {
                if(ElementalAspectStrategy.getElementEnum(nx,ny, conf.getChunkBlockSize(), elements) == type){
                    average_val += World.getUnit(nx,ny,conf.getChunkBlockSize(),scalars);
                    division += 1;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private int numOfElements(int x, int y, FloatBuffer elements, Material.Elements type){
        int num = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(conf.getChunkBlockSize(), x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(conf.getChunkBlockSize(), y + 2); ++ny) {
                if(ElementalAspectStrategy.getElementEnum(nx,ny, conf.getChunkBlockSize(), elements) == type){
                    ++num;
                }
            }
        }
        return num;
    }

    private float avgOfUnitsWithinDistance(int x, int y, FloatBuffer elements, FloatBuffer scalars){
        float average_val = World.getUnit(x,y, conf.getChunkBlockSize(), scalars);
        float division = 1;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(conf.getChunkBlockSize(), x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(conf.getChunkBlockSize(), y + 2); ++ny) {
                if(
                    (x != nx) && (y != ny)
                    &&Material.isSameMat(
                        ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), elements), World.getUnit(x,y, conf.getChunkBlockSize(), scalars),
                        ElementalAspectStrategy.getElementEnum(nx,ny, conf.getChunkBlockSize(), elements), World.getUnit(nx,ny, conf.getChunkBlockSize(), scalars)
                    )
                ){
                    average_val += World.getUnit(nx,ny, conf.getChunkBlockSize(), scalars);
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
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            Material.Elements element = ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[1]);
            if(0 != RealityAspectStrategy.getOffsetCode(x,y,conf.getChunkBlockSize(), inputs[0])){
                int targetX = RealityAspectStrategy.getTargetX(x,y,conf.getChunkBlockSize(), inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,conf.getChunkBlockSize(), inputs[0]);
                int toApply = (int) RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]);
                if(
                    (0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
                    &&(0 < toApply)
                    &&(targetX >= 0)&&(targetX < conf.getChunkBlockSize())
                    &&(targetY >= 0)&&(targetY < conf.getChunkBlockSize())
                ){
                    element = ElementalAspectStrategy.getElementEnum(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);
                }
            }
            ElementalAspectStrategy.setElement(x,y, conf.getChunkBlockSize(), output, element);
            RealityAspectStrategy.setPriority(x,y, conf.getChunkBlockSize(), output, ElementalAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), inputs[1]));
            /*!Note: Priorities serve as an arbitration measure based on coordinates, so they should not be switched
             */
        }}
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: dynamics
     * @param output dynamics buffer
     */
    private void switchDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            int targetX = RealityAspectStrategy.getTargetX(x,y,conf.getChunkBlockSize(), inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y,conf.getChunkBlockSize(), inputs[0]);
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[1]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[1]);
            int velocityTick = ElementalAspectStrategy.getVelocityTick(x,y, conf.getChunkBlockSize(), inputs[1]);
            float gravityCorrection = ElementalAspectStrategy.getGravityCorrection(x,y, conf.getChunkBlockSize(), inputs[1]);
            if(
                (0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
                &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,conf.getChunkBlockSize(), inputs[0]))
                &&(targetX >= 0)&&(targetX < conf.getChunkBlockSize())
                &&(targetY >= 0)&&(targetY < conf.getChunkBlockSize())
            ){
                forceX = ElementalAspectStrategy.getForceX(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);
                forceY = ElementalAspectStrategy.getForceY(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);
                gravityCorrection = ElementalAspectStrategy.getGravityCorrection(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);;
                velocityTick = ElementalAspectStrategy.getVelocityTick(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);
            }
            ElementalAspectStrategy.setForceX(x,y, conf.getChunkBlockSize(), output, forceX);
            ElementalAspectStrategy.setForceY(x,y, conf.getChunkBlockSize(), output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, velocityTick);
            ElementalAspectStrategy.setGravityCorrection(x,y, conf.getChunkBlockSize(), output, gravityCorrection);
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
        for(int x = 0;x < conf.getChunkBlockSize(); ++x) { for (int y = 0; y < conf.getChunkBlockSize(); ++y) { /* Calculate dilution */
            if(Material.movable(ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[1]))) {
                World.setUnit(x,y,conf.getChunkBlockSize(), output, avgOfUnitsWithinDistance(x,y,inputs[0], inputs[1]));
            }else{
                World.setUnit(x,y, conf.getChunkBlockSize(), output, World.getUnit(x,y, conf.getChunkBlockSize(), inputs[1]));
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

    /**
     * Provides a refined version of the current elemental aspect
     * @param inputs [0]: elements; [1]: ethereal; [2]: scalars
     * @param output elements
     */
    private void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = conf.getChunkBlockSize() - 1;x >= 0; --x)for(int y = conf.getChunkBlockSize() - 1 ; y >= 0; --y) {
            Material.Elements currentElement = EtherealAspectStrategy.getElementEnum(x,y,conf.getChunkBlockSize(),inputs[1]);
            float currentUnit = World.getUnit(x,y,conf.getChunkBlockSize(), inputs[2]);
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
            ElementalAspectStrategy.setElement(x,y,conf.getChunkBlockSize(),output,currentElement);
            RealityAspectStrategy.setPriority(x,y, conf.getChunkBlockSize(), output, ElementalAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), inputs[0]));
        }
    }

    private void processTypeUnitsPhase(FloatBuffer[] inputs, FloatBuffer output) {
        for(int x = conf.getChunkBlockSize() - 1;x >= 0; --x) for(int y = conf.getChunkBlockSize() - 1 ; y >= 0; --y) {
            Material.Elements currentElement = ElementalAspectStrategy.getElementEnum(x,y,conf.getChunkBlockSize(),inputs[0]);
            float currentUnit = World.getUnit(x,y,conf.getChunkBlockSize(), inputs[1]);
            if(Material.Elements.Water == currentElement){
                if(y > (conf.getChunkBlockSize() * 0.9)){
                    currentUnit -= currentUnit * 0.02f;
                }
            }

            if(Material.Elements.Fire == currentElement){
                if(
                    (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                    && (currentUnit < avgOfUnit(x,y,inputs[0],inputs[1], Material.Elements.Fire))
                ){
                    currentUnit -= currentUnit * 0.1f;
                }else
                if(
                    (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                ){
                    currentUnit -= currentUnit * 0.05f;
                }
            }

            /* TODO: Make nearby fire consume compatible Earth */
            currentUnit = Math.max(0.1f,currentUnit);
            World.setUnit(x,y,conf.getChunkBlockSize(),output,currentUnit);
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



    /**
     * A function to propose force updates based on material properties
     * @param inputs: none
     * @param output the initialized proposed changes
     */
    private void initChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < conf.getChunkBlockSize(); ++x){ for(int y = 1; y < conf.getChunkBlockSize(); ++y){
            ElementalAspectStrategy.setOffsetCode(x,y, conf.getChunkBlockSize(), output, 0);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, 0);
            ElementalAspectStrategy.setToApply(x,y, conf.getChunkBlockSize(), output, 0);
        }}
    }

    /**
     * A function to propose force updates based on material properties
     * @param inputs: [0]: elements; [1]: dynamics; [2]: scalars; [3]: ethereal
     * @param output the resulting updated dynamics
     */
    private void proposeForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < conf.getChunkBlockSize(); ++x){ for(int y = 1; y < conf.getChunkBlockSize(); ++y){
            float forceX = 0.0f;
            float forceY = 0.0f;

            /*!Note: Adding gravity is handled in the post-process phase, not here, to handle collisions correctly */
            if(!Material.discardable(ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0]),World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]))){
                forceX = ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[1]);
                forceY = ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[1]);
            }

            if(Material.Elements.Ether == ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0])){
                for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                    if ( /* in the bounds of the chunk.. */
                        (0 <= nx)&&(conf.getChunkBlockSize() > nx)&&(0 <= ny)&&(conf.getChunkBlockSize() > ny)
                        &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* ..after the not immediate neighbourhood.. */
                        &&(World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]) <= World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]))
                        &&(Material.Elements.Ether == ElementalAspectStrategy.getElementEnum(nx,ny, conf.getChunkBlockSize(), inputs[0]))
                    ){ /* ..Calculate forces from surplus ethers */
                        float aether_diff = Math.max(-10.5f, Math.min(10.5f,
                            ( EtherealAspectStrategy.aetherValueAt(x,y, conf.getChunkBlockSize(), inputs[3]) - EtherealAspectStrategy.aetherValueAt(nx,ny, conf.getChunkBlockSize(), inputs[3]))
                        ));
                        forceX += ((nx - x) * aether_diff);
                        forceY += ((ny - y) * aether_diff);
                    }
                }
            }

            if(Material.MechaProperties.Fluid == Material.getState(ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]))){
                if(Material.isSameMat(
                    ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]),
                    ElementalAspectStrategy.getElementEnum(x,y-1, conf.getChunkBlockSize(), inputs[0]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2])
                )){ /* TODO: define the water cell force behavior correctly: each water cell aims to be 1.5 cells from one another */
                    /* the cell is a liquid on top of another liquid, so it must move. */
                    if((0.0f < forceX)&&(6.0f > forceX))forceX *= 1.2f;
                    else{
                        forceX = rnd.nextInt(6) - 3;
                        forceY = 1.00f;
                    }
                }
            }else if(Material.MechaProperties.Plasma == Material.getState(getElement(x,y), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]))){
                forceX += rnd.nextInt(4) - 2;
            }/* TODO: Make gases loom, instead of staying still ( move about a bit maybe? )  */

            ElementalAspectStrategy.setForce(x,y, conf.getChunkBlockSize(), output,forceX,forceY);
            ElementalAspectStrategy.setGravityCorrection(x,y, conf.getChunkBlockSize(), output, 0); /* Gravity correction is not part of this phase */
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, velocityMaxTicks);
        } }
    }

    /**
     * Provides proposed cell switches based on Forces
     * @param inputs [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output proposed switches, toApply set all 0
     */
    private void proposeChangesFromForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            int newVelocityTick = ElementalAspectStrategy.getVelocityTick(x,y, conf.getChunkBlockSize(), inputs[2]);
            int targetX = RealityAspectStrategy.getTargetX(x,y, conf.getChunkBlockSize(), inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y, conf.getChunkBlockSize(), inputs[0]);
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]); /* a previously proposed change would overwrite a current loop change */

            if((0 == toApply)||(x == targetX && y == targetY)){ /* if no switch was arbitrated in the previously proposed changes */
                /* a target was not proposed previously for this cell, which would overwrite any switch proposed from forces */
                if(
                    !Material.discardable(ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[1]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[3]))
                    && (1 <= ElementalAspectStrategy.getForce(x,y, conf.getChunkBlockSize(), inputs[2]).len())
                ){
                    /* propose to change to the direction of the force */
                    if(1 < Math.abs(ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[2])))targetX = (int)(x + Math.max(-1, Math.min(ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[2]),1)));
                    if(1 < Math.abs(ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[2])))targetY = (int)(y + Math.max(-1, Math.min(ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[2]),1)));
                    targetX = Math.max(0, Math.min(conf.getChunkBlockSize()-1, targetX));
                    targetY = Math.max(0, Math.min(conf.getChunkBlockSize()-1, targetY));

                    /* calculate the final position of the intended target cell */
                    int targetFinalPosX = targetX;
                    int targetFinalPosY = targetY;
                    if(1 < Math.abs(ElementalAspectStrategy.getForceX(targetX,targetY, conf.getChunkBlockSize(), inputs[2])))
                        targetFinalPosX = (int)(targetX + Math.max(-1.1f, Math.min(ElementalAspectStrategy.getForceX(targetX, targetY, conf.getChunkBlockSize(), inputs[2]),1.1f)));
                    if(1 < Math.abs(ElementalAspectStrategy.getForceY(targetX,targetY, conf.getChunkBlockSize(), inputs[2])))
                        targetFinalPosY = (int)(targetY + Math.max(-1.1f, Math.min(ElementalAspectStrategy.getForceY(targetX,targetY, conf.getChunkBlockSize(), inputs[2]),1.1f)));

                    /* see if the two cells still intersect with forces included */
                    if(2 > MiscUtils.distance(x,y,targetFinalPosX,targetFinalPosY)){
                       if(
                            !((x == targetX) && (y == targetY))
                            &&( /* In case both is discardable, then no operations shall commence */
                                Material.discardable(getElement(x,y),World.getUnit(x,y, conf.getChunkBlockSize(), inputs[3]))
                                &&Material.discardable(getElement(targetX,targetY),World.getUnit(targetX,targetY, conf.getChunkBlockSize(), inputs[3]))
                            )
                        ){ /* both cells are discardable, so don't switch */
                            targetX = x;
                            targetY = y;
                        }else if (velocityMaxTicks > newVelocityTick){ /* propose a switch with the target only if the velocity ticks are at threshold */
                            ++newVelocityTick;
                            targetX = x;
                            targetY = y;
                        }
                    }else{ /* the two cells don't intersect by transition, so don't update the target */
                        targetX = x;
                        targetY = y;
                    }
                }
            }
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, newVelocityTick);
            /* TODO: Try to have everything go TOP RIGHT direction: Bottom left corner will duplicate cells somehow... */
            ElementalAspectStrategy.setOffsetCode(x,y,conf.getChunkBlockSize(),output, RealityAspectStrategy.getOffsetCode((targetX - x), (targetY - y)));
            ElementalAspectStrategy.setToApply(x,y, conf.getChunkBlockSize(),output, 0);
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
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            float offsetCode = 0;
            float toApply = 0;

            /* Initialize local data */
            int minIndexX = Math.max((x-index_radius), 0);
            int maxIndexX = Math.min((x+index_radius), conf.getChunkBlockSize());
            int minIndexY = Math.max((y-index_radius), 0);
            int maxIndexY = Math.min((y+index_radius), conf.getChunkBlockSize());
            for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                int sx = ix - x + (index_radius);
                int sy = iy - y + (index_radius);
                priority[sx][sy] = (int)( /* The priority of the given cell consist of..  */
                    ElementalAspectStrategy.getForce(ix,iy, conf.getChunkBlockSize(), inputs[2]).len() /* ..the power of the force on it.. */
                    + ElementalAspectStrategy.getWeight(ix,iy, conf.getChunkBlockSize(), inputs[1], inputs[3]) /* .. and its weight */
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
                    int targetOfCX = RealityAspectStrategy.getTargetX(ix,iy,conf.getChunkBlockSize(),inputs[0]);
                    int targetOfCY = RealityAspectStrategy.getTargetY(ix,iy,conf.getChunkBlockSize(),inputs[0]);
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
                            ||(
                                (priority[highestPrioLocalX][highestPrioLocalY] < priority[localSourceX][localSourceY])
                                ||(
                                    (priority[highestPrioLocalX][highestPrioLocalY] == priority[localSourceX][localSourceY])
                                    &&(
                                        priority[highestPrioLocalX][highestPrioLocalY]
                                            < (priority[localSourceX][localSourceY] + ElementalAspectStrategy.getPriority(ix, iy, conf.getChunkBlockSize(), inputs[1]))
                                    )
                                )
                            )
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
                        if((x == highestPrioX)&&(y == highestPrioY)){
                            toApply = 2;
                            offsetCode = RealityAspectStrategy.getOffsetCode((highestTargetX - x), (highestTargetY - y));
                        }else{ /* if((x == highestTargetX)&&(y == highestTargetY)){ /* This is always true here.. */
                            toApply = 1;
                            offsetCode = RealityAspectStrategy.getOffsetCode((highestPrioX - x), (highestPrioY - y));
                        }
                    }
                    /*!Note: Only set the target if the current cell is actually the highest priority;
                     * because the swap will be decided for the target(other cell) based on this information
                     * */
                    break;
                }
            }

            /*!Note: At this point highestPrio* and highestTarget*
             * should contain either -2 or the highest priority switch request involving c
             * */
            RealityAspectStrategy.setOffsetCode(x,y, conf.getChunkBlockSize(), output, offsetCode);
            RealityAspectStrategy.setToApply(x,y, conf.getChunkBlockSize(),output, toApply);
        }}
    }

    /**
     * Applies the changes to forces proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output dynamics buffer updated with proper forces
     */
    private void applyChangesDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){ /* TODO: Define edges as connection point to other chunks */
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]);
            int targetX = ElementalAspectStrategy.getTargetX(x,y,conf.getChunkBlockSize(), inputs[0]);
            int targetY = ElementalAspectStrategy.getTargetY(x,y,conf.getChunkBlockSize(), inputs[0]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[2]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[2]);
            float weight = ElementalAspectStrategy.getWeight(x,y, conf.getChunkBlockSize(), inputs[1], inputs[3]);
            float gravityCorrection = ElementalAspectStrategy.getWeight(x,y, conf.getChunkBlockSize(), inputs[1], inputs[3]);
            int EH = 0;

            if( /* Update the forces on a cell.. */
                (0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y) /* ..when it is inside bounds.. */
                &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,conf.getChunkBlockSize(), inputs[0])) /* ..only if it wants to switch..  */
                &&((targetX >= 0)&&(targetX < conf.getChunkBlockSize())&&(targetY >= 0)&&(targetY < conf.getChunkBlockSize())) /* ..but only if the target is also inside the bounds of the chunk */
            ){
                gravityCorrection = 0; /* Gravity is being added at forces update, so no need to re-add it at the end of the loop */
                if( ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, conf.getChunkBlockSize(), inputs[1], inputs[3]) ){ /* The cells swap, decreasing forces on both *//* TODO: Also decrease the force based on the targets weight */
                    forceX += -forceX * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceX))) );
                    forceY += -forceY * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceY))) );
                    forceX += (myMiscUtils.getGravity(x,y).x * weight);
                    forceY += (myMiscUtils.getGravity(x,y).y * weight);
                    EH = 1;
                }else if(ElementalAspectStrategy.aCanMoveB(targetX,targetY,x,y, conf.getChunkBlockSize(), inputs[1], inputs[3])){ /* The cells collide, updating forces, but no swapping */
                    Vector2 u1 = ElementalAspectStrategy.getForce(x,y, conf.getChunkBlockSize(), inputs[2]).cpy().nor();
                    float m2 = ElementalAspectStrategy.getWeight(targetX, targetY, conf.getChunkBlockSize(), inputs[1], inputs[3]);
                    Vector2 u2 = ElementalAspectStrategy.getForce(targetX, targetY, conf.getChunkBlockSize(), inputs[2]).cpy().nor();
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
                    EH = 2;
                }
            }
            ElementalAspectStrategy.setForceX(x,y, conf.getChunkBlockSize(), output, forceX);
            ElementalAspectStrategy.setForceY(x,y, conf.getChunkBlockSize(), output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, ElementalAspectStrategy.getVelocityTick(x,y, conf.getChunkBlockSize(), inputs[2]));
            ElementalAspectStrategy.setGravityCorrection(x,y, conf.getChunkBlockSize(), output, gravityCorrection);
        }}
    }

    /**
     * Decides whether the proposed changes are swaps or collisions
     * @param inputs [0]: proposed changes; [1]: elements; [2]:  scalars
     * @param output the proposed changes where toApply means swaps need to happen
     */
    private void arbitrateInteractionsPhase(FloatBuffer[] inputs, FloatBuffer output){
        /* Note: At this point the switches are supposed to be mutual: If a <> b, then every time b <> a  */
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            float toApply = RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]);
            int targetX = RealityAspectStrategy.getTargetX(x,y,conf.getChunkBlockSize(), inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y,conf.getChunkBlockSize(), inputs[0]);
            if( /* Check for swaps */
                (0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y) /* ..when cell is inside bounds.. */
                &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,conf.getChunkBlockSize(), inputs[0])) /* ..and it wants to switch..  */
                &&((targetX >= 0)&&(targetX < conf.getChunkBlockSize())&&(targetY >= 0)&&(targetY < conf.getChunkBlockSize())) /* ..but only if the target is also inside the bounds of the chunk */
            ){
                if( /* this cell can not move its target */
                    ((2 == toApply)&&(!ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, conf.getChunkBlockSize(), inputs[1], inputs[2])))
                    ||((1 == toApply)&&(!ElementalAspectStrategy.aCanMoveB(targetX,targetY, x,y, conf.getChunkBlockSize(), inputs[1], inputs[2])))
                    ||(
                        (!ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, conf.getChunkBlockSize(), inputs[1], inputs[2]))
                        &&(
                            (x != RealityAspectStrategy.getTargetX(targetX,targetY, conf.getChunkBlockSize(), inputs[0]))
                            ||(y != RealityAspectStrategy.getTargetY(targetX,targetY, conf.getChunkBlockSize(), inputs[0]))
                        )
                    )
                ){
                    toApply = 0;
                }
            }
            RealityAspectStrategy.setOffsetCode(x,y, conf.getChunkBlockSize(), output, RealityAspectStrategy.getOffsetCode(x,y, conf.getChunkBlockSize(), inputs[0]));
            RealityAspectStrategy.setToApply(x,y, conf.getChunkBlockSize(),output, toApply);
        }}
    }

    /**
     * Post-processing with the dynamics:basically corrects with the gravity based on GravityCorrection
     * @param inputs [0]: elements; [1]: dynamics; [2]: scalars
     * @param output the post-processed dynamics buffer
     */
    private void mechanicsPostProcessDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            float gravityCorrection = ElementalAspectStrategy.getGravityCorrection(x,y, conf.getChunkBlockSize(), inputs[1]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, conf.getChunkBlockSize(), inputs[1]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, conf.getChunkBlockSize(), inputs[1]);
            if(
                (0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
                &&(0 < gravityCorrection)&&Material.movable(ElementalAspectStrategy.getElementEnum(x,y, conf.getChunkBlockSize(), inputs[0]), World.getUnit(x,y, conf.getChunkBlockSize(), inputs[2]))
            ){
                forceX += gravityCorrection * myMiscUtils.getGravity(x,y).x;
                forceY += gravityCorrection * myMiscUtils.getGravity(x,y).y;
            }
            ElementalAspectStrategy.setForceX(x,y, conf.getChunkBlockSize(), output, forceX);
            ElementalAspectStrategy.setForceY(x,y, conf.getChunkBlockSize(), output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), output, ElementalAspectStrategy.getVelocityTick(x,y, conf.getChunkBlockSize(), inputs[1]));
            ElementalAspectStrategy.setGravityCorrection(x,y, conf.getChunkBlockSize(), output, 0);
        }}
    }

    @Override
    public void processMechanics(World parent) {
        /* Init Mechanics phase */
        backend.runPhase(initChangesPhaseIndex);
        BufferUtils.copy(backend.getOutput(initChangesPhaseIndex), proposedChanges);

        for(int x = 1; x < conf.getChunkBlockSize()-1; ++x){
            for(int y = conf.getChunkBlockSize()-2; y > 0; --y){
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
            backend.runPhase(proposeForcesPhaseIndex); /* output: new dynamics */

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

            arbitrateInteractionsPhaseInputs[0] = backend.getOutput(arbitrateChangesPhaseIndex);
            arbitrateInteractionsPhaseInputs[1] = elements;
            parent.provideScalarsTo(arbitrateInteractionsPhaseInputs, 2);
            backend.setInputs(arbitrateInteractionsPhaseInputs);
            backend.runPhase(arbitrateInteractionsPhaseIndex);

            parent.switchValues(backend.getOutput(arbitrateInteractionsPhaseIndex));
            if(i == velocityMaxTicks-1){
                BufferUtils.copy(backend.getOutput(arbitrateInteractionsPhaseIndex), proposedChanges);
            }
        }

        mechanicsPostProcessDynamicsPhaseInputs[0] = elements;
        mechanicsPostProcessDynamicsPhaseInputs[1] = dynamics;
        parent.provideScalarsTo(mechanicsPostProcessDynamicsPhaseInputs, 2);
        backend.setInputs(mechanicsPostProcessDynamicsPhaseInputs);
        backend.runPhase(mechanicsPostProcessDynamicsPhaseIndex);
        BufferUtils.copy(backend.getOutput(mechanicsPostProcessDynamicsPhaseIndex), dynamics);
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    @Override
    public void postProcess(World parent) {
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                setElement(x,y, parent.etherealPlane.elementAt(x,y));/* TODO: keep priority should buffered backend be used */
            }
        }
    }

    /**
     * Create a simple pond with some fire on one side
     * @param floorHeight - the height of the ground floor
     */
    public void pondWithGrill(World parent, int floorHeight){
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ /* create the ground floor */
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                ElementalAspectStrategy.setForce(x,y, conf.getChunkBlockSize(), dynamics,0,0);
                if(
                    (y <= floorHeight)
                    &&(0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
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
                x = (int)(conf.getChunkBlockSize()/2 + Math.cos(sector) * radius);
                x = Math.max(0, Math.min(conf.getChunkBlockSize(), x));
                y = (int)(floorHeight + Math.sin(sector) * radius);
                y = Math.max(0, Math.min(conf.getChunkBlockSize(), y));
                if(y <= (floorHeight - (floorHeight/4)) && (0 == rnd.nextInt(3)))
                    parent.setUnit(x,y,parent.getUnit(x,y) * 2.5f);
                setElement(x,y, Material.Elements.Water);
            }
        }

        /* Create a fire */
        x = conf.getChunkBlockSize()/4;
        y = floorHeight + 1;
        setElement(x,y, Material.Elements.Fire);
        setElement(x-1,y, Material.Elements.Fire);
        setElement(x+1,y, Material.Elements.Fire);
        setElement(x,y+1, Material.Elements.Fire);
    }

    public void provideElementsTo(FloatBuffer[] inputs, int inputIndex){
        inputs[inputIndex] = elements;
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


    public Material.Elements getElement(int x, int y){
        return ElementalAspectStrategy.getElementEnum(x,y,conf.getChunkBlockSize(), elements);
    }

    public void setElement(int x, int y, Material.Elements element){
        ElementalAspectStrategy.setElement(x,y,conf.getChunkBlockSize(),elements,element);
    }

    public void debugMeasure(World parent){
//        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ /* create the ground floor */
//            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
//                unitDebugVariable[x][y] = parent.getUnit(x,y);
//                if(getElement(x,y) == Material.Elements.Fire){
//                }
//            }
//        }
    }

    public void debugPrint(World parent){

    }

    public void setDebugViewPercent(float percent){
        debugViewPercent = percent;
    }

    private boolean showForces = false;
    public void setShowForces(boolean show){
        showForces = show;
    }

    float debugViewPercent = 0;
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
        if( x == conf.getChunkBlockSize()/2 &&  y == conf.getChunkBlockSize() / 3)
            unitsDiff = 0.8f;

        Color debugColor;
        /* Red <-> Blue: left <-> right */
        float offsetX = (RealityAspectStrategy.getTargetX(x,y, conf.getChunkBlockSize(), proposedChanges) - x + 1) / 2.0f;
        float offsetY = (RealityAspectStrategy.getTargetY(x,y, conf.getChunkBlockSize(), proposedChanges) - y + 1) / 2.0f;
        debugColor = new Color(1.0f - offsetX, offsetY, offsetX, 1.0f); /* <-- proposed offsets *///        float prio = getPriority(x,y, conf.getChunkBlockSize(), elements)/maxPrio;
//        Color debugColor = new Color(prio, prio, prio, 1.0f);
//                    offsetR,
////                netherDebugVal(parent,x,y)/parent.getEtherealPlane().netherValueAt(x,y),//Math.max(1.0f, Math.min(0.0f, forces[x][y].x)),
//                //-Math.max(0.0f, Math.min(-5.0f, forces[x][y].y))/5.0f,
////                    (0 == touchedByMechanics[x][y])?0.0f:1.0f,
//                    1f,//unitsDiff,
////                aetherDebugVal(parent,x,y)/parent.getEtherealPlane().aetherValueAt(x,y),
//                    offsetB,
//                1.0f
//            );
            defColor.lerp(debugColor,debugViewPercent);
//        }
        return defColor;
    }

}
