package com.crystalline.aether.models.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.world.EtherealAspect;

import java.nio.FloatBuffer;

public class EtherealAspectStrategy extends RealityAspectStrategy{
    public static float getMaxNether(int x, int y, int sizeX, FloatBuffer buffer){
        return aetherValueAt(x,y, sizeX, buffer) * Material.ratioOf(Material.Elements.Fire);
    }

    public static float getMinAether(int x, int y, int sizeX, FloatBuffer buffer){
        return netherValueAt(x,y, sizeX, buffer) / Material.ratioOf(Material.Elements.Earth);
    }

    public static float getAetherValue(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x, y, sizeX, Config.bufferCellSize,2, buffer);
    }

    public static float getNetherValue(int x, int y, int sizeX, FloatBuffer buffer){
        return BufferUtils.get(x, y, sizeX, Config.bufferCellSize,0, buffer);
    }

    public static float aetherValueAt(int x, int y, int sizeX, FloatBuffer buffer){
        return getAetherValue(x,y, sizeX, buffer);
    }

    public static float netherValueAt(int x, int y, int sizeX, FloatBuffer buffer){
        return getNetherValue(x,y, sizeX, buffer);
    }

    public static float getRatio(int x, int y, int sizeX, FloatBuffer buffer){
        if(0 != getAetherValue(x,y, sizeX, buffer))
            return (getNetherValue(x,y, sizeX, buffer) / getAetherValue(x,y, sizeX, buffer));
        else return 0;
    }

    public static float getElement(int x, int y, int sizeX, FloatBuffer buffer){
        return getRatio(x,y, sizeX, buffer);
    }

    public static Material.Elements getElementEnum(int x, int y, int sizeX, FloatBuffer buffer){
        if(EtherealAspect.getUnit(x,y, sizeX, buffer) <= Material.ratioOf(Material.Elements.Fire)) return Material.Elements.Air;
        else if(0.05f > Math.abs(getRatio(x,y,sizeX, buffer) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether; /*!Note: Setting the thresholds here will increase the chance of flickering crystals/vapor! */
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y,sizeX, buffer) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }

    public static void setAether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, sizeX, Config.bufferCellSize,2, buffer, value);
    }

    public static void setNether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        BufferUtils.set(x, y, sizeX, Config.bufferCellSize,0, buffer, value);
    }

    private static void addNether(int x, int y, int sizeX, FloatBuffer buffer, float value){
        setNether(x,y, sizeX, buffer, Math.max(0.01f, netherValueAt(x,y, sizeX, buffer) + value));
    }

    public static void tryToEqualize(int x, int y, int sizeX, FloatBuffer buffer, float aetherDelta, float netherDelta, float ratio){
        setAether(x,y, sizeX, buffer, getEqualizeAttemptAetherValue(getAetherValue(x,y, sizeX, buffer),getNetherValue(x,y, sizeX, buffer),aetherDelta,netherDelta,ratio));
        setNether(x,y, sizeX, buffer, getEqualizeAttemptNetherValue(getAetherValue(x,y, sizeX, buffer),getNetherValue(x,y, sizeX, buffer),aetherDelta,netherDelta,ratio));
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
