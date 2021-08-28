package com.crystalline.aether.services.world;

import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.EtherealAspectStrategy;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.services.computation.CPUBackend;
import com.crystalline.aether.services.computation.GPUBackend;
import com.crystalline.aether.services.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* TODO: Surplus Ether to modify the force of the Ether vapor in an increased amount */
public class EtherealAspect extends RealityAspect {
    private FloatBuffer etherValues;
    private final CPUBackend backend; /* TODO: Make implementation common */
    private final GPUBackend gpuBackend;
    private final int preprocessPhaseIndex;
    private final int sharingPhaseIndex;
    private final int finalizePhaseIndex;
    private final int processTypesPhaseIndex;
    private final int determineUnitsPhaseIndex;
    private final int defineByElementalPhaseIndex;
    private final int switchEtherPhaseIndex;

    private final FloatBuffer[] finalizeInputs;
    private final FloatBuffer[] sharingInputs;
    private final FloatBuffer[] preProcessInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] determineUnitsPhaseInputs;
    private final FloatBuffer[] defineByElementalPhaseInputs;
    private final FloatBuffer[] switchEtherPhaseInputs;

    private final boolean useGPU = true;

    private int initKernel(String source){
        try{
            return gpuBackend.addPhase(source, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        } catch (Exception e) {
            String[] source_ = source.split("\n");
            int line = 0;
            for(String s : source_){
                System.out.println("["+line+"] " + s);
                ++line;
            }
            e.printStackTrace();
            return -1;
        }
    }

    public EtherealAspect(Config conf_){
        super(conf_);
        backend = new CPUBackend();
        gpuBackend = new GPUBackend(conf.getChunkBlockSize());
        etherValues = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        EtherealAspectStrategy strategy = new EtherealAspectStrategy(conf.getChunkBlockSize());

        preprocessPhaseIndex = backend.addPhase(strategy::preProcessCalculationPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        sharingPhaseIndex = backend.addPhase(strategy::sharingCalculationPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        finalizePhaseIndex = backend.addPhase(strategy::finalizeCalculationPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        if(!useGPU){
            processTypesPhaseIndex = backend.addPhase(strategy::processTypesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            determineUnitsPhaseIndex = backend.addPhase(strategy::determineUnitsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            defineByElementalPhaseIndex = backend.addPhase(strategy::defineByElementalPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        }else
        {
            processTypesPhaseIndex = initKernel(EtherealAspectStrategy.processTypesPhaseKernel);
            determineUnitsPhaseIndex = initKernel(EtherealAspectStrategy.determineUnitsPhaseKernel);
            defineByElementalPhaseIndex = initKernel(EtherealAspectStrategy.defineByElementalPhaseKernel);
        }
        switchEtherPhaseIndex = backend.addPhase(strategy::switchEtherPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        preProcessInputs = new FloatBuffer[1];
        sharingInputs = new FloatBuffer[1];
        finalizeInputs = new FloatBuffer[3];
        processTypesPhaseInputs = new FloatBuffer[]{etherValues, null, null};
        determineUnitsPhaseInputs = new FloatBuffer[]{etherValues};
        defineByElementalPhaseInputs = new FloatBuffer[]{null, null};
        switchEtherPhaseInputs = new FloatBuffer[2];
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
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                setAether(x,y, 1);
                setNether(x,y, Material.ratioOf(Material.Elements.Air));
            }
        }
    }

    public void defineBy(ElementalAspect plane, World parent){
        int x = conf.getChunkBlockSize()/2;
        plane.provideElementsTo(defineByElementalPhaseInputs,0);
        parent.provideScalarsTo(defineByElementalPhaseInputs, 1);
        if(useGPU){
            gpuBackend.setInputs(defineByElementalPhaseInputs);
            gpuBackend.runPhase(defineByElementalPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(defineByElementalPhaseIndex), etherValues);
        }else{
            backend.setInputs(defineByElementalPhaseInputs);
            backend.runPhase(defineByElementalPhaseIndex);
            BufferUtils.copy(backend.getOutput(defineByElementalPhaseIndex), etherValues);
        }
    }

    @Override
    public void switchValues(FloatBuffer proposals) {
        switchEtherPhaseInputs[0] = proposals;
        switchEtherPhaseInputs[1] = etherValues;
        backend.setInputs(switchEtherPhaseInputs);
        backend.runPhase(switchEtherPhaseIndex);
        BufferUtils.copy(backend.getOutput(switchEtherPhaseIndex), etherValues);
    }

    public float getMaxNether(int x, int y){
        return aetherValueAt(x,y) * Material.ratioOf(Material.Elements.Fire);
    }

    public float getMinAether(int x, int y){
        return netherValueAt(x,y) / Material.ratioOf(Material.Elements.Earth);
    }

    private void processEther() {
        /* Pre-process phase: copy the released ether for each buffer */
        preProcessInputs[0] = etherValues;
        backend.setInputs(preProcessInputs);
        backend.runPhase(preprocessPhaseIndex);

        /* sharing phase: released ether to be averaged together */
        sharingInputs[0] = backend.getOutput(preprocessPhaseIndex);
        backend.setInputs(sharingInputs);
        backend.runPhase(sharingPhaseIndex);

        /* finalize phase: final ether to be read and decided */
        finalizeInputs[0] = backend.getOutput(preprocessPhaseIndex);
        finalizeInputs[1] = backend.getOutput(sharingPhaseIndex);
        finalizeInputs[2] = etherValues;
        backend.setInputs(finalizeInputs);
        backend.runPhase(finalizePhaseIndex);
        BufferUtils.copy(backend.getOutput(finalizePhaseIndex), etherValues);
    }

    @Override
    public void processUnits(World parent){
        processEther();
        parent.setScalars(determineUnits(parent));
    }

    @Override
    public void processTypes(World parent){
        processTypesPhaseInputs[0] = etherValues;
        parent.getElementalPlane().provideElementsTo(processTypesPhaseInputs, 1);
        parent.provideScalarsTo(processTypesPhaseInputs,2); /* TODO: This might be needed only once? */
        if(!useGPU){
            backend.setInputs(processTypesPhaseInputs);
            backend.runPhase(processTypesPhaseIndex);
            BufferUtils.copy(backend.getOutput(processTypesPhaseIndex), etherValues);
        }else{
            gpuBackend.setInputs(processTypesPhaseInputs);
            gpuBackend.runPhase(processTypesPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(processTypesPhaseIndex), etherValues);
        }
    }

    private float getUnit(int x, int y){
        return EtherealAspectStrategy.getUnit(x,y,conf.getChunkBlockSize(), etherValues);
    }

    private float getAetherValue(int x, int y){
        return EtherealAspectStrategy.getAetherValue(x,y,conf.getChunkBlockSize(), etherValues);
    }
    private float getNetherValue(int x, int y){
        return EtherealAspectStrategy.getNetherValue(x,y,conf.getChunkBlockSize(), etherValues);
    }

    @Override
    public FloatBuffer determineUnits(World parent) {
        determineUnitsPhaseInputs[0] = etherValues;
        if(!useGPU){
            backend.setInputs(determineUnitsPhaseInputs);
            backend.runPhase(determineUnitsPhaseIndex);
            return backend.getOutput(determineUnitsPhaseIndex);
        }else{
            gpuBackend.setInputs(determineUnitsPhaseInputs);
            gpuBackend.runPhase(determineUnitsPhaseIndex);
            return gpuBackend.getOutput(determineUnitsPhaseIndex);
        }
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
    public void setEther(FloatBuffer value){
        BufferUtils.copy(value, etherValues);
    }

    public float aetherValueAt(int x, int y){
        return EtherealAspectStrategy.getAetherValue(x,y, conf.getChunkBlockSize(), etherValues);
    }

    public float netherValueAt(int x, int y){
        return EtherealAspectStrategy.getNetherValue(x,y, conf.getChunkBlockSize(), etherValues);
    }

    public float getReleasedAether(int x, int y){
        return EtherealAspectStrategy.getReleasedAether(x,y, conf.getChunkBlockSize(), backend.getOutput(preprocessPhaseIndex));
    }
    public float getReleasedNether(int x, int y){
        return EtherealAspectStrategy.getReleasedNether(x,y, conf.getChunkBlockSize(), backend.getOutput(preprocessPhaseIndex));
    }

    public float getAvgReleasedAether(int x, int y){
        return EtherealAspectStrategy.getAvgReleasedAether(x,y, conf.getChunkBlockSize(), backend.getOutput(preprocessPhaseIndex));
    }
    public float getAvgReleasedNether(int x, int y){
        return EtherealAspectStrategy.getAvgReleasedNether(x,y, conf.getChunkBlockSize(), backend.getOutput(preprocessPhaseIndex));
    }

    public float getRatio(int x, int y){
        return EtherealAspectStrategy.getRatio(x,y,conf.getChunkBlockSize(),etherValues);
    }

    public Material.Elements elementAt(int x, int y){
        return EtherealAspectStrategy.getElementEnum(x,y,conf.getChunkBlockSize(),etherValues);
    }

    public void addAetherTo(int x, int y, float value){
        setAether(x,y, Math.max(0.01f, aetherValueAt(x,y) + value));
    }
    public void setAether(int x, int y, float value){
        EtherealAspectStrategy.setAether(x,y, conf.getChunkBlockSize(), etherValues, Math.max(0.01f, value));
    }

    public void addNether(int x, int y, float value){
        setNether(x,y, Math.max(0.01f, netherValueAt(x,y) + value));
    }
    public void setNether(int x, int y, float value){
        EtherealAspectStrategy.setNether(x,y, conf.getChunkBlockSize(), etherValues, Math.max(0.01f, value));
    }
    public void tryToEqualize(int x, int y, float aetherDelta, float netherDelta, float ratio){
        EtherealAspectStrategy.tryToEqualize(x,y, conf.getChunkBlockSize(), etherValues,aetherDelta,netherDelta,ratio);
    }

}
