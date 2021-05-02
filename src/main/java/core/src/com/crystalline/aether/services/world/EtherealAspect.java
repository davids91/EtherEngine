package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;

import java.util.Arrays;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    protected final int sizeX;
    protected final int sizeY;

    private int [][] ratio_change_tick;
    private int[][] aetherValues; /* Stationary substance */
    private int[][] netherValues; /* Moving substance */
    private int[][] targetRatios;

    private static final int ticks_to_change = 0;
    private static final float nether_dynamic = 0.9f;

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        aetherValues = new int[sizeX][sizeY];
        netherValues = new int[sizeX][sizeY];
        targetRatios = new int[sizeX][sizeY];
        ratio_change_tick = new int[sizeX][sizeY];
        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
                Arrays.copyOf(aetherValues,aetherValues.length),
                Arrays.copyOf(netherValues,netherValues.length),
                Arrays.copyOf(targetRatios,targetRatios.length),
                Arrays.copyOf(ratio_change_tick,ratio_change_tick.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        aetherValues = (int[][])state[0];
        netherValues = (int[][])state[1];
        targetRatios = (int[][])state[2];
        ratio_change_tick = (int[][])state[3];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                aetherValues[x][y] = 1;
                targetRatios[x][y] = Material.ratioOf(Material.Elements.Air);
                netherValues[x][y] = Material.ratioOf(Material.Elements.Air);
                ratio_change_tick[x][y] = 0;
            }
        }
    }

    public void defineBy(ElementalAspect plane, int[][] units){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < units[x][y]) {
                    netherValues[x][y] = units[x][y];
                    aetherValues[x][y] = (int)(netherValues[x][y] / (float)Material.ratioOf(plane.elementAt(x, y)));
                }else{
                    netherValues[x][y] = 0;
                    aetherValues[x][y] = 0;
                }
                targetRatios[x][y] = getTargetRatio(x,y);
            }
        }
    }

    private float getEtherStep(float requested, float available){
        if(0 < requested) return Math.min(requested, available);
        else return requested;
    }

    public void setTargetRatio(int x, int y, int target){
        targetRatios[x][y] = target;
    }

    @Override
    public void switchValues(int fromX, int fromY, int toX, int toY) {
        int tmp_val = aetherValues[toX][toY];
        aetherValues[toX][toY] = aetherValues[fromX][fromY];
        aetherValues[fromX][fromY] = tmp_val;

        tmp_val = netherValues[toX][toY];
        netherValues[toX][toY] = netherValues[fromX][fromY];
        netherValues[fromX][fromY] = tmp_val;

        tmp_val = targetRatios[toX][toY];
        targetRatios[toX][toY] = targetRatios[fromX][fromY];
        targetRatios[fromX][fromY] = tmp_val;
    }

    private float avgOf(int x, int y, float[][] table){
        float ret = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                ret += table[x][y];
            }
        }
        return (ret / 9.0f);
    }

    private void process_ether(World parent, boolean decide_target_ratios) {
        float[][] requestedNether = new float[sizeX][sizeY];
        float[][] availableNether = new float[sizeX][sizeY];
        float[][] availableAvgNe = new float[sizeX][sizeY];

        float[][] requestedAether = new float[sizeX][sizeY];
        float[][] availableAether = new float[sizeX][sizeY];
        float[][] availableAvgAe = new float[sizeX][sizeY];

        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {

                if (decide_target_ratios){
                    if(0 == ratio_change_tick[x][y]){
                        targetRatios[x][y] = getTargetRatio(x, y);
                        ratio_change_tick[x][y] = ticks_to_change;
                    }else --ratio_change_tick[x][y];
                }

                /* calculate the values ether is converging to */
                requestedAether[x][y] = getTargetAether(x, y) - aetherValues[x][y];
                requestedNether[x][y] = getTargetNether(x, y) - netherValues[x][y];
                availableAether[x][y] -= requestedAether[x][y];
                availableNether[x][y] -= requestedNether[x][y];
            }
        }

        /* Sharing ether */
        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
//                availableAvgAe[x][y] = parent.avgOfCompatible(x, y, availableAether);
//                availableAvgNe[x][y] = parent.avgOfCompatible(x, y, availableNether);
                availableAvgAe[x][y] = avgOf(x, y, availableAether);
                availableAvgNe[x][y] = avgOf(x, y, availableNether);
            }
        }

        /* finalize Ether */
        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                availableAether[x][y] = (availableAvgAe[x][y] + requestedAether[x][y]);
                availableNether[x][y] = (availableAvgNe[x][y] + requestedNether[x][y]);
