package com.crystalline.aether.models.world;

import com.badlogic.gdx.Gdx;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.computation.Includer;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.StringUtils;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;

public class EtherealAspectStrategy extends RealityAspectStrategy{
    public static final float aetherWeightInUnits = 4;
    public static final float etherReleaseThreshold = 0.1f;

    public final int chunkSize;
    public EtherealAspectStrategy(int chunkSize_){
        chunkSize = chunkSize_;
    }

    protected static String buildKernel(String rawKernelCode, Includer includer){
        return includer.process(rawKernelCode);
    }

    public static final String defineByElementalPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/ethDefineByElementalPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Provides the Ethereal plane adapted to fit into the given elemental plane
     * @param inputs [0]: elements; [1]: scalars
     * @param output etherValues buffer
     */
    public void defineByElementalPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float currentUnits = World.getUnit(x,y, chunkSize, inputs[1]);
            Material.Elements currentElement = ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]);
            float newAether = ((2.0f * currentUnits) / (1.0f + Material.ratioOf(currentElement)));
            if(0 < currentUnits) {
                setAether(x,y, chunkSize, output, newAether);
                setNether(x,y, chunkSize, output, ( newAether * Material.ratioOf(currentElement) ));
            }else{
                setAether(x,y, chunkSize, output,1);
                setNether(x,y, chunkSize, output, Material.ratioOf(Material.Elements.Air));
            }
        } }
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes;[0]: scalars
     * @param output etherValues buffer
     */
    public void switchEtherPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float aetherValue = getAetherValue(x, y, chunkSize, inputs[1]);
            float netherValue = getNetherValue(x, y, chunkSize, inputs[1]);
            if(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0])){
                int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
                int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]);
                if(
                        (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                                &&(0 < toApply)
                                &&(targetX >= 0)&&(targetX < chunkSize)
                                &&(targetY >= 0)&&(targetY < chunkSize)
                ){
                    aetherValue = getAetherValue(targetX, targetY, chunkSize, inputs[1]);
                    netherValue = getNetherValue(targetX, targetY, chunkSize, inputs[1]);
                }
            }
            setAether(x,y, chunkSize, output, aetherValue);
            setNether(x,y, chunkSize, output, netherValue);
        }}
    }

    /**
     * Calculates the released Ether values based on the current levels
     * @param inputs [0]: Ether values
     * @param output The released Ether values: {R: released Nether; G: - ; B: released Nether}
     */
    public void preProcessCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < chunkSize; ++x) { /* Preprocess Ether */
            for (int y = 0; y < chunkSize; ++y) {
                setReleasedNether(x,y,chunkSize,output,0);
                setAvgReleasedNether(x,y,chunkSize,output,0);
                setReleasedAether(x,y,chunkSize,output,0);
                setAvgReleasedAether(x,y,chunkSize,output,0);
                float currentRatio = getRatio(x,y, chunkSize, inputs[0]);
                if( 0.5 < Math.abs(currentRatio - Material.ratioOf(Material.Elements.Ether)) ){
                    float aetherToRelease = (aetherValueAt(x,y, chunkSize, inputs[0]) - getMinAether(x,y, chunkSize, inputs[0]));
                    float netherToRelease = (netherValueAt(x,y, chunkSize, inputs[0]) - getMaxNether(x,y, chunkSize, inputs[0]));
                    if(
                        ( netherValueAt(x,y, chunkSize, inputs[0]) >= (getMaxNether(x,y, chunkSize, inputs[0])) + (aetherValueAt(x,y, chunkSize, inputs[0]) * etherReleaseThreshold) )
                        || ( aetherValueAt(x,y, chunkSize, inputs[0]) >= (getMinAether(x,y, chunkSize, inputs[0]) + (netherValueAt(x,y,chunkSize, inputs[0]) * etherReleaseThreshold)) )
                    ){
                        if(netherToRelease >= aetherToRelease){
                            setReleasedNether(x,y, chunkSize, output,netherToRelease / 9.0f);
                        }else{
                            setReleasedAether(x,y, chunkSize, output, aetherToRelease / 9.0f);
                        }
                    }
                }
            }
        }
    }

    private float avgOf(int x, int y, int cellOffset, FloatBuffer table){
        float ret = 0.0f;
        float divisor = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(chunkSize, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(chunkSize, y + 2); ++ny) {
                ret += BufferUtils.get(nx,ny, chunkSize, Config.bufferCellSize,cellOffset,table);
                ++divisor;
            }
        }
        return (ret / divisor);
    }

    /**
     * Calculates the shared Ether values through contextual filters ( average )
     * @param inputs [0]: The Released ether from preprocess phase
     * @param output the average values of shared Ether
     */
    public void sharingCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < chunkSize; ++x) { /* Sharing released ether */
            for (int y = 0; y < chunkSize; ++y) { /* calculate shared ether from released ether */
                setAvgReleasedAether(x,y, chunkSize, output, avgOf(x, y, 2,inputs[0]));
                setAvgReleasedNether(x,y, chunkSize, output, avgOf(x, y, 0,inputs[0]));
            }
        }
    }

    /**
     * Calculates the final Ether based on the released and average calculations
     * @param inputs [0]: released Ether; [1]: average released Ether; [2]: previous ether values;
     * @param output processed Ether values
     */
    public void finalizeCalculationPhase(FloatBuffer[] inputs, FloatBuffer output){
        for (int x = 0; x < chunkSize; ++x) { /* finalizing Ether */
            for (int y = 0; y < chunkSize; ++y) {
                /* Subtract the released Ether, and add the shared */
                /* TODO: The more units there is, the more ether is absorbed */
                float newAetherValue = Math.max( 0.01f, /* Update values with safety cut */
                    aetherValueAt(x,y, chunkSize, inputs[2]) - getReleasedAether(x,y, chunkSize, inputs[0])
                    + (getAvgReleasedAether(x,y, chunkSize, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );
                float newNetherValue = Math.max( 0.01f,
                    netherValueAt(x,y, chunkSize, inputs[2]) - getReleasedNether(x,y, chunkSize, inputs[0])
                    + (getAvgReleasedNether(x,y, chunkSize, inputs[1]) * 0.9f)// / parent.getUnits(x,y));
                );

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                setAether(x,y, chunkSize, output, newAetherValue);
                setNether(x,y, chunkSize, output, newNetherValue);
            }
        }
    }

    /**
     * Provides a refined step of the ethereal aspect buffer after processing the ratio differences
     * @param inputs [0]: etherValues; [1]: elements; [2]: scalars
     * @param output etherValues
     */
    public void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < chunkSize; ++x){ /* Take over unit changes from Elemental plane */
            for(int y = 0; y < chunkSize; ++y){
                float oldRatio = getRatio(x,y, chunkSize, inputs[0]);
                float oldUnit = getUnit(x,y, chunkSize, inputs[0]);
                Material.Elements oldElement = getElementEnum(x,y,chunkSize,inputs[0]);
//                Material.Elements oldElement = ElementalAspect.getElementEnum(x,y,chunkSize,inputs[1]);
                float newAetherValue = (
                    (
                        aetherValueAt(x,y, chunkSize, inputs[0])* aetherWeightInUnits + netherValueAt(x,y, chunkSize, inputs[0]))
                        * World.getUnit(x,y,chunkSize,inputs[2])
                    ) / (oldUnit * aetherWeightInUnits + oldUnit * oldRatio
                );
                setAether(x,y, chunkSize, output, newAetherValue);
                setNether(x,y, chunkSize, output, (newAetherValue * oldRatio));
            }
        }
    }

    public static final String determineUnitsPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/ethDetermineUnitsPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Calculates the scalar units for the world based on the Ethereal values
     * @param inputs [0]: etherValues
     * @param output new scalars for the world to take over
     */
    public void determineUnitsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < chunkSize; ++x){
            for(int y = 0; y < chunkSize; ++y){
                World.setUnit(x,y,chunkSize,output,getUnit(x,y, chunkSize, inputs[0]));
            }
        }
    }

    /* Buffer Structures
     */
    /** etherValues:
     * A texture image representing the ethereal values in the plane
     * - R: Moving substance
     * - G: -
     * - B: Stationary substance
     */
    public static float getMaxNether(int x, int y, int chunkSize, FloatBuffer buffer){
        return aetherValueAt(x,y, chunkSize, buffer) * Material.ratioOf(Material.Elements.Fire);
    }

    public static float getMinAether(int x, int y, int chunkSize, FloatBuffer buffer){
        return netherValueAt(x,y, chunkSize, buffer) / Material.ratioOf(Material.Elements.Earth);
    }

    public static float getAetherValue(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x, y, chunkSize, Config.bufferCellSize,2, buffer);
    }

    public static float getNetherValue(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x, y, chunkSize, Config.bufferCellSize,0, buffer);
    }

    public static float aetherValueAt(int x, int y, int chunkSize, FloatBuffer buffer){
        return getAetherValue(x,y, chunkSize, buffer);
    }

    public static float netherValueAt(int x, int y, int chunkSize, FloatBuffer buffer){
        return getNetherValue(x,y, chunkSize, buffer);
    }

    public static float getRatio(int x, int y, int chunkSize, FloatBuffer buffer){
        if(0 != getAetherValue(x,y, chunkSize, buffer))
            return (getNetherValue(x,y, chunkSize, buffer) / getAetherValue(x,y, chunkSize, buffer));
        else return 0;
    }

    public static float getElement(int x, int y, int chunkSize, FloatBuffer buffer){
        return getRatio(x,y, chunkSize, buffer);
    }

    public static Material.Elements getElementEnum(int x, int y, int chunkSize, FloatBuffer buffer){
        if(getUnit(x,y, chunkSize, buffer) <= Material.ratioOf(Material.Elements.Fire)) return Material.Elements.Air;
        else if(0.05f > Math.abs(getRatio(x,y,chunkSize, buffer) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether; /*!Note: Setting the thresholds here will increase the chance of flickering crystals/vapor! */
        else if(getRatio(x,y,chunkSize, buffer) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y,chunkSize, buffer) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y,chunkSize, buffer) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }

    public static void setAether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, chunkSize, Config.bufferCellSize,2, buffer, value);
    }

    public static void setNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x, y, chunkSize, Config.bufferCellSize,0, buffer, value);
    }

    public static void addNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        setNether(x,y, chunkSize, buffer, Math.max(0.01f, netherValueAt(x,y, chunkSize, buffer) + value));
    }

    public static void tryToEqualize(int x, int y, int chunkSize, FloatBuffer buffer, float aetherDelta, float netherDelta, float ratio){
        setAether(x,y, chunkSize, buffer, getEqualizeAttemptAetherValue(getAetherValue(x,y, chunkSize, buffer),getNetherValue(x,y, chunkSize, buffer),aetherDelta,netherDelta,ratio));
        setNether(x,y, chunkSize, buffer, getEqualizeAttemptNetherValue(getAetherValue(x,y, chunkSize, buffer),getNetherValue(x,y, chunkSize, buffer),aetherDelta,netherDelta,ratio));
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

    /** etherValues_tmp - Released Ether
     * Inside the backend there is also another buffer structure used for sharing Ether in-between cells,
     * which is not declared explicitly as it is part of the backend, and never used outside the calculation phases
     * - R: Released Nether
     * - G: -
     * - B: Released Aether
     * */
    public static void setReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,2, buffer, value);
    }
    public static float getReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,2, buffer);
    }
    public static void setReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,0, buffer, value);
    }
    public static float getReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, buffer);
    }

    /** etherValues_tmp - Average Ether
     * Inside the backend there is also another buffer structure used for sharing Ether in-between cells,
     * which is not declared explicitly as it is part of the backend, and never used outside the calculation phases
     * - R: Avg Nether
     * - G: -
     * - B: Avg Aether
     * */
    public static void setAvgReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,2, buffer, value);
    }
    public static void setAvgReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,0, buffer, value);
    }
    public static float getAvgReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, buffer);
    }
    public static float getAvgReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,2, buffer);
    }

    public static float getUnit(int x, int y,  int chunkBlockSize, FloatBuffer etherValues){
        return ( /* Since Aether is the stabilizer, it shall weigh more */
            (
                getAetherValue(x,y, chunkBlockSize, etherValues) * aetherWeightInUnits
                + getNetherValue(x,y, chunkBlockSize, etherValues)
            ) / (aetherWeightInUnits+1)
        );
    }
}
