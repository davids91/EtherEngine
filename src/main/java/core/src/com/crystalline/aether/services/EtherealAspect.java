package com.crystalline.aether.services;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.models.Reality_aspect;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends Reality_aspect {
    protected final int sizeX;
    protected final int sizeY;

    private final int [][] ratio_change_tick;
    private final float [][] aetherValues; /* Stationary substance */
    private final float [][] netherValues; /* Moving substance */
    private final float [][] target_ratios;

    private static final int ticks_to_change = 0;
    private static final float nether_dynamic = 0.9f;

    public EtherealAspect(Config conf_){
        super(conf_);
        sizeX = conf.world_block_number[0];
        sizeY = conf.world_block_number[1];
        aetherValues = new float[sizeX][sizeY];
        netherValues = new float[sizeX][sizeY];
        target_ratios = new float[sizeX][sizeY];
        ratio_change_tick = new int[sizeX][sizeY];
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                aetherValues[x][y] = 1.0f;
                if((0 == x)||(x == (sizeX-1))||(0 == y)||(y == (sizeY-1))){
                    target_ratios[x][y] = Materials.nether_ratios[Materials.Names.Ether.ordinal()];
                    netherValues[x][y] = 1.0f;
                }else{
                    target_ratios[x][y] = Materials.nether_ratios[Materials.Names.Air.ordinal()];
                    netherValues[x][y] = Materials.nether_ratios[Materials.Names.Air.ordinal()];
                }
                ratio_change_tick[x][y] = 0;
            }
        }
    }

    public void define_by(ElementalAspect plane, float [][] units){
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(0 < units[x][y]) {
                    netherValues[x][y] = units[x][y];
                    aetherValues[x][y] = netherValues[x][y] / Materials.nether_ratios[plane.element_at(x, y).ordinal()];
                }else{
                    netherValues[x][y] = 0f;
                    aetherValues[x][y] = 0f;
                }
                target_ratios[x][y] = get_target_ratio(x,y);
            }
        }
    }

    private float get_ether_step(float requested, float available){
        if(0 < requested) return Math.min(requested, available);
        else return requested;
    }

    private float avg_of(int x, int y, float[][] table){
        float ret = 0.0f;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(sizeX, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(sizeX, y + 2); ++ny) {
                ret += table[x][y];
            }
        }
        return (ret / 9.0f);
    }

    @Override
    public void switch_values(int fromX, int fromY, int toX, int toY) {
        float tmp_val = aetherValues[toX][toY];
        aetherValues[toX][toY] = aetherValues[fromX][fromY];
        aetherValues[fromX][fromY] = tmp_val;

        tmp_val = netherValues[toX][toY];
        netherValues[toX][toY] = netherValues[fromX][fromY];
        netherValues[fromX][fromY] = tmp_val;

        tmp_val = target_ratios[toX][toY];
        target_ratios[toX][toY] = target_ratios[fromX][fromY];
        target_ratios[fromX][fromY] = tmp_val;
    }

    private void process_ether(float[][] units, World parent, boolean decide_target_ratios) {
        float[][] requested_aether = new float[sizeX][sizeY];
        float[][] available_aether = new float[sizeX][sizeY];
        float[][] requested_nether = new float[sizeX][sizeY];
        float[][] available_nether = new float[sizeX][sizeY];
        float[][] available_avg_ae = new float[sizeX][sizeY];
        float[][] available_avg_ne = new float[sizeX][sizeY];
        float[][] requested_avg_ae = new float[sizeX][sizeY];
        float[][] requested_avg_ne = new float[sizeX][sizeY];

        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()])
                    continue;
                if (decide_target_ratios){
                    if(0 == ratio_change_tick[x][y]){
                        target_ratios[x][y] = get_target_ratio(x, y);
                        ratio_change_tick[x][y] = ticks_to_change;
                    }else --ratio_change_tick[x][y];
                }

                /* calculate the values ether is converging to */
                requested_aether[x][y] = get_target_aether(x, y) - aetherValues[x][y];
                requested_nether[x][y] = get_target_nether(x, y) - netherValues[x][y];
                aetherValues[x][y] += requested_aether[x][y];
                available_aether[x][y] -= requested_aether[x][y];
                netherValues[x][y] += requested_nether[x][y];
                available_nether[x][y] -= requested_nether[x][y];
            }
        }

        /* Sharing ether */
        for (int x = 0; x < sizeX; ++x) { /* Let's see the ether requests in the contexts */
            for (int y = 0; y < sizeY; ++y) {
                if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()])
                    continue;
                requested_avg_ae[x][y] = avg_of(x, y, available_aether);
                requested_avg_ne[x][y] = avg_of(x, y, available_nether);
            }
        }

        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()])
                    continue;
                available_avg_ae[x][y] = parent.avg_of_compatible(x, y, available_aether);
                available_avg_ne[x][y] = parent.avg_of_compatible(x, y, available_nether);
            }
        }

        /* finalize Ether */
        for (int x = 0; x < sizeX; ++x) {
            for (int y = 0; y < sizeY; ++y) {
                if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()])
                    continue;
                available_aether[x][y] = available_avg_ae[x][y];
                available_nether[x][y] = available_avg_ne[x][y];

                aetherValues[x][y] -= requested_aether[x][y];
                available_aether[x][y] += requested_aether[x][y];
                netherValues[x][y] -= requested_nether[x][y];
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
                if(netherValues[x][y] > aetherValues[x][y] * target_ratios[x][y]) {
                    parent.get_elm_plane().get_force(x,y).scl( /* Surplus Nether enhances movement */
                        (netherValues[x][y] / (aetherValues[x][y] * target_ratios[x][y]))
                    );
                    netherValues[x][y] -= 0.1f * (netherValues[x][y] - (aetherValues[x][y] * target_ratios[x][y]));
                }

                if(aetherValues[x][y] > netherValues[x][y] / target_ratios[x][y]) {
                    parent.get_elm_plane().get_force(x,y).scl( /* Surplus Aether depresses movement */
                        ((netherValues[x][y] / target_ratios[x][y]) / aetherValues[x][y])
                    );
                    aetherValues[x][y] -= 0.1f * (aetherValues[x][y] - (netherValues[x][y] / target_ratios[x][y]));
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
    public void process_units(float[][] units, World parent){
        process_ether(units,parent,true);
        determine_units(units,parent);
    }

    @Override
    public void process_types(float[][] units, World parent){
        /* Take over unit changes from Elemental plane */
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                if(Materials.Names.Ether != parent.elemental_plane.element_at(x,y)){
                    target_ratios[x][y] = Materials.nether_ratios[parent.elemental_plane.element_at(x,y).ordinal()];
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
    public void process_mechanics(float[][] units, World parent) {
        process_types(units, parent);
    }

    @Override
    public void post_process(float[][] units, World parent) {
        process_ether(units,parent,false);
        /* TODO: Decide para-modifiers ( e.g. heat ) */
    }

    @Override
    public void determine_units(float[][] units, World parent) {
        for(int x = 0;x < sizeX; ++x){
            for(int y = 0; y < sizeY; ++y){
                units[x][y] = (aether_value_at(x,y) + nether_value_at(x,y))/2.0f;
            }
        }
    }

    public float aether_value_at(int x, int y){
        return aetherValues[x][y];
    }
    public float surplus_aether_at(int x, int y){
        return get_target_aether(x,y) - aetherValues[x][y];
    }
    public float nether_value_at(int x, int y){
        return netherValues[x][y];
    }
    public float surplus_nether_at(int x, int y){
        return get_target_nether(x,y) - netherValues[x][y];
    }
    private float get_ratio_delta(int x, int y){
        return get_ratio(x,y) - target_ratios[x][y];
    }

    public float get_ratio(int x, int y){
        if(0 != aetherValues[x][y])
            return (netherValues[x][y]/ aetherValues[x][y]);
        else return 0;
    }

    public Materials.Names element_at(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 0.1f){ /* In case there's almost no ether */
            return Materials.Names.Air;
        }else
        if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()]){
            return Materials.Names.Ether;
        }else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Earth.ordinal()] + Materials.nether_ratios[Materials.Names.Water.ordinal()])/2.0f))
            return Materials.Names.Earth;
        else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Water.ordinal()] + Materials.nether_ratios[Materials.Names.Air.ordinal()])/2.0f))
            return Materials.Names.Water;
        else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Air.ordinal()] + Materials.nether_ratios[Materials.Names.Fire.ordinal()])/2.0f))
            return Materials.Names.Air;
        else return Materials.Names.Fire;
    }

    private float get_target_ratio(int x, int y){
        if((netherValues[x][y] + netherValues[x][y]) < 0.1f){ /* In case there's almost no ether */
            return Materials.nether_ratios[Materials.Names.Air.ordinal()];
        }else
        if(get_ratio(x,y) == Materials.nether_ratios[Materials.Names.Ether.ordinal()]){
            return Materials.nether_ratios[Materials.Names.Ether.ordinal()];
        }
        else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Earth.ordinal()] + Materials.nether_ratios[Materials.Names.Water.ordinal()])/2.0f))
            return Materials.nether_ratios[Materials.Names.Earth.ordinal()];
        else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Water.ordinal()] + Materials.nether_ratios[Materials.Names.Air.ordinal()])/2.0f))
            return Materials.nether_ratios[Materials.Names.Water.ordinal()];
        else if(get_ratio(x,y) <= ((Materials.nether_ratios[Materials.Names.Air.ordinal()] + Materials.nether_ratios[Materials.Names.Fire.ordinal()])/2.0f))
            return Materials.nether_ratios[Materials.Names.Air.ordinal()];
        else return Materials.nether_ratios[Materials.Names.Fire.ordinal()];
    }

    private float get_target_aether(int x, int y){
        return (netherValues[x][y] / (target_ratios[x][y] + ((1.0f - nether_dynamic)*get_ratio_delta(x,y))));
    }

    private float get_target_nether(int x, int y){
        return aetherValues[x][y] * (target_ratios[x][y] + nether_dynamic*(get_ratio_delta(x,y)));
    }

    public void add_aether_to(int x, int y, float value){
        aetherValues[x][y] = Math.max(0.001f, aetherValues[x][y]+value);
    }
    public void add_nether_to(int x, int y, float value){
        netherValues[x][y] = Math.max(0.001f, netherValues[x][y]+value);
    }
    public void tryToEqualize(int x, int y, float value, float ratio){
        float manaLeft = value;
        float manaToUse;
        if(aetherValues[x][y] < (netherValues[x][y]*ratio)){
            manaToUse = Math.min(value, ((netherValues[x][y]/ratio) - aetherValues[x][y]));
            aetherValues[x][y] += manaToUse;
        }else{ /* Aether will most likely be less, the Nether.. */
            manaToUse = Math.min(value, (aetherValues[x][y] - (netherValues[x][y]/ratio)));
            netherValues[x][y] += manaToUse;
        }
        manaLeft -= manaToUse;
        float x1 = ratio*manaLeft / (1.0f + ratio);
        float x2 = manaLeft - x1;
        netherValues[x][y] += x1;
        aetherValues[x][y] += x2;
    }

}