//                availableAether[x][y] = (availableAether[x][y] + requestedAether[x][y]);
//                availableNether[x][y] = (availableNether[x][y] + requestedNether[x][y]);

                /* step in the direction of the target ratio */
                float tmpEther = getEtherStep(requestedAether[x][y], availableAether[x][y]);
                aetherValues[x][y] += tmpEther;
                availableAether[x][y] -= tmpEther;
                tmpEther = getEtherStep(requestedNether[x][y], availableNether[x][y]);
                netherValues[x][y] += tmpEther;
                availableNether[x][y] -= tmpEther;

                /* Equalize available polarity values to 0 */
                aetherValues[x][y] += availableAether[x][y];
                netherValues[x][y] += availableNether[x][y];

                /* Surplus Nether to goes into other effects */
                if(netherValues[x][y] > aetherValues[x][y] * targetRatios[x][y]) {
                    parent.getElementalPlane().getForce(x,y).scl( /* Surplus Nether enhances movement */
                        (netherValues[x][y] / (float)Math.max(1,(aetherValues[x][y] * targetRatios[x][y])))
                    );
                    netherValues[x][y] -= 0.1f * (netherValues[x][y] - (aetherValues[x][y] * (float)targetRatios[x][y]));
                }

                if(aetherValues[x][y] > netherValues[x][y] / targetRatios[x][y]) {
                    parent.getElementalPlane().getForce(x,y).scl( /* Surplus Aether depresses movement */
                        ((netherValues[x][y] / (float)targetRatios[x][y]) / (float)Math.max(1,aetherValues[x][y]))
                    );
                    aetherValues[x][y] -= 0.1f * (aetherValues[x][y] - (netherValues[x][y] / (float)targetRatios[x][y]));
                }

                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                /* Safety to never let values below zero */
                if (0 > aetherValues[x][y]) {
                    aetherValues[x][y] = 1;
                }
                if (0 > netherValues[x][y]) {
                    netherValues[x][y] = 1;
                }
            }
        }
    }

    @Override
    public void processUnits(int[][] units, World parent){
        process_ether(parent,true);
        determineUnits(units,parent);
    }

    @Override
    public void processTypes(int[][] units, World parent){
        /* Take over unit changes from Elemental plane */
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(Material.Elements.Ether != parent.elementalPlane.elementAt(x,y)){
                    targetRatios[x][y] = Material.ratioOf(parent.elementalPlane.elementAt(x,y));
                }
                takeOverUnitChanges(x,y, units);
            }
        }
    }

    @Override
    public void takeOverUnitChanges(int x, int y, int[][] units) {
        float old_unit = ((float)netherValues[x][y] + aetherValues[x][y])/2.0f;
        netherValues[x][y] *= units[x][y] / old_unit;
        aetherValues[x][y] *= units[x][y] / old_unit;
    }

    @Override
    public void processMechanics(int[][] units, World parent) {
        processTypes(units, parent);
    }

    @Override
    public void postProcess(int[][] units, World parent) {
        process_ether(parent,false);
        /* TODO: Decide para-modifiers ( e.g. heat ) */
    }

    @Override
    public void determineUnits(int[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = (aetherValueAt(x,y) + netherValueAt(x,y))/2;
            }
        }
    }

    public int aetherValueAt(int x, int y){
        return aetherValues[x][y];
    }
    public int netherValueAt(int x, int y){
        return netherValues[x][y];
    }
    private float get_ratio_delta(int x, int y){
        return getRatio(x,y) - targetRatios[x][y];
    }
    public float getRatio(int x, int y){
        if(0 != aetherValues[x][y])
            return ((float)netherValues[x][y]/ aetherValues[x][y]);
        else return 0;
    }

    public Material.Elements elementAt(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 1) return Material.Elements.Air;
        else if(1 > Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }

    private int getTargetRatio(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 1) return Material.ratioOf(Material.Elements.Air);
        else if(1 > Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether)))
            return Material.ratioOf(Material.Elements.Ether);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.ratioOf(Material.Elements.Earth);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.ratioOf(Material.Elements.Water);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.ratioOf(Material.Elements.Air);
        else return Material.ratioOf(Material.Elements.Fire);
    }

    private float getTargetAether(int x, int y){
        return (netherValues[x][y] / (targetRatios[x][y] + ((1 - nether_dynamic)*get_ratio_delta(x,y))));
    }

    private float getTargetNether(int x, int y){
        return (aetherValues[x][y] * (targetRatios[x][y] + nether_dynamic*(get_ratio_delta(x,y))));
    }

    public void addAetherTo(int x, int y, int value){
        aetherValues[x][y] = Math.max(1, aetherValues[x][y]+value);
    }
    public void addNetherTo(int x, int y, int value){
        netherValues[x][y] = Math.max(1, netherValues[x][y]+value);
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
