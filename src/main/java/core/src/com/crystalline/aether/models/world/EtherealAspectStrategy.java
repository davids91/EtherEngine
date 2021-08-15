package com.crystalline.aether.models.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;

import java.nio.FloatBuffer;

public class EtherealAspectStrategy extends RealityAspectStrategy{
    public static final float aetherWeightInUnits = 4;
    public static final float etherReleaseThreshold = 0.1f;

    private final int chunkSize;
    public EtherealAspectStrategy(int chunkSize_){
        chunkSize = chunkSize_;
    }

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
        if(EtherealAspectStrategy.getUnit(x,y, chunkSize, buffer) <= Material.ratioOf(Material.Elements.Fire)) return Material.Elements.Air;
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

    private static void addNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
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

    public static void setReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,0, buffer, value);
    }

    public static void setAvgReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,1, buffer, value);
    }

    public static void setReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,2, buffer, value);
    }

    public static void setAvgReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,3, buffer, value);
    }

    public static float getReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, buffer);
    }

    public static float getAvgReleasedNether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,1, buffer);
    }

    public static float getReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,2, buffer);
    }

    public static float getAvgReleasedAether(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,3, buffer);
    }

    public static float getUnit(int x, int y,  int chunkBlockSize, FloatBuffer buffer){
        return ( /* Since Aether is the stabilizer, it shall weigh more */
                (EtherealAspectStrategy.getAetherValue(x,y, chunkBlockSize, buffer)* aetherWeightInUnits + EtherealAspectStrategy.getNetherValue(x,y, chunkBlockSize, buffer)) /(aetherWeightInUnits+1)
        );
    }
}
