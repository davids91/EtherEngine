package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.services.utils.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    private static final float aetherWeightInUnits = 4;
    private static final float etherReleaseThreshold = 0.1f;
    private static final int bufferCellSize = 4; /* RGBA */

    protected final int sizeX;
    protected final int sizeY;

    /**
     * A texture image representing the ethereal values in the plane
     * - R: Moving substance
     * - G: Unsued yet...
     * - B: Stationary substance
     * - A: Unsued
     */
    private FloatBuffer etherValues;

    /**
     * A texture representing some intermediate values in the ethereal plane
     * - R: Released Nether
     * - G: Average Released Nether in context
     * - B: Released Aether
     * - A: Released Aether in context
     */
    private final FloatBuffer sharingBuffer;

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        etherValues = ByteBuffer.allocateDirect(Float.BYTES * bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        sharingBuffer = ByteBuffer.allocateDirect(Float.BYTES * bufferCellSize * sizeX * sizeY).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
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
                setAetherTo(x,y, 1);
                setNetherTo(x,y, Material.ratioOf(Material.Elements.Air));
            }
        }
    }

    public void defineBy(ElementalAspect plane, float[][] units){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < units[x][y]) {
                    setAetherTo(x,y, (((2.0f * units[x][y]) / (1.0f + Material.ratioOf(plane.elementAt(x, y))))));
                    setNetherTo(x,y, ( aetherValueAt(x,y)* Material.ratioOf(plane.elementAt(x, y))));
                }else{
                    setAetherTo(x,y,1);
                    setNetherTo(x,y, Material.ratioOf(Material.Elements.Air));
                }
            }
        }
    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        float tmpVal = aetherValueAt(toX,toY);
        setAetherTo(toX, toY, aetherValueAt(fromX,fromY));
        setAetherTo(fromX, fromY, tmpVal);

        tmpVal = netherValueAt(toX,toY);
        setNetherTo(toX, toY, netherValueAt(fromX,fromY));
        setNetherTo(fromX, fromY, tmpVal);
    }

    private float avgOf(int x, int y, int cellOffset, FloatBuffer table){
        float ret = 0.0f;
        float divisor = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                ret += BufferUtils.get(nx,ny, sizeX, bufferCellSize,cellOffset,table);
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
    private void processEther() {

        for (int x = 0; x < sizeX; ++x) { /* Preprocess Ether */
            for (int y = 0; y < sizeY; ++y) {
                BufferUtils.set(x,y,sizeX, bufferCellSize,0, sharingBuffer,0);
                BufferUtils.set(x,y,sizeX, bufferCellSize,2, sharingBuffer,0);
                float currentRatio = getRatio(x,y);
                if( 0.5 < Math.abs(currentRatio - Material.ratioOf(Material.Elements.Ether)) ){
                    float aetherToRelease = (aetherValueAt(x,y) - getMinAether(x, y));
                    float netherToRelease = (netherValueAt(x,y) - getMaxNether(x, y));
                    if(
                        ( netherValueAt(x,y) >= (getMaxNether(x,y)) + (aetherValueAt(x,y) * etherReleaseThreshold) )
                        || ( aetherValueAt(x,y) >= (getMinAether(x,y) + (netherValueAt(x,y) * etherReleaseThreshold)) )
                    ){
                        if(netherToRelease >= aetherToRelease){
                            BufferUtils.set(x,y, sizeX, bufferCellSize,0, sharingBuffer,netherToRelease / 9.0f);
                        }else{
                            BufferUtils.set(x,y, sizeX, bufferCellSize,2, sharingBuffer,aetherToRelease / 9.0f);
                        }
                    }
                }
            }
        }

        for (int x = 0; x < sizeX; ++x) { /* Sharing released ether */
            for (int y = 0; y < sizeY; ++y) {
                BufferUtils.set(x,y, sizeX, bufferCellSize,3, sharingBuffer,avgOf(x, y, 2,sharingBuffer));
                BufferUtils.set(x,y, sizeX, bufferCellSize,1, sharingBuffer,avgOf(x, y, 0,sharingBuffer));
            }
        }

        for (int x = 0; x < sizeX; ++x) { /* finalizing Ether */
            for (int y = 0; y < sizeY; ++y) {

                /* Subtract the released Ether, and add the shared */
                /* TODO: The more units there is, the more ether is absorbed */
                float newAetherValue = Math.max( 0.01f, /* Update values with safety cut */
                    aetherValueAt(x,y) - BufferUtils.get(x,y,sizeX, bufferCellSize,2,sharingBuffer)
                    + (BufferUtils.get(x,y,sizeX, bufferCellSize,3, sharingBuffer) * 0.9f)// / parent.getUnits(x,y));
                );
                float newNetherValue = Math.max( 0.01f,
                    netherValueAt(x,y) - BufferUtils.get(x,y,sizeX, bufferCellSize,0,sharingBuffer)
                    + (BufferUtils.get(x,y,sizeX, bufferCellSize,1, sharingBuffer) * 0.9f)// / parent.getUnits(x,y));
                );

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                setAetherTo(x, y, newAetherValue);
                setNetherTo(x, y, newNetherValue);
            }
        }
    }

    @Override
    public void processUnits(float[][] units, World parent){
        processEther();
        determineUnits(units,parent);
    }

    @Override
    public void processTypes(float[][] units, World parent){
        /* Take over unit changes from Elemental plane */
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                takeOverUnitChanges(x, y, units);
            }
        }
    }

    private float getUnit(int x, int y){ /* Since Aether is the stabilizer */
        return (
            (aetherValueAt(x,y)* aetherWeightInUnits + netherValueAt(x,y)) /(aetherWeightInUnits +1)
        );
    }

    @Override
    public void determineUnits(float[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = getUnit(x,y);
            }
        }
    }

    @Override
    public void takeOverUnitChanges(int x, int y, float[][] units) {
        float oldRatio = getRatio(x,y);
        float oldUnit = getUnit(x,y);
        setAetherTo(x,y, (
            ((aetherValueAt(x,y)* aetherWeightInUnits + netherValueAt(x,y)) * units[x][y])
            / (oldUnit* aetherWeightInUnits + oldUnit * oldRatio)
        ));
        setNetherTo(x,y, (aetherValueAt(x,y) * oldRatio));
    }

    @Override
    public void processMechanics(float[][] units, World parent) {

    }

    @Override
    public void postProcess(float[][] units, World parent) {
        /* TODO: Decide para-modifiers ( e.g. heat, light?! ) */
        /* TODO: Increase heat where there is a surplus Nether */
    }

    public float aetherValueAt(int x, int y){
        return BufferUtils.get(x, y, sizeX, bufferCellSize,2, etherValues);
    }
    public float netherValueAt(int x, int y){
        return BufferUtils.get(x, y, sizeX, bufferCellSize, 0, etherValues);
    }

    public float getRatio(int x, int y){
        if(0 != aetherValueAt(x,y))
            return (netherValueAt(x,y) / aetherValueAt(x,y));
        else return 0;
    }

    public Material.Elements elementAt(int x, int y){
        if(getUnit(x,y) <= Material.ratioOf(Material.Elements.Fire)) return Material.Elements.Air; /* TODO: Recheck so extra AENE are not counted, maybe? */
        else if(0 == Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }

    public void addAetherTo(int x, int y, float value){
        setAetherTo(x,y, Math.max(0.01f, aetherValueAt(x,y) + value));
    }
    public void setAetherTo(int x, int y, float value){
        BufferUtils.set(x, y, sizeX, bufferCellSize,2, etherValues,Math.max(0.01f, value));
    }
    public void addNetherTo(int x, int y, float value){
        setNetherTo(x,y, Math.max(0.01f, netherValueAt(x,y) + value));
    }
    public void setNetherTo(int x, int y, float value){
        BufferUtils.set(x, y, sizeX, bufferCellSize,0, etherValues,Math.max(0.01f, value));
    }


    public void tryToEqualize(int x, int y, float aetherDelta, float netherDelta, float ratio){
        setAetherTo(x,y, getEqualizeAttemptAetherValue(aetherValueAt(x,y),netherValueAt(x,y),aetherDelta,netherDelta,ratio));
        setNetherTo(x,y, getEqualizeAttemptNetherValue(aetherValueAt(x,y),netherValueAt(x,y),aetherDelta,netherDelta,ratio));
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

}
