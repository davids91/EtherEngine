package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;

import java.util.Arrays;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    private static final float aetherWeightInUnits = 4;
    private static final float etherReleaseThreshold = 0.1f;

    protected final int sizeX;
    protected final int sizeY;

    private float[][] aetherValues; /* Stationary substance */
    private float[][] netherValues; /* Moving substance */

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        aetherValues = new float[sizeX][sizeY];
        netherValues = new float[sizeX][sizeY];
        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
            Arrays.copyOf(aetherValues,aetherValues.length),
            Arrays.copyOf(netherValues,netherValues.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        aetherValues = (float[][])state[0];
        netherValues = (float[][])state[1];
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

    public float getMaxNether(int x, int y){
        return aetherValueAt(x,y) * Material.ratioOf(Material.Elements.Fire);
    }
    public float getMinAether(int x, int y){
        return netherValueAt(x,y) / Material.ratioOf(Material.Elements.Earth);
    }
    private void processEther() {
        float[][] releasedNether = new float[sizeX][sizeY];
        float[][] availableAvgNe = new float[sizeX][sizeY];
        float[][] releasedAether = new float[sizeX][sizeY];
        float[][] availableAvgAe = new float[sizeX][sizeY];

        for (int x = 0; x < sizeX; ++x) { /* Preprocess Ether */
            for (int y = 0; y < sizeY; ++y) {
                releasedNether[x][y] = 0;
                releasedAether[x][y] = 0;
                float currentRatio = getRatio(x,y);
                if( 0.5 < Math.abs(currentRatio - Material.ratioOf(Material.Elements.Ether)) ){
                    float aetherToRelease = (aetherValueAt(x,y) - getMinAether(x, y));
                    float netherToRelease = (netherValueAt(x,y) - getMaxNether(x, y));
                    if(
                        ( netherValueAt(x,y) >= (getMaxNether(x,y)) + (aetherValueAt(x,y) * etherReleaseThreshold) )
                        || ( aetherValueAt(x,y) >= (getMinAether(x,y) + (netherValueAt(x,y) * etherReleaseThreshold)) )
                    ){
                        if(netherToRelease >= aetherToRelease){
                            releasedNether[x][y] = netherToRelease / 9.0f;
                        }else{
                            releasedAether[x][y] = aetherToRelease / 9.0f;
                        }
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

                /* Subtract the released Ether, and add the shared */
                netherValues[x][y] -= releasedNether[x][y];
                aetherValues[x][y] -= releasedAether[x][y];
                /* TODO: The more units there is, the more ether is absorbed */
                aetherValues[x][y] += availableAvgAe[x][y] * 0.9f;// / parent.getUnits(x,y);
                netherValues[x][y] += availableAvgNe[x][y] * 0.9f;// / parent.getUnits(x,y);

                /* TODO: Surplus Nether to goes into other effects?? */
                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                /* Safety to never let values below zero */
                if (0 >= aetherValueAt(x,y)) {
                    aetherValues[x][y] = 0.01f;
                }
                if (0 >= netherValueAt(x,y)) {
                    netherValues[x][y] = 0.01f;
                }

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
                takeOverUnitChanges(x,y, units);
            }
        }
    }

    private float getUnit(int x, int y){ /* Since Aether is the stabilizer */
        return ((aetherValueAt(x,y)* aetherWeightInUnits + netherValueAt(x,y))/(aetherWeightInUnits +1));
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
        return aetherValues[x][y];
    }
    public float netherValueAt(int x, int y){
        return netherValues[x][y];
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
        aetherValues[x][y] = Math.max(0.01f, value);
    }
    public void addNetherTo(int x, int y, float value){
        setNetherTo(x,y, Math.max(0.01f, netherValueAt(x,y) + value));
    }
    public void setNetherTo(int x, int y, float value){
        netherValues[x][y] = Math.max(0.01f, value);
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
