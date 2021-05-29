package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
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

    private final FloatBuffer[] finalizeInputs;
    private final FloatBuffer[] sharingInputs;
    private final FloatBuffer[] preProcessInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] determineUnitsPhaseInputs;

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

        preProcessInputs = new FloatBuffer[]{backend.getOutput(finalizePhaseIndex)};
        sharingInputs = new FloatBuffer[]{backend.getOutput(preprocessPhaseIndex)};
        finalizeInputs = new FloatBuffer[]{etherValues, backend.getOutput(sharingPhaseIndex)};
        processTypesPhaseInputs = new FloatBuffer[]{etherValues, null};
        determineUnitsPhaseInputs = new FloatBuffer[]{etherValues};

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
                setAetherTo(x,y, etherValues, 1);
                setNetherTo(x,y, etherValues, Material.ratioOf(Material.Elements.Air));
            }
        }
    }

    public void defineBy(ElementalAspect plane, World parent){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < parent.getUnit(x,y)) {
                    setAetherTo(x,y, etherValues, (((2.0f * parent.getUnit(x,y)) / (1.0f + Material.ratioOf(plane.getElement(x, y))))));
                    setNetherTo(x,y, etherValues, ( aetherValueAt(x,y,etherValues)* Material.ratioOf(plane.getElement(x, y))));
                }else{
                    setAetherTo(x,y, etherValues,1);
                    setNetherTo(x,y, etherValues, Material.ratioOf(Material.Elements.Air));
                }
            }
        }
    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        float tmpVal = aetherValueAt(toX,toY,etherValues);
        setAetherTo(toX, toY, etherValues, aetherValueAt(fromX,fromY,etherValues));
        setAetherTo(fromX, fromY, etherValues, tmpVal);

        tmpVal = netherValueAt(toX,toY,etherValues);
        setNetherTo(toX, toY, etherValues, netherValueAt(fromX,fromY,etherValues));
        setNetherTo(fromX, fromY, etherValues, tmpVal);
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
    private float getMaxNether(int x, int y, FloatBuffer buffer){
        return aetherValueAt(x,y,buffer) * Material.ratioOf(Material.Elements.Fire);
    }
    public float getMinAether(int x, int y){
        return netherValueAt(x,y) / Material.ratioOf(Material.Elements.Earth);
    }
    private float getMinAether(int x, int y, FloatBuffer buffer){
        return netherValueAt(x,y,buffer) / Material.ratioOf(Material.Elements.Earth);
    }

    private void preProcessCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < sizeX; ++x) { /* Preprocess Ether */
            for (int y = 0; y < sizeY; ++y) {
                setReleasedNether(x,y,sizeX,output,0);
                setAvgReleasedNether(x,y,sizeX,output,0);
                setReleasedAether(x,y,sizeX,output,0);
                setAvgReleasedAether(x,y,sizeX,output,0);
                float currentRatio = getRatio(x,y, sizeX, inputs[0]);
                if( 0.5 < Math.abs(currentRatio - Material.ratioOf(Material.Elements.Ether)) ){
                    float aetherToRelease = (aetherValueAt(x,y,inputs[0]) - getMinAether(x,y,inputs[0]));
                    float netherToRelease = (netherValueAt(x,y,inputs[0]) - getMaxNether(x,y,inputs[0]));
                    if(
                        ( netherValueAt(x,y,inputs[0]) >= (getMaxNether(x,y,inputs[0])) + (aetherValueAt(x,y,inputs[0]) * etherReleaseThreshold) )
                        || ( aetherValueAt(x,y,inputs[0]) >= (getMinAether(x,y,inputs[0]) + (netherValueAt(x,y,inputs[0]) * etherReleaseThreshold)) )
                    ){
                        if(netherToRelease >= aetherToRelease){
                            setReleasedNether(x,y, sizeX, output,netherToRelease / 9.0f);
                        }else{
                            setReleasedAether(x,y, sizeX, output, aetherToRelease / 9.0f);
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
                setReleasedNether(x,y, sizeX, output, getReleasedNether(x, y, sizeX, inputs[0]));
                setReleasedAether(x,y, sizeX, output, getReleasedAether(x, y, sizeX, inputs[0]));

                /* calculate shared ether from released ether */
                setAvgReleasedAether(x,y, sizeX, output, avgOf(x, y, 2,inputs[0]));
                setAvgReleasedNether(x,y, sizeX, output, avgOf(x, y, 0,inputs[0]));
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
                    aetherValueAt(x,y,inputs[0]) - getReleasedAether(x,y, sizeX, inputs[1])
                    + (getAvgReleasedAether(x,y, sizeX, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );
                float newNetherValue = Math.max( 0.01f,
                    netherValueAt(x,y,inputs[0]) - getReleasedNether(x,y, sizeX, inputs[1])
                    + (getAvgReleasedNether(x,y, sizeX, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                setAetherTo(x,y, output, newAetherValue);
                setNetherTo(x,y, output, newNetherValue);
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
                float oldRatio = getRatio(x,y, sizeX, inputs[0]);
                float oldUnit = getUnit(x,y, sizeX, inputs[0]);
                float newAetherValue = (
                    (
                        aetherValueAt(x,y,inputs[0])* aetherWeightInUnits + netherValueAt(x,y,inputs[0]))
                        * World.getUnit(x,y,sizeX,inputs[1])
                    ) / (oldUnit * aetherWeightInUnits + oldUnit * oldRatio
                );
                setAetherTo(x,y, output, newAetherValue);
                setNetherTo(x,y, output, (newAetherValue * oldRatio));
            }
        }
    }


    @Override
    public void processTypes(World parent){
        processTypesPhaseInputs[0] = etherValues;
        parent.provideScalarsTo(processTypesPhaseInputs,1); /* TODO: This might be needed only once? */
        backend.setInputs(processTypesPhaseInputs);
        backend.runPhase(processTypesPhaseIndex);
        BufferUtils.copy(backend.getOutput(processTypesPhaseIndex), etherValues);
    }

    public static float getUnit(int x, int y,  int sizeX, FloatBuffer buffer){
        return ( /* Since Aether is the stabilizer, it shall weigh more */
            (getAetherValue(x,y, sizeX, buffer)* aetherWeightInUnits + getNetherValue(x,y, sizeX, buffer)) /(aetherWeightInUnits+1)
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

    public static float getAetherValue(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x, y, sizeX, Config.bufferCellSize,2, buffer);
    }
    public static float getNetherValue(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x, y, sizeX, Config.bufferCellSize,0, buffer);
    }
    public float aetherValueAt(int x, int y){
        return getAetherValue(x,y, sizeX, etherValues);
    }
    private float aetherValueAt(int x, int y, FloatBuffer buffer){
        return getAetherValue(x,y, sizeX, buffer);
    }
    public float netherValueAt(int x, int y){
        return getNetherValue(x,y, sizeX, etherValues);
    }
    private float netherValueAt(int x, int y, FloatBuffer buffer){
        return getNetherValue(x,y, sizeX, buffer);
    }
    public static float getRatio(int x, int y, int sizeX, FloatBuffer buffer){
        if(0 != getAetherValue(x,y, sizeX, buffer))
            return (getNetherValue(x,y, sizeX, buffer) / getAetherValue(x,y, sizeX, buffer));
        else return 0;
    }
    public float getRatio(int x, int y){
        return getRatio(x,y,sizeX,etherValues);
    }
    public static float getElement(int x, int y, int sizeX, FloatBuffer buffer){
        return getRatio(x,y, sizeX, buffer);
    }
    public static Material.Elements getElementEnum(int x, int y, int sizeX, FloatBuffer buffer){
        if(getUnit(x,y, sizeX, buffer) <= Material.ratioOf(Material.Elements.Fire)) return Material.Elements.Air;
        else if(0 == Math.abs(getRatio(x,y,sizeX, buffer) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether;
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }
    public Material.Elements elementAt(int x, int y){
        return getElementEnum(x,y,sizeX,etherValues);
    }

    public static void setAether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, sizeX, Config.bufferCellSize,2, buffer, value);
    }
    public void addAetherTo(int x, int y, float value){
        addAetherTo(x,y, etherValues, Math.max(0.01f, aetherValueAt(x,y,etherValues) + value));
    }
    private void addAetherTo(int x, int y, FloatBuffer buffer, float value){
        setAetherTo(x,y, buffer, Math.max(0.01f, aetherValueAt(x,y, buffer) + value));
    }
    public void setAetherTo(int x, int y, FloatBuffer buffer, float value){
        setAether(x,y, sizeX, buffer, Math.max(0.01f, value));
    }
    public static void setNether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x, y, sizeX, Config.bufferCellSize,0, buffer, value);
    }
    public void addNetherTo(int x, int y, float value){
        setNetherTo(x,y, etherValues, Math.max(0.01f, netherValueAt(x,y,etherValues) + value));
    }
    private void addNetherTo(int x, int y, FloatBuffer buffer, float value){
        setNetherTo(x,y, buffer, Math.max(0.01f, netherValueAt(x,y, buffer) + value));
    }
    public void setNetherTo(int x, int y, FloatBuffer buffer, float value){
        setNether(x,y, sizeX, buffer, Math.max(0.01f, value));
    }
    public void tryToEqualize(int x, int y, float aetherDelta, float netherDelta, float ratio){
        tryToEqualize(x,y,etherValues,aetherDelta,netherDelta,ratio);
    }
    private void tryToEqualize(int x, int y, FloatBuffer buffer, float aetherDelta, float netherDelta, float ratio){
        setAetherTo(x,y, buffer, getEqualizeAttemptAetherValue(aetherValueAt(x,y,buffer),netherValueAt(x,y,buffer),aetherDelta,netherDelta,ratio));
        setNetherTo(x,y, buffer, getEqualizeAttemptNetherValue(aetherValueAt(x,y,buffer),netherValueAt(x,y,buffer),aetherDelta,netherDelta,ratio));
        /* TODO: the remainder should be radiated into pra-effects */
    }

    public static float getAetherDeltaToTargetRatio(float manaToUse, float aetherValue, float netherValue, float targetRatio){
        return ((netherValue + manaToUse - (targetRatio * aetherValue))/(1.0f + targetRatio));
    }

    public static float getNetherDeltaToTargetRatio(float manaToUse, float aetherValue, float netherValue, float targetRatio){
        /*!Note: Calculation should be the following, but extra corrections are needed to increase the punctuality
         * float netherDelta = ((targetRatio*(aetherValue + manaToUse)) - netherValue)/(1 + targetRatio); */
        float aetherDelta = getAetherDeltaToTargetRatio(manaToUse, aetherValue, netherValue, targetRatio);
        float newNetherValue = (aetherValue + aetherDelta) * targetRatio;
        return Math.min(manaToUse, newNetherValue - netherValue);
    }

    public static float getEqualizeAttemptAetherValue(
        float aetherValue, float netherValue,
        float aetherDelta, float netherDelta,
        float targetRatio
    ){
        /* Since Aether is the less reactive one, firstly Nether shall decide how much shall remain */
        float remainingAether = (aetherValue + aetherDelta) - ((netherValue + netherDelta)/targetRatio);
        return (aetherValue + aetherDelta - remainingAether);
    }

    public static float getEqualizeAttemptNetherValue(
        float aetherValue, float netherValue,
        float aetherDelta, float netherDelta,
        float targetRatio
    ){
        float remainingAether = (aetherValue + aetherDelta) - ((netherValue + netherDelta)/targetRatio);
        float remainingNether = (netherValue + netherDelta) - ((aetherValue + aetherDelta - remainingAether)*targetRatio);
        return (netherValue + netherDelta - remainingNether);
    }

    public static void setReleasedNether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX, Config.bufferCellSize,0, buffer, value);
    }
    public static void setAvgReleasedNether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX, Config.bufferCellSize,1, buffer, value);
    }
    public static void setReleasedAether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX, Config.bufferCellSize,2, buffer, value);
    }
    public static void setAvgReleasedAether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,sizeX, Config.bufferCellSize,3, buffer, value);
    }

    public static float getReleasedNether(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX, Config.bufferCellSize,0, buffer);
    }
    public static float getAvgReleasedNether(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX, Config.bufferCellSize,1, buffer);
    }
    public static float getReleasedAether(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX, Config.bufferCellSize,2, buffer);
    }
    public static float getAvgReleasedAether(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x,y,sizeX, Config.bufferCellSize,3, buffer);
    }

}
