package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;

import java.util.Arrays;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    protected final int sizeX;
    protected final int sizeY;

    private int [][] releaseTick;
    private int[][] aetherValues; /* Stationary substance */
    private int[][] netherValues; /* Moving substance */

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        aetherValues = new int[sizeX][sizeY];
        netherValues = new int[sizeX][sizeY];
        releaseTick = new int[sizeX][sizeY];
        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
            Arrays.copyOf(aetherValues,aetherValues.length),
            Arrays.copyOf(netherValues,netherValues.length),
            Arrays.copyOf(releaseTick, releaseTick.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        aetherValues = (int[][])state[0];
        netherValues = (int[][])state[1];
        releaseTick = (int[][])state[2];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                aetherValues[x][y] = 1;
                netherValues[x][y] = Material.ratioOf(Material.Elements.Air);
            }
        }
    }

    public void defineBy(ElementalAspect plane, int[][] units){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < units[x][y]) {
                    aetherValues[x][y] = (int)Math.ceil(((2.0f * units[x][y]) / (1.0f + (float)Material.ratioOf(plane.elementAt(x, y)))));
                    netherValues[x][y] = aetherValues[x][y] * Material.ratioOf(plane.elementAt(x, y));
                }else{
                    aetherValues[x][y] = 1;
                    netherValues[x][y] = Material.ratioOf(Material.Elements.Air);
                }
            }
        }
    }

    private float getEtherDelta(float requested, float available){
        if(0 < requested) return Math.min(requested, available);
        else return Math.max(requested,-available);
    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        int tmp_val = aetherValues[toX][toY];
        aetherValues[toX][toY] = aetherValues[fromX][fromY];
        aetherValues[fromX][fromY] = tmp_val;

        tmp_val = netherValues[toX][toY];
        netherValues[toX][toY] = netherValues[fromX][fromY];
        netherValues[fromX][fromY] = tmp_val;
    }

    private float avgOfPositive(int x, int y, float[][] table){
        float ret = 0.0f;
        float divisor = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                if(0 < table[nx][ny]){
                    ret += table[nx][ny];
                    ++divisor;
                }
            }
        }
        return (ret / Math.max(0.001f,divisor));
    }

    private float avgOf(int x, int y, float[][] table){
        float ret = 0.0f;
        float divisor = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                ret += table[nx][ny];
                ++divisor;
            }
        }
        return (ret / divisor);
    }

    private static final int ticksToChange = 0;
    private static final int ticksToRelease = 5;
    private static final float etherReleaseThreshold = 0.1f;

    public float getMaxNether(int x, int y){
        return aetherValueAt(x,y) * Material.ratioOf(Material.Elements.Fire);
    }

    public float getMinAether(int x, int y){
        return (int)(netherValueAt(x,y) / (float)Material.ratioOf(Material.Elements.Earth));
    }

    private void processEther(World parent) {
        float[][] releasedNether = new float[sizeX][sizeY];
        float[][] availableAvgNe = new float[sizeX][sizeY];
        float[][] releasedAether = new float[sizeX][sizeY];
        float[][] availableAvgAe = new float[sizeX][sizeY];

        for (int x = 0; x < sizeX; ++x) { /* Preprocess Ether */
            for (int y = 0; y < sizeY; ++y) {
                releasedNether[x][y] = 0;
                releasedAether[x][y] = 0;

                if(1.0 < Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether))){
                    if( netherValues[x][y] >= (getMaxNether(x,y)) + (aetherValues[x][y] * etherReleaseThreshold) ){
                        if (0 == releaseTick[x][y]) {
                            releasedNether[x][y] = (netherValues[x][y] - getMaxNether(x, y)) / 9.f;
                            netherValues[x][y] -= releasedNether[x][y];
                            releaseTick[x][y] = ticksToRelease; /* TODO: Tics to release to depend on Nether */
                        } else --releaseTick[x][y];
                    }

                    /* Only release Aether if there is more, than currently needed */
                    if(aetherValues[x][y] >= (getMinAether(x,y) - (netherValues[x][y] * etherReleaseThreshold))){
                        if (0 == releaseTick[x][y]) {
                            releasedAether[x][y] = (aetherValues[x][y] - getMinAether(x, y))/9.0f;
                            aetherValues[x][y] -= releasedAether[x][y];
                            releaseTick[x][y] = ticksToRelease; /* TODO: Tics to release to depend on Nether */
                        } else --releaseTick[x][y];
                    }
                }
            }
        }

        for (int x = 0; x < sizeX; ++x) { /* Sharing released ether */
            for (int y = 0; y < sizeY; ++y) {
                availableAvgAe[x][y] = avgOf(x, y, releasedAether);
                availableAvgNe[x][y] = avgOf(x, y, releasedNether);
            }
        }

        for (int x = 0; x < sizeX; ++x) { /* finalizing Ether */
            for (int y = 0; y < sizeY; ++y) {

                /* The more units there is, the more ether is absorbed */
                aetherValues[x][y] += availableAvgAe[x][y];// / parent.getUnits(x,y);
                netherValues[x][y] += availableAvgNe[x][y];// / parent.getUnits(x,y);

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                /* Safety to never let values below zero */
                if (0 >= aetherValues[x][y]) {
                    aetherValues[x][y] = 1;
                }
                if (0 >= netherValues[x][y]) {
                    netherValues[x][y] = 1;
                }

            }
        }
    }

    @Override
    public void processUnits(int[][] units, World parent){
        processEther(parent);
        determineUnits(units,parent);
    }

    @Override
    public void processTypes(int[][] units, World parent){
        /* Take over unit changes from Elemental plane */
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                takeOverUnitChanges(x,y, units);
            }
        }
    }

    private float getUnit(int x, int y){
        return ((aetherValueAt(x,y) + netherValueAt(x,y))/2.0f);
    }

    @Override
    public void determineUnits(int[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = (int)getUnit(x,y);
            }
        }
    }

    @Override
    public void takeOverUnitChanges(int x, int y, int[][] units) {
        float oldRatio = getRatio(x,y);
        float oldUnit = getUnit(x,y);
        setAetherTo(x,y, (int)Math.ceil(
            (aetherValues[x][y] + netherValues[x][y]) * units[x][y]
            / (oldUnit + oldUnit * oldRatio)
        ));
        setNetherTo(x,y, (int)(aetherValues[x][y] * oldRatio));
    }

    @Override
    public void processMechanics(int[][] units, World parent) {

    }

    @Override
    public void postProcess(int[][] units, World parent) {
        /* TODO: Decide para-modifiers ( e.g. heat ) */
        /* TODO: Increase heat where there is a surplus Nether */
        /* Take over unit changes from Elemental plane */
    }

    public int aetherValueAt(int x, int y){
        return aetherValues[x][y];
    }
    public int netherValueAt(int x, int y){
        return netherValues[x][y];
    }

    public float getRatio(int x, int y){
        if(0 != aetherValues[x][y])
            return ((float)netherValues[x][y]/ aetherValues[x][y]);
        else return 0;
    }

    public Material.Elements elementAt(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 1) return Material.Elements.Air;
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

    public Material.Elements getTargetRatio(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 1) return Material.Elements.Air;
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

    public void addAetherTo(int x, int y, int value){
        aetherValues[x][y] = Math.max(1, aetherValues[x][y]+value);
    }
    public void setAetherTo(int x, int y, int value){
        aetherValues[x][y] = Math.max(1, value);
    }
    public void addNetherTo(int x, int y, int value){
        netherValues[x][y] = Math.max(1, netherValues[x][y]+value);
    }
    public void setNetherTo(int x, int y, int value){
        netherValues[x][y] = Math.max(1, value);
    }


    public void tryToEqualize(int x, int y, int aetherDelta, int netherDelta, int ratio){
        aetherValues[x][y] = getEqualizeAttemptAetherValue(aetherValues[x][y],netherValues[x][y],aetherDelta,netherDelta,ratio);
        netherValues[x][y] = getEqualizeAttemptNetherValue(aetherValues[x][y],netherValues[x][y],aetherDelta,netherDelta,ratio);
        /* TODO: the remainder should be radiated into pra-effects */
    }

    public static int getAetherDeltaToTargetRatio(int manaToUse, int aetherValue, int netherValue, int targetRatio){
        return (int)((netherValue + manaToUse - (targetRatio * aetherValue))/(1.0f + targetRatio));
    }

    public static int getNetherDeltaToTargetRatio(int manaToUse, int aetherValue, int netherValue, int targetRatio){
        /*!Note: Calculation should be the following, but extra corrections are needed to increase the punctuality
         * float netherDelta = ((targetRatio*(aetherValue + manaToUse)) - netherValue)/(1 + targetRatio); */
        float aetherDelta = getAetherDeltaToTargetRatio(manaToUse, aetherValue, netherValue, targetRatio);
        float newNetherValue = (aetherValue + aetherDelta) * targetRatio;
        return (int)Math.min(manaToUse, newNetherValue - netherValue);
    }

    public static int getEqualizeAttemptAetherValue(
        int aetherValue, int netherValue,
        int aetherDelta, int netherDelta,
        int targetRatio
    ){
        /* Since Aether is the less reactive one, firstly Nether shall decide how much shall remain */
        float remainingAether = (aetherValue + aetherDelta) - (((float)netherValue + netherDelta)/targetRatio);
        return ((int)(aetherValue + aetherDelta - remainingAether));
    }

    public static int getEqualizeAttemptNetherValue(
        int aetherValue,int netherValue,
        int aetherDelta,int netherDelta,
        int targetRatio
    ){
        float remainingAether = (aetherValue + aetherDelta) - (((float)netherValue + netherDelta)/targetRatio);
        float remainingNether = (netherValue + netherDelta) - (((float)aetherValue + aetherDelta - remainingAether)*targetRatio);
        return ((int)(netherValue + netherDelta - remainingNether));
    }

}
