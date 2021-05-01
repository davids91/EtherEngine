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
    private float [][] aetherValues; /* Stationary substance */
    private float [][] netherValues; /* Moving substance */
    private float [][] targetRatios;

    private static final int ticks_to_change = 0;
    private static final float nether_dynamic = 0.9f;

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.WORLD_BLOCK_NUMBER[0];
        sizeY = conf.WORLD_BLOCK_NUMBER[1];
        aetherValues = new float[sizeX][sizeY];
        netherValues = new float[sizeX][sizeY];
        targetRatios = new float[sizeX][sizeY];
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
        aetherValues = (float[][])state[0];
        netherValues = (float[][])state[1];
        targetRatios = (float[][])state[2];
        ratio_change_tick = (int[][])state[3];
    }

    public void reset(){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                aetherValues[x][y] = 1.0f;
                targetRatios[x][y] = Material.ratioOf(Material.Elements.Air);
                netherValues[x][y] = Material.ratioOf(Material.Elements.Air);
                ratio_change_tick[x][y] = 0;
            }
        }
    }

    public void define_by(ElementalAspect plane, float [][] units){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < units[x][y]) {
                    netherValues[x][y] = units[x][y];
                    aetherValues[x][y] = netherValues[x][y] / Material.netherRatios[plane.elementAt(x, y).ordinal()];
                }else{
                    netherValues[x][y] = 0f;
                    aetherValues[x][y] = 0f;
                }
                targetRatios[x][y] = get_target_ratio(x,y);
            }
        }
    }

    private float get_ether_step(float requested, float available){
        if(0 < requested) return Math.min(requested, available);
        else return requested;
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

    public void setTargetRatio(int x, int y, float target){
        targetRatios[x][y] = target;
    }

    @Override
    public void switch_values(int fromX, int fromY, int toX, int toY) {
        float tmp_val = aetherValues[toX][toY];
        aetherValues[toX][toY] = aetherValues[fromX][fromY];
        aetherValues[fromX][fromY] = tmp_val;

        tmp_val = netherValues[toX][toY];
        netherValues[toX][toY] = netherValues[fromX][fromY];
        netherValues[fromX][fromY] = tmp_val;

        tmp_val = targetRatios[toX][toY];
        targetRatios[toX][toY] = targetRatios[fromX][fromY];
        targetRatios[fromX][fromY] = tmp_val;
    }

    private void process_ether(float[][] units, World parent, boolean decide_target_ratios) {
        float[][] requested_aether = new float[sizeX][sizeY];
        float[][] available_aether = new float[sizeX][sizeY];
        float[][] requested_nether = new float[sizeX][sizeY];
        float[][] available_nether = new float[sizeX][sizeY];
        float[][] available_avg_ae = new float[sizeX][sizeY];
        float[][] available_avg_ne = new float[sizeX][sizeY];

        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {

                if (decide_target_ratios){
                    if(0 == ratio_change_tick[x][y]){
                        targetRatios[x][y] = get_target_ratio(x, y);
                        ratio_change_tick[x][y] = ticks_to_change;
                    }else --ratio_change_tick[x][y];
                }

                /* calculate the values ether is converging to */
                requested_aether[x][y] = get_target_aether(x, y) - aetherValues[x][y];
                requested_nether[x][y] = get_target_nether(x, y) - netherValues[x][y];
                available_aether[x][y] -= requested_aether[x][y];
                available_nether[x][y] -= requested_nether[x][y];
            }
        }

        /* Sharing ether */
        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                available_avg_ae[x][y] = parent.avg_of_compatible(x, y, available_aether);
                available_avg_ne[x][y] = parent.avg_of_compatible(x, y, available_nether);
            }
        }

        /* finalize Ether */
        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                available_aether[x][y] = available_avg_ae[x][y];
                available_nether[x][y] = available_avg_ne[x][y];
                available_aether[x][y] += requested_aether[x][y];
                available_nether[x][y] += requested_nether[x][y];

                /* step in the direction of the target ratio */
                float tmp_ether = get_ether_step(requested_aether[x][y], available_aether[x][y]);
                aetherValues[x][y] += tmp_ether;
                available_aether[x][y] -= tmp_ether;
                tmp_ether = get_ether_step(requested_nether[x][y], available_nether[x][y]);
                netherValues[x][y] += tmp_ether;
                available_nether[x][y] -= tmp_ether;

                /* Equalize available polarity values to 0 */
                aetherValues[x][y] += available_aether[x][y];
                netherValues[x][y] += available_nether[x][y];

                /* Surplus Nether to goes into other effects */
                if(netherValues[x][y] > aetherValues[x][y] * targetRatios[x][y]) {
                    parent.getElementalPlane().get_force(x,y).scl( /* Surplus Nether enhances movement */
                            (netherValues[x][y] / (aetherValues[x][y] * targetRatios[x][y]))
                    );
                    netherValues[x][y] -= 0.1f * (netherValues[x][y] - (aetherValues[x][y] * targetRatios[x][y]));
                }

                if(aetherValues[x][y] > netherValues[x][y] / targetRatios[x][y]) {
                    parent.getElementalPlane().get_force(x,y).scl( /* Surplus Aether depresses movement */
                            ((netherValues[x][y] / targetRatios[x][y]) / aetherValues[x][y])
                    );
                    aetherValues[x][y] -= 0.1f * (aetherValues[x][y] - (netherValues[x][y] / targetRatios[x][y]));
                }

                /* TODO: Implement heat */
                /* TODO: Surplus Aether to go into para-effects also */
                /* TODO: Make Earth not share Aether so easily ( decide if this is even needed )  */
                /* Safety to never let values below zero */
                if (0 > aetherValues[x][y]) {
                    aetherValues[x][y] -= aetherValues[x][y] * 1.1f;
                }
                if (0 > netherValues[x][y]) {
                    netherValues[x][y] -= netherValues[x][y] * 1.1f;
                }
            }
        }
    }

    @Override
    public void processUnits(float[][] units, World parent){
        process_ether(units,parent,true);
        determine_units(units,parent);
    }

    @Override
    public void processTypes(float[][] units, World parent){
        /* Take over unit changes from Elemental plane */
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(Material.Elements.Ether != parent.elementalPlane.elementAt(x,y)){
                    targetRatios[x][y] = Material.ratioOf(parent.elementalPlane.elementAt(x,y));
                }
                take_over_unit_changes(x,y, units);
            }
        }
    }

    @Override
    public void take_over_unit_changes(int x, int y, float[][] units) {
        float old_unit = (netherValues[x][y] + aetherValues[x][y])/2.0f;
        netherValues[x][y] *= units[x][y] / old_unit;
        aetherValues[x][y] *= units[x][y] / old_unit;
    }

    @Override
    public void processMechanics(float[][] units, World parent) {
        processTypes(units, parent);
    }

    @Override
    public void postProcess(float[][] units, World parent) {
        process_ether(units,parent,false);
        /* TODO: Decide para-modifiers ( e.g. heat ) */
    }

    @Override
    public void determine_units(float[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = (aetherValueAt(x,y) + netherValueAt(x,y))/2.0f;
            }
        }
    }

    public float aetherValueAt(int x, int y){
        return aetherValues[x][y];
    }
    public float surplus_aether_at(int x, int y){
        return get_target_aether(x,y) - aetherValues[x][y];
    }
    public float netherValueAt(int x, int y){
        return netherValues[x][y];
    }
    public float surplus_nether_at(int x, int y){
        return get_target_nether(x,y) - netherValues[x][y];
    }
    private float get_ratio_delta(int x, int y){
        return getRatio(x,y) - targetRatios[x][y];
    }

    public float getRatio(int x, int y){
        if(0 != aetherValues[x][y])
            return (netherValues[x][y]/ aetherValues[x][y]);
        else return 0;
    }

    public Material.Elements elementAt(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 0.1f) return Material.Elements.Air;
        else if(0.01f > Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether)))
            return Material.Elements.Ether;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.Elements.Earth;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.Elements.Water;
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.Elements.Air;
        else return Material.Elements.Fire;
    }

    private float get_target_ratio(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 0.1f) return Material.ratioOf(Material.Elements.Air);
        else if(0.01f > Math.abs(getRatio(x,y) - Material.ratioOf(Material.Elements.Ether)))
            return Material.ratioOf(Material.Elements.Ether);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Earth) + Material.ratioOf(Material.Elements.Water))/2.0f))
            return Material.ratioOf(Material.Elements.Earth);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Water) + Material.ratioOf(Material.Elements.Air))/2.0f))
            return Material.ratioOf(Material.Elements.Water);
        else if(getRatio(x,y) <= ((Material.ratioOf(Material.Elements.Air) + Material.ratioOf(Material.Elements.Fire))/2.0f))
            return Material.ratioOf(Material.Elements.Air);
        else return Material.ratioOf(Material.Elements.Fire);
    }

    private float get_target_aether(int x, int y){
        return (netherValues[x][y] / (targetRatios[x][y] + ((1.0f - nether_dynamic)*get_ratio_delta(x,y))));
    }

    private float get_target_nether(int x, int y){
        return aetherValues[x][y] * (targetRatios[x][y] + nether_dynamic*(get_ratio_delta(x,y)));
    }

    public void addAetherTo(int x, int y, float value){
        aetherValues[x][y] = Math.max(0.001f, aetherValues[x][y]+value);
    }
    public void addNetherTo(int x, int y, float value){
        netherValues[x][y] = Math.max(0.001f, netherValues[x][y]+value);
    }

    public void tryToEqualize(int x, int y, float aetherDelta, float netherDelta, float ratio){
        aetherValues[x][y] = getEqualizeAttemptAetherValue(aetherValues[x][y],netherValues[x][y],aetherDelta,netherDelta,ratio);
        netherValues[x][y] = getEqualizeAttemptNetherValue(aetherValues[x][y],netherValues[x][y],aetherDelta,netherDelta,ratio);
        /* TODO: the remainder should be radiated into pra-effects */
    }

    public static float getAetherDeltaToTargetRatio(float manaToUse, float aetherValue, float netherValue, float targetRatio){
        return ((netherValue + manaToUse - (targetRatio * aetherValue))/(1.0f + targetRatio));
    }

    public static float getNetherDeltaToTargetRatio(float manaToUse, float aetherValue, float netherValue, float targetRatio){
        /*!Note: Calculation should be the following, but extra corrections are needed to increase the punctuality
         * float netherDelta = ((targetRatio*(aetherValue + manaToUse)) - netherValue)/(1.0f + targetRatio); */
        float aetherDelta = getAetherDeltaToTargetRatio(manaToUse, aetherValue, netherValue, targetRatio);
        float newNetherValue = (aetherValue + aetherDelta) * targetRatio;
        return Math.min(manaToUse, newNetherValue - netherValue);
    }

    public static float getEqualizeAttemptAetherValue(
            float aetherValue,float netherValue,
            float aetherDelta,float netherDelta,
            float targetRatio
    ){
        /* Since Aether is the less reactive one, firstly Nether shall decide how much shall remain */
        float remainingAether = (aetherValue + aetherDelta) - ((netherValue + netherDelta)/targetRatio);
        return aetherValue + aetherDelta - remainingAether;
    }

    public static float getEqualizeAttemptNetherValue(
            float aetherValue,float netherValue,
            float aetherDelta,float netherDelta,
            float targetRatio
    ){
        float remainingAether = (aetherValue + aetherDelta) - ((netherValue + netherDelta)/targetRatio);
        float remainingNether = (netherValue + netherDelta) - ((aetherValue + aetherDelta - remainingAether)*targetRatio);
        return netherValue + netherDelta - remainingNether;
    }

}
