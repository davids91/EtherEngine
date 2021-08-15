package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.ElementalAspectStrategy;
import com.crystalline.aether.models.world.EtherealAspectStrategy;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.models.world.RealityAspectStrategy;
import com.crystalline.aether.services.CPUBackend;
import com.crystalline.aether.services.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    private static final float aetherWeightInUnits = 4;
    private static final float etherReleaseThreshold = 0.1f;

    protected final int sizeX;
    protected final int sizeY;

    /**
     * A texture image representing the ethereal values in the plane
     * - R: Moving substance
     * - G: Unsued yet...
     * - B: Stationary substance
     * - A: Unsued
     * Inside the backend there is also another buffer structure used for sharing Ether in-between cells,
     * which is not declared explicitly as it is part of the backend, and never used outside the calculation phases
     * - R: Released Nether
     * - G: Average Released Nether in context
     * - B: Released Aether
     * - A: Released Aether in context
     * */
    private FloatBuffer etherValues;

    private final CPUBackend backend;
    private final int preprocessPhaseIndex;
    private final int sharingPhaseIndex;
    private final int finalizePhaseIndex;
    private final int processTypesPhaseIndex;
    private final int determineUnitsPhaseIndex;
    private final int defineByElementalPhaseIndex;
    private final int switchEtherPhaseIndex;

    private final FloatBuffer[] finalizeInputs;
    private final FloatBuffer[] sharingInputs;
    private final FloatBuffer[] preProcessInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] determineUnitsPhaseInputs;
    private final FloatBuffer[] defineByElementalPhaseInputs;
    private final FloatBuffer[] switchEtherPhaseInputs;

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        backend = new CPUBackend();
        etherValues = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();

        preprocessPhaseIndex = backend.addPhase(this::preProcessCalculationPhase, (Config.bufferCellSize * sizeX * sizeY));
        sharingPhaseIndex = backend.addPhase(this::sharingCalculationPhase, (Config.bufferCellSize * sizeX * sizeY));
        finalizePhaseIndex = backend.addPhase(this::finalizeCalculationPhase, (Config.bufferCellSize * sizeX * sizeY));
        processTypesPhaseIndex = backend.addPhase(this::processTypesPhase, (Config.bufferCellSize * sizeX * sizeY));
        determineUnitsPhaseIndex = backend.addPhase(this::determineUnitsPhase, (Config.bufferCellSize * sizeX * sizeY));
        defineByElementalPhaseIndex = backend.addPhase(this::defineByElementalPhase, (Config.bufferCellSize * sizeX * sizeY));
        switchEtherPhaseIndex = backend.addPhase(this::switchEtherPhase, (Config.bufferCellSize * sizeX * sizeY));

        preProcessInputs = new FloatBuffer[]{backend.getOutput(finalizePhaseIndex)};
        sharingInputs = new FloatBuffer[]{backend.getOutput(preprocessPhaseIndex)};
        finalizeInputs = new FloatBuffer[]{etherValues, backend.getOutput(sharingPhaseIndex)};
        processTypesPhaseInputs = new FloatBuffer[]{etherValues, null, null};
        determineUnitsPhaseInputs = new FloatBuffer[]{etherValues};
        defineByElementalPhaseInputs = new FloatBuffer[]{null, null};
        switchEtherPhaseInputs = new FloatBuffer[2];

        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{BufferUtils.clone(etherValues)};
    }

    @Override
    protected void setState(Object[] state) {
        etherValues = (FloatBuffer)state[0];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                setAether(x,y, 1);
                setNether(x,y, Material.ratioOf(Material.Elements.Air));
            }
        }
    }

    private void defineByElementalPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                float currentUnits = World.getUnit(x,y, sizeX, inputs[1]);
                Material.Elements currentElement = ElementalAspectStrategy.getElementEnum(x,y, sizeX, inputs[0]);
                float newAether = ((2.0f * currentUnits) / (1.0f + Material.ratioOf(currentElement)));
                if(0 < currentUnits) {
                    EtherealAspectStrategy.setAether(x,y, sizeX, output, newAether);
                    EtherealAspectStrategy.setNether(x,y, sizeX, output, ( newAether * Material.ratioOf(currentElement) ));
                }else{
                    EtherealAspectStrategy.setAether(x,y, sizeX, output,1);
                    EtherealAspectStrategy.setNether(x,y, sizeX, output, Material.ratioOf(Material.Elements.Air));
                }
            }
        }
    }

    public void defineBy(ElementalAspect plane, World parent){
        plane.provideElementsTo(defineByElementalPhaseInputs,0);
        parent.provideScalarsTo(defineByElementalPhaseInputs, 1);
        backend.setInputs(defineByElementalPhaseInputs);
        backend.runPhase(defineByElementalPhaseIndex);
        BufferUtils.copy(backend.getOutput(defineByElementalPhaseIndex), etherValues);
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes;[0]: scalars
     * @param output elements buffer
     */
    private void switchEtherPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < sizeX; ++x){ for(int y = 0; y < sizeY; ++y){
            float aetherValue = EtherealAspectStrategy.getAetherValue(x, y, sizeX, inputs[1]);
            float netherValue = EtherealAspectStrategy.getNetherValue(x, y, sizeX, inputs[1]);
            if(0 != RealityAspectStrategy.getOffsetCode(x,y,sizeX, inputs[0])){
                int targetX = RealityAspectStrategy.getTargetX(x,y,sizeX, inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,sizeX, inputs[0]);
                int toApply = (int) RealityAspectStrategy.getToApply(x,y, sizeX, inputs[0]);
                if(
                    (0 < x)&&(sizeX-1 > x)&&(0 < y)&&(sizeY-1 > y)
                    &&(0 < toApply)
                    &&(targetX >= 0)&&(targetX < sizeX)
                    &&(targetY >= 0)&&(targetY < sizeY)
                ){
                    aetherValue = EtherealAspectStrategy.getAetherValue(targetX, targetY, sizeX, inputs[1]);
                    netherValue = EtherealAspectStrategy.getNetherValue(targetX, targetY, sizeX, inputs[1]);
                }
            }
            EtherealAspectStrategy.setAether(x,y, sizeX, output, aetherValue);
            EtherealAspectStrategy.setNether(x,y, sizeX, output, netherValue);
        }}
    }

    @Override
    public void switchValues(FloatBuffer proposals) {
        switchEtherPhaseInputs[0] = proposals;
        switchEtherPhaseInputs[1] = etherValues;
        backend.setInputs(switchEtherPhaseInputs);
        backend.runPhase(switchEtherPhaseIndex);
        BufferUtils.copy(backend.getOutput(switchEtherPhaseIndex), etherValues);
    }

    private float avgOf(int x, int y, int cellOffset, FloatBuffer table){
        float ret = 0.0f;
        float divisor = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                ret += BufferUtils.get(nx,ny, sizeX, Config.bufferCellSize,cellOffset,table);
                ++divisor;
            }
        }
        return (ret / divisor);
    }

    public float getMaxNether(int x, int y){
        return aetherValueAt(x,y) * Material.ratioOf(Material.Elements.Fire);
    }

    public float getMinAether(int x, int y){
        return netherValueAt(x,y) / Material.ratioOf(Material.Elements.Earth);
    }


    private void preProcessCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < sizeX; ++x) { /* Preprocess Ether */
            for (int y = 0; y < sizeY; ++y) {
                EtherealAspectStrategy.setReleasedNether(x,y,sizeX,output,0);
                EtherealAspectStrategy.setAvgReleasedNether(x,y,sizeX,output,0);
                EtherealAspectStrategy.setReleasedAether(x,y,sizeX,output,0);
                EtherealAspectStrategy.setAvgReleasedAether(x,y,sizeX,output,0);
                float currentRatio = EtherealAspectStrategy.getRatio(x,y, sizeX, inputs[0]);
                if( 0.5 < Math.abs(currentRatio - Material.ratioOf(Material.Elements.Ether)) ){
                    float aetherToRelease = (EtherealAspectStrategy.aetherValueAt(x,y, sizeX, inputs[0]) - EtherealAspectStrategy.getMinAether(x,y, sizeX, inputs[0]));
                    float netherToRelease = (EtherealAspectStrategy.netherValueAt(x,y, sizeX, inputs[0]) - EtherealAspectStrategy.getMaxNether(x,y, sizeX, inputs[0]));
                    if(
                        ( EtherealAspectStrategy.netherValueAt(x,y, sizeX, inputs[0]) >= (EtherealAspectStrategy.getMaxNether(x,y, sizeX, inputs[0])) + (EtherealAspectStrategy.aetherValueAt(x,y, sizeX, inputs[0]) * etherReleaseThreshold) )
                        || ( EtherealAspectStrategy.aetherValueAt(x,y, sizeX, inputs[0]) >= (EtherealAspectStrategy.getMinAether(x,y, sizeX, inputs[0]) + (EtherealAspectStrategy.netherValueAt(x,y,sizeX, inputs[0]) * etherReleaseThreshold)) )
                    ){
                        if(netherToRelease >= aetherToRelease){
                            EtherealAspectStrategy.setReleasedNether(x,y, sizeX, output,netherToRelease / 9.0f);
                        }else{
                            EtherealAspectStrategy.setReleasedAether(x,y, sizeX, output, aetherToRelease / 9.0f);
                        }
                    }
                }
            }
        }
    }

    private void sharingCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < sizeX; ++x) { /* Sharing released ether */
            for (int y = 0; y < sizeY; ++y) {
                /* save released ether from the previous phase */
                EtherealAspectStrategy.setReleasedNether(x,y, sizeX, output, EtherealAspectStrategy.getReleasedNether(x, y, sizeX, inputs[0]));
                EtherealAspectStrategy.setReleasedAether(x,y, sizeX, output, EtherealAspectStrategy.getReleasedAether(x, y, sizeX, inputs[0]));

                /* calculate shared ether from released ether */
                EtherealAspectStrategy.setAvgReleasedAether(x,y, sizeX, output, avgOf(x, y, 2,inputs[0]));
                EtherealAspectStrategy.setAvgReleasedNether(x,y, sizeX, output, avgOf(x, y, 0,inputs[0]));
            }
        }
    }
    /* which is not declared explicitly as it is part of the backend, and never used outside the calculation phases
     * - R: Released Nether : etReleasedNether
     * - G: Average Released Nether in context : etAvgReleasedNether
     * - B: Released Aether : etReleasedAether
     * - A: Released Aether in context : etAvgReleasedAether
     * */
    private void finalizeCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < sizeX; ++x) { /* finalizing Ether */
            for (int y = 0; y < sizeY; ++y) {

                /* Subtract the released Ether, and add the shared */
                /* TODO: The more units there is, the more ether is absorbed */
                float newAetherValue = Math.max( 0.01f, /* Update values with safety cut */
                    EtherealAspectStrategy.aetherValueAt(x,y, sizeX, inputs[0]) - EtherealAspectStrategy.getReleasedAether(x,y, sizeX, inputs[1])
                    + (EtherealAspectStrategy.getAvgReleasedAether(x,y, sizeX, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );
                float newNetherValue = Math.max( 0.01f,
                    EtherealAspectStrategy.netherValueAt(x,y, sizeX, inputs[0]) - EtherealAspectStrategy.getReleasedNether(x,y, sizeX, inputs[1])
                    + (EtherealAspectStrategy.getAvgReleasedNether(x,y, sizeX, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                EtherealAspectStrategy.setAether(x,y, sizeX, output, newAetherValue);
                EtherealAspectStrategy.setNether(x,y, sizeX, output, newNetherValue);
            }
        }
    }

    private void processEther() {
        /* Pre-process phase: copy the released ether for each buffer */
        backend.setInputs(preProcessInputs);
        backend.runPhase(preprocessPhaseIndex);

        /* sharing phase: released ether to be averaged together */
        backend.setInputs(sharingInputs);
        backend.runPhase(sharingPhaseIndex);

        /* finalize phase: final ether to be read and decided */
        finalizeInputs[0] = etherValues;
        backend.setInputs(finalizeInputs);
        backend.runPhase(finalizePhaseIndex);
        BufferUtils.copy(backend.getOutput(finalizePhaseIndex), etherValues);
    }

    @Override
    public void processUnits(World parent){
        processEther();
        parent.setScalars(determineUnits(parent));
    }

    private void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < sizeX; ++x){ /* Take over unit changes from Elemental plane */
            for(int y = 0; y < sizeY; ++y){
                float oldRatio = EtherealAspectStrategy.getRatio(x,y, sizeX, inputs[0]);
                float oldUnit = getUnit(x,y, sizeX, inputs[0]);
                Material.Elements oldElement = EtherealAspectStrategy.getElementEnum(x,y,sizeX,inputs[0]);
//                Material.Elements oldElement = ElementalAspect.getElementEnum(x,y,sizeX,inputs[1]);
                float newAetherValue = (
                    (
                        EtherealAspectStrategy.aetherValueAt(x,y, sizeX, inputs[0])* aetherWeightInUnits + EtherealAspectStrategy.netherValueAt(x,y, sizeX, inputs[0]))
                        * World.getUnit(x,y,sizeX,inputs[2])
                    ) / (oldUnit * aetherWeightInUnits + oldUnit * oldRatio
                );
                EtherealAspectStrategy.setAether(x,y, sizeX, output, newAetherValue);
                EtherealAspectStrategy.setNether(x,y, sizeX, output, (newAetherValue * oldRatio));
            }
        }
    }


    @Override
    public void processTypes(World parent){
        processTypesPhaseInputs[0] = etherValues;
        parent.getElementalPlane().provideElementsTo(processTypesPhaseInputs, 1);
        parent.provideScalarsTo(processTypesPhaseInputs,2); /* TODO: This might be needed only once? */
        backend.setInputs(processTypesPhaseInputs);
        backend.runPhase(processTypesPhaseIndex);
        BufferUtils.copy(backend.getOutput(processTypesPhaseIndex), etherValues);
    }

    public static float getUnit(int x, int y,  int sizeX, FloatBuffer buffer){
        return ( /* Since Aether is the stabilizer, it shall weigh more */
            (EtherealAspectStrategy.getAetherValue(x,y, sizeX, buffer)* aetherWeightInUnits + EtherealAspectStrategy.getNetherValue(x,y, sizeX, buffer)) /(aetherWeightInUnits+1)
        );
    }

    private float getUnit(int x, int y){
        return getUnit(x,y,sizeX, etherValues);
    }

    private void determineUnitsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                World.setUnit(x,y,sizeX,output,getUnit(x,y, sizeX, inputs[0]));
            }
        }
    }

    @Override
    public FloatBuffer determineUnits(World parent) {
        determineUnitsPhaseInputs[0] = etherValues;
        backend.setInputs(determineUnitsPhaseInputs);
        backend.runPhase(determineUnitsPhaseIndex);
        return backend.getOutput(determineUnitsPhaseIndex);
    }

    @Override
    public void processMechanics(World parent) {

    }

    @Override
    public void postProcess(World parent) {
        /* TODO: Decide para-modifiers ( e.g. heat, light?! ) */
        /* TODO: Increase heat where there is a surplus Nether */
    }

    public void provideEtherTo(FloatBuffer[] inputs, int inputIndex){
        inputs[inputIndex] = etherValues;
    }
    public void setEther(FloatBuffer value){
        BufferUtils.copy(value, etherValues);
    }

    public float aetherValueAt(int x, int y){
        return EtherealAspectStrategy.getAetherValue(x,y, sizeX, etherValues);
    }

    public float netherValueAt(int x, int y){
        return EtherealAspectStrategy.getNetherValue(x,y, sizeX, etherValues);
    }

    public float getRatio(int x, int y){
        return EtherealAspectStrategy.getRatio(x,y,sizeX,etherValues);
    }

    public Material.Elements elementAt(int x, int y){
        return EtherealAspectStrategy.getElementEnum(x,y,sizeX,etherValues);
    }

    public void addAetherTo(int x, int y, float value){
        setAether(x,y, Math.max(0.01f, aetherValueAt(x,y) + value));
    }
    public void setAether(int x, int y, float value){
        EtherealAspectStrategy.setAether(x,y, sizeX, etherValues, Math.max(0.01f, value));
    }

    public void addNether(int x, int y, float value){
        setNether(x,y, Math.max(0.01f, netherValueAt(x,y) + value));
    }

    public void setNether(int x, int y, float value){
        EtherealAspectStrategy.setNether(x,y, sizeX, etherValues, Math.max(0.01f, value));
    }
    public void tryToEqualize(int x, int y, float aetherDelta, float netherDelta, float ratio){
        EtherealAspectStrategy.tryToEqualize(x,y, sizeX, etherValues,aetherDelta,netherDelta,ratio);
    }

}
