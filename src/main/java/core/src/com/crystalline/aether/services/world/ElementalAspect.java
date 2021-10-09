package com.crystalline.aether.services.world;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.ElementalAspectStrategy;
import com.crystalline.aether.models.world.Material;
import com.crystalline.aether.models.architecture.RealityAspect;
import com.crystalline.aether.models.world.RealityAspectStrategy;
import com.crystalline.aether.services.computation.CPUBackend;
import com.crystalline.aether.services.computation.GPUBackend;
import com.crystalline.aether.services.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Random;

public class ElementalAspect extends RealityAspect {
    private final Random rnd = new Random();

    private FloatBuffer elements;
    private FloatBuffer forces;

    private final FloatBuffer proposedChanges;

    private float[][] touchedByMechanics; /* Debug purposes */

    private final CPUBackend backend;
    private final GPUBackend gpuBackend;
    private final int processUnitsPhaseIndex;
    private final int processTypesPhaseIndex;
    private final int processTypeUnitsPhaseIndex;
    private final int defineByEtherealPhaseIndex;
    private final int switchElementsPhaseIndex;
    private final int switchForcesPhaseIndex;
    private final int initChangesPhaseIndex;
    private final int proposeForcesPhaseIndex;
    private final int proposeChangesFromForcesPhaseIndex;
    private final int arbitrateChangesPhaseIndex;
    private final int applyChangesDynamicsPhaseIndex;
    private final int mechanicsPostProcessDynamicsPhaseIndex;
    private final int arbitrateInteractionsPhaseIndex;

    private final FloatBuffer[] processUnitsPhaseInputs;
    private final FloatBuffer[] processTypesPhaseInputs;
    private final FloatBuffer[] processTypeUnitsPhaseInputs;
    private final FloatBuffer[] defineByEtherealPhaseInputs;
    private final FloatBuffer[] switchElementsPhaseInputs;
    private final FloatBuffer[] switchForcesPhaseInputs;
    private final FloatBuffer[] initChangesPhaseInputs;
    private final FloatBuffer[] proposeForcesPhaseInputs;
    private final FloatBuffer[] proposeChangesFromForcesPhaseInputs;
    private final FloatBuffer[] arbitrateChangesPhaseInputs;
    private final FloatBuffer[] applyChangesDynamicsPhaseInputs;
    private final FloatBuffer[] mechanicsPostProcessDynamicsPhaseInputs;
    private final FloatBuffer[] arbitrateInteractionsPhaseInputs;

    private final int debugIndex;
    public ElementalAspect(Config conf_){
        super(conf_);
        elements = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        forces = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        proposedChanges = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        touchedByMechanics = new float[conf.getChunkBlockSize()][conf.getChunkBlockSize()];
        backend = new CPUBackend();
        gpuBackend = new GPUBackend(conf.getChunkBlockSize());
        ElementalAspectStrategy strategy = new ElementalAspectStrategy(conf.getChunkBlockSize());

        if(!useGPU){
            defineByEtherealPhaseIndex = backend.addPhase(strategy::defineByEtherealPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            processUnitsPhaseIndex = backend.addPhase(strategy::processUnitsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            processTypesPhaseIndex = backend.addPhase(strategy::processTypesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            processTypeUnitsPhaseIndex = backend.addPhase(strategy::processTypeUnitsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            switchElementsPhaseIndex = backend.addPhase(strategy::switchElementsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            switchForcesPhaseIndex = backend.addPhase(strategy::switchForcesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            initChangesPhaseIndex = backend.addPhase(strategy::initChangesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            proposeForcesPhaseIndex = backend.addPhase(strategy::proposeForcesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            proposeChangesFromForcesPhaseIndex = backend.addPhase(strategy::proposeChangesFromForcesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
            arbitrateChangesPhaseIndex = backend.addPhase(strategy::arbitrateChangesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        }else{
            defineByEtherealPhaseIndex = initKernel(ElementalAspectStrategy.defineByEtherealPhaseKernel, gpuBackend);
            processUnitsPhaseIndex = initKernel(ElementalAspectStrategy.processUnitsPhaseKernel, gpuBackend);
            processTypesPhaseIndex = initKernel(ElementalAspectStrategy.processTypesPhaseKernel, gpuBackend);
            processTypeUnitsPhaseIndex = initKernel(ElementalAspectStrategy.processTypesUnitsPhaseKernel, gpuBackend);
            switchElementsPhaseIndex = initKernel(ElementalAspectStrategy.switchElementsPhaseKernel, gpuBackend);
            switchForcesPhaseIndex = initKernel(ElementalAspectStrategy.switchForcesPhaseKernel, gpuBackend);
            initChangesPhaseIndex = initKernel(ElementalAspectStrategy.initChangesPhaseKernel, gpuBackend);
            proposeForcesPhaseIndex = initKernel(ElementalAspectStrategy.proposeForcesPhaseKernel, gpuBackend);
            proposeChangesFromForcesPhaseIndex = initKernel(ElementalAspectStrategy.proposeChangesFromForcesPhaseKernel, gpuBackend);
            arbitrateChangesPhaseIndex = initKernel(ElementalAspectStrategy.arbitrateChangesPhaseKernel, gpuBackend);
            debugIndex = backend.addPhase(strategy::arbitrateChangesPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        }
        applyChangesDynamicsPhaseIndex = backend.addPhase(strategy::applyChangesDynamicsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        mechanicsPostProcessDynamicsPhaseIndex = backend.addPhase(strategy::mechanicsPostProcessDynamicsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        arbitrateInteractionsPhaseIndex = backend.addPhase(strategy::arbitrateInteractionsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));

        processUnitsPhaseInputs = new FloatBuffer[]{elements, null};
        processTypesPhaseInputs = new FloatBuffer[]{elements,null,null};
        processTypeUnitsPhaseInputs = new FloatBuffer[]{elements,null};
        defineByEtherealPhaseInputs = new FloatBuffer[2];
        switchElementsPhaseInputs = new FloatBuffer[2];
        switchForcesPhaseInputs = new FloatBuffer[2];
        proposeForcesPhaseInputs = new FloatBuffer[4];
        initChangesPhaseInputs = new FloatBuffer[1];
        proposeChangesFromForcesPhaseInputs = new FloatBuffer[4];
        arbitrateChangesPhaseInputs = new FloatBuffer[4];
        applyChangesDynamicsPhaseInputs = new FloatBuffer[4];
        mechanicsPostProcessDynamicsPhaseInputs = new FloatBuffer[4];
        arbitrateInteractionsPhaseInputs = new FloatBuffer[3];

        reset();
    }

    @Override
    protected Object[] getState() {
        return new Object[]{
            BufferUtils.clone(elements),
            BufferUtils.clone(forces),
            Arrays.copyOf(touchedByMechanics, touchedByMechanics.length)
        };
    }

    @Override
    protected void setState(Object[] state) {
        elements = (FloatBuffer) state[0];
        forces = (FloatBuffer) state[1];
        touchedByMechanics = (float[][]) state[2];
    }

    private void addWater(int ox, int oy, World parent){
        setElement(ox, oy, Material.Elements.Water);
        parent.setUnit(ox, oy,16);
    }

    private void addRock(int ox, int oy, World parent){
        setElement(ox, oy, Material.Elements.Earth);
        parent.setUnit(ox, oy,550);
    }

    public void addMiniPool(World parent){
        addRock((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2)-1, parent);
        addRock((conf.getChunkBlockSize()/2)+2, (conf.getChunkBlockSize()/2), parent);
        addRock((conf.getChunkBlockSize()/2)-2, (conf.getChunkBlockSize()/2), parent);
        addRock((conf.getChunkBlockSize()/2)+2, (conf.getChunkBlockSize()/2)+1, parent);
        addRock((conf.getChunkBlockSize()/2)-2, (conf.getChunkBlockSize()/2)+1, parent);

        addWater((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2), (conf.getChunkBlockSize()/2)+1, parent);
        addWater((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2)+1, (conf.getChunkBlockSize()/2)+1, parent);
        addWater((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2), parent);
        addWater((conf.getChunkBlockSize()/2)-1, (conf.getChunkBlockSize()/2)+1, parent);
    }

    private float maxPrio = 0;
    private void calculatePrio(){
        Random rnd = new Random();
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            ElementalAspectStrategy.setPriority(x,y,conf.getChunkBlockSize(),elements,rnd.nextFloat());
        }}
        while(true){
            int similarities = 0;
            for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                int index_radius = 2;
                int minIndexX = Math.max((x-index_radius), 0);
                int maxIndexX = Math.min((x+index_radius), conf.getChunkBlockSize()-1);
                int minIndexY = Math.max((y-index_radius), 0);
                int maxIndexY = Math.min((y+index_radius), conf.getChunkBlockSize()-1);
                for(int ix = minIndexX; ix <= maxIndexX; ++ix){ for(int iy = minIndexY; iy <= maxIndexY; ++iy) {
                    if( /* TODO: Re-check priority; make it inside bounds; use it as random function */
                        ((x != ix)&&(y != iy))
                        &&(500 > Math.abs(ElementalAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements) - ElementalAspectStrategy.getPriority(ix,iy, conf.getChunkBlockSize(), elements)))
                    ){
                        ElementalAspectStrategy.setPriority(x,y,conf.getChunkBlockSize(),elements,rnd.nextFloat() * 1000000.0f);
                        ++similarities;
                    }
                }}
            }}
            if(0 == similarities)break;
        }
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            if(maxPrio <  ElementalAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements)){
                maxPrio = ElementalAspectStrategy.getPriority(x,y, conf.getChunkBlockSize(), elements);
            }
        }}
    }
    public void reset(){
        calculatePrio();
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            setElement(x,y,Material.Elements.Air);
            ElementalAspectStrategy.setForce(x,y, conf.getChunkBlockSize(), forces,0,0);
            ElementalAspectStrategy.setVelocityTick(x,y, conf.getChunkBlockSize(), proposedChanges, ElementalAspectStrategy.velocityMaxTicks);
            touchedByMechanics[x][y] = 0;
        }}
    }

    public void defineBy(EtherealAspect plane){
        defineByEtherealPhaseInputs[0] = elements;
        plane.provideEtherTo(defineByEtherealPhaseInputs, 1);
        if(!useGPU){
            backend.setInputs(defineByEtherealPhaseInputs);
            backend.runPhase(defineByEtherealPhaseIndex);
            BufferUtils.copy(backend.getOutput(defineByEtherealPhaseIndex), elements);
        }else{
            gpuBackend.setInputs(defineByEtherealPhaseInputs);
            gpuBackend.runPhase(defineByEtherealPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(defineByEtherealPhaseIndex), elements);
        }
    }

    @Override
    public FloatBuffer determineUnits(World parent) {
        return null; /* Don't modify anything */
    }

    @Override
    public void switchValues(FloatBuffer proposals) {
        switchElementsPhaseInputs[0] = proposals;
        switchElementsPhaseInputs[1] = elements;

        if(!useGPU){ /* TODO: change priority value to another random value */
            backend.setInputs(switchElementsPhaseInputs);
            backend.runPhase(switchElementsPhaseIndex);
            BufferUtils.copy(backend.getOutput(switchElementsPhaseIndex), elements);
        }else{
            gpuBackend.setInputs(switchElementsPhaseInputs);
            gpuBackend.runPhase(switchElementsPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(switchElementsPhaseIndex), elements);
        }

        switchForcesPhaseInputs[0] = proposals;
        switchForcesPhaseInputs[1] = forces;
        if(!useGPU){
            backend.setInputs(switchForcesPhaseInputs);
            backend.runPhase(switchForcesPhaseIndex);
            BufferUtils.copy(backend.getOutput(switchForcesPhaseIndex), forces);
        }else{
            gpuBackend.setInputs(switchForcesPhaseInputs);
            gpuBackend.runPhase(switchForcesPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(switchForcesPhaseIndex), forces);
        }
    }

    @Override
    public void processUnits(World parent){
        /* TODO: Calculate dilution
        processUnitsPhaseInputs[0] = elements;
        parent.provideScalarsTo(processUnitsPhaseInputs,1);
        if(!useGPU ||true){
            backend.setInputs(processUnitsPhaseInputs);
            backend.runPhase(processUnitsPhaseIndex);
            parent.setScalars(backend.getOutput(processUnitsPhaseIndex));
        }else{
            gpuBackend.setInputs(processUnitsPhaseInputs);
            gpuBackend.runPhase(processUnitsPhaseIndex);
            parent.setScalars(gpuBackend.getOutput(processUnitsPhaseIndex));
        }
        */
    }

    @Override
    public void processTypes(World parent) {
        processTypesPhaseInputs[0] = elements;
        parent.getEtherealPlane().provideEtherTo(processTypesPhaseInputs,1);
        parent.provideScalarsTo(processTypesPhaseInputs,2);
        if(!useGPU){
            backend.setInputs(processTypesPhaseInputs);
            backend.runPhase(processTypesPhaseIndex);
            BufferUtils.copy(backend.getOutput(processTypesPhaseIndex),elements);

            processTypeUnitsPhaseInputs[0] = elements;
            parent.provideScalarsTo(processTypeUnitsPhaseInputs,1);
            backend.setInputs(processTypeUnitsPhaseInputs);
            backend.runPhase(processTypeUnitsPhaseIndex);
            parent.setScalars(backend.getOutput(processTypeUnitsPhaseIndex));
        }else{
            gpuBackend.setInputs(processTypesPhaseInputs);
            gpuBackend.runPhase(processTypesPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(processTypesPhaseIndex),elements);

            processTypeUnitsPhaseInputs[0] = elements;
            parent.provideScalarsTo(processTypeUnitsPhaseInputs,1);
            gpuBackend.setInputs(processTypeUnitsPhaseInputs);
            gpuBackend.runPhase(processTypeUnitsPhaseIndex);
            parent.setScalars(gpuBackend.getOutput(processTypeUnitsPhaseIndex));
        }

    }

    private void check(FloatBuffer cpu, FloatBuffer gpu, boolean stop){
        int chunkSize = conf.getChunkBlockSize();
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y) {
            if(
                (BufferUtils.get(x,y, chunkSize, 4, 0, cpu) != BufferUtils.get(x,y, chunkSize, 4, 0, gpu))
               // ||(BufferUtils.get(x,y, chunkSize, 4, 1, a) != BufferUtils.get(x,y, chunkSize, 4, 1, b))
               // ||(BufferUtils.get(x,y, chunkSize, 4, 2, cpu) != BufferUtils.get(x,y, chunkSize, 4, 2, gpu))
            ){
                System.out.println(
                "["+x+"]["+y+"]not equal! "
                + "("
                + BufferUtils.get(x,y, chunkSize, 4, 0, cpu) + ","
                + BufferUtils.get(x,y, chunkSize, 4, 1, cpu) + ","
                + BufferUtils.get(x,y, chunkSize, 4, 2, cpu)
                +") != "
                + "("
                + BufferUtils.get(x,y, chunkSize, 4, 0, gpu) + ","
                + BufferUtils.get(x,y, chunkSize, 4, 1, gpu) + ","
                + BufferUtils.get(x,y, chunkSize, 4, 2, gpu)
                +");"
                );
                if(stop)System.exit(1);
            }
        }}
    }

    @Override
    public void processMechanics(World parent) {
        initChangesPhaseInputs[0] = proposedChanges;
        if(!useGPU){ /* Init Mechanics phase */
            backend.setInputs(initChangesPhaseInputs);
            backend.runPhase(initChangesPhaseIndex);
            BufferUtils.copy(backend.getOutput(initChangesPhaseIndex), proposedChanges);
        }else{
            gpuBackend.setInputs(initChangesPhaseInputs);
            gpuBackend.runPhase(initChangesPhaseIndex);
            BufferUtils.copy(gpuBackend.getOutput(initChangesPhaseIndex), proposedChanges);
        }
        for(int x = 1; x < conf.getChunkBlockSize()-1; ++x){
            for(int y = conf.getChunkBlockSize()-2; y > 0; --y){
                touchedByMechanics[x][y] = 0;
            }
        }

        /* Main Mechanic phase */
        for(int i = 0; i < ElementalAspectStrategy.velocityMaxTicks; ++i){
            proposeForcesPhaseInputs[0] = elements;
            proposeForcesPhaseInputs[1] = forces;
            parent.provideScalarsTo(proposeForcesPhaseInputs, 2);
            parent.getEtherealPlane().provideEtherTo(proposeForcesPhaseInputs, 3);
            if(!useGPU){
                backend.setInputs(proposeForcesPhaseInputs);
                backend.runPhase(proposeForcesPhaseIndex); /* output: new proposed forces */
            }else{
                gpuBackend.setInputs(proposeForcesPhaseInputs);
                gpuBackend.runPhase(proposeForcesPhaseIndex); /* output: new proposed forces */
            }

            proposeChangesFromForcesPhaseInputs[0] = proposedChanges;
            proposeChangesFromForcesPhaseInputs[1] = elements;
            parent.provideScalarsTo(proposeChangesFromForcesPhaseInputs, 3);
            if(!useGPU){
                proposeChangesFromForcesPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
                backend.setInputs(proposeChangesFromForcesPhaseInputs);
                backend.runPhase(proposeChangesFromForcesPhaseIndex); /* output: newly proposed changes */
            }else{
                proposeChangesFromForcesPhaseInputs[2] = gpuBackend.getOutput(proposeForcesPhaseIndex);
                gpuBackend.setInputs(proposeChangesFromForcesPhaseInputs);
                gpuBackend.runPhase(proposeChangesFromForcesPhaseIndex); /* output: newly proposed changes */
            }

            arbitrateChangesPhaseInputs[1] = elements;
            parent.provideScalarsTo(arbitrateChangesPhaseInputs, 3);
            if(!useGPU){
                arbitrateChangesPhaseInputs[0] = backend.getOutput(proposeChangesFromForcesPhaseIndex);
                arbitrateChangesPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
                backend.setInputs(arbitrateChangesPhaseInputs);
                backend.runPhase(arbitrateChangesPhaseIndex); /* output: newly proposed changes */
            }else{
                arbitrateChangesPhaseInputs[0] = gpuBackend.getOutput(proposeChangesFromForcesPhaseIndex);
                arbitrateChangesPhaseInputs[2] = gpuBackend.getOutput(proposeForcesPhaseIndex);
                gpuBackend.setInputs(arbitrateChangesPhaseInputs);
                gpuBackend.runPhase(arbitrateChangesPhaseIndex); /* output: newly proposed changes */
            }

            if(!useGPU){
                arbitrateInteractionsPhaseInputs[0] = backend.getOutput(arbitrateChangesPhaseIndex);
            }else{
                arbitrateInteractionsPhaseInputs[0] = gpuBackend.getOutput(arbitrateChangesPhaseIndex);
            }
            arbitrateInteractionsPhaseInputs[1] = elements;
            parent.provideScalarsTo(arbitrateInteractionsPhaseInputs, 2);
            backend.setInputs(arbitrateInteractionsPhaseInputs);
            backend.runPhase(arbitrateInteractionsPhaseIndex); /* Output: proposed changes, toApply where switches will happen */
            /* TODO: unify toApply and Offset if possible */

            /*!Note: arbitrateInteractions decides whether switches happen, but not whether interactions will happen!
             * This is why arbitrateChanges is being used here.
             * */
            applyChangesDynamicsPhaseInputs[1] = elements;
            if(!useGPU){
                applyChangesDynamicsPhaseInputs[0] = backend.getOutput(arbitrateChangesPhaseIndex);
                applyChangesDynamicsPhaseInputs[2] = backend.getOutput(proposeForcesPhaseIndex);
            }else{
                applyChangesDynamicsPhaseInputs[0] = gpuBackend.getOutput(arbitrateChangesPhaseIndex);
                applyChangesDynamicsPhaseInputs[2] = gpuBackend.getOutput(proposeForcesPhaseIndex);
            }
            parent.provideScalarsTo(applyChangesDynamicsPhaseInputs, 3);
            backend.setInputs(applyChangesDynamicsPhaseInputs);
            backend.runPhase(applyChangesDynamicsPhaseIndex); /* output: dynamics before the swaps in light of the proposed ones */
            BufferUtils.copy(backend.getOutput(applyChangesDynamicsPhaseIndex), forces); /* TODO: maybe copies can be avoided here? */

            parent.switchValues(backend.getOutput(arbitrateInteractionsPhaseIndex));

            if(i == ElementalAspectStrategy.velocityMaxTicks-1){
                BufferUtils.copy(backend.getOutput(arbitrateInteractionsPhaseIndex), proposedChanges);
            }
        }

        mechanicsPostProcessDynamicsPhaseInputs[0] = elements;
        mechanicsPostProcessDynamicsPhaseInputs[1] = forces;
        parent.provideScalarsTo(mechanicsPostProcessDynamicsPhaseInputs, 2);
        mechanicsPostProcessDynamicsPhaseInputs[3] = proposedChanges;
        backend.setInputs(mechanicsPostProcessDynamicsPhaseInputs);
        backend.runPhase(mechanicsPostProcessDynamicsPhaseIndex);
        BufferUtils.copy(backend.getOutput(mechanicsPostProcessDynamicsPhaseIndex), forces);
    }

    /* TODO: Make movable objects, depending of the solidness "merge into one another", leaving vacuum behind, which are to resolved at the end of the mechanics round */
    @Override
    public void postProcess(World parent) {
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                setElement(x,y, parent.etherealPlane.elementAt(x,y));/* TODO: keep priority should buffered backend be used */
            }
        }
    }

    /**
     * Create a simple pond with some fire on one side
     * @param floorHeight - the height of the ground floor
     */
    public void pondWithGrill(World parent, int floorHeight){
        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ /* create the ground floor */
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                ElementalAspectStrategy.setForce(x,y, conf.getChunkBlockSize(), forces,0,0);
                if(
                    (y <= floorHeight)
                    &&(0 < x)&&(conf.getChunkBlockSize()-1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
                ){
                    setElement(x,y, Material.Elements.Earth);
                    if(y <= (floorHeight/2)) parent.setUnit(x,y, Math.min(100,rnd.nextInt(500)));
                    else {
                        if(rnd.nextInt(8) > (y%8)) parent.setUnit(x,y,Math.max(10,rnd.nextInt(30)));
                            else parent.setUnit(x,y,Math.max(19,rnd.nextInt(50)));
                    }
                }else{
                    setElement(x,y, Material.Elements.Air);
                    parent.setUnit(x,y,Math.min(3,rnd.nextInt(10)));
                }
            }
        }

        int x; int y; /* Create the pond */
        for(float radius = 0; radius < (floorHeight/2.0f); radius += 0.5f){
            for(float sector = (float)Math.PI * 0.99f; sector < Math.PI * 2.01f; sector += Math.PI / 180){
                x = (int)(conf.getChunkBlockSize()/2 + Math.cos(sector) * radius);
                x = Math.max(0, Math.min(conf.getChunkBlockSize(), x));
                y = (int)(floorHeight + Math.sin(sector) * radius);
                y = Math.max(0, Math.min(conf.getChunkBlockSize(), y));
                if(y <= (floorHeight - (floorHeight/4)) && (0 == rnd.nextInt(3)))
                    parent.setUnit(x,y,parent.getUnit(x,y) * 2.5f);
                setElement(x,y, Material.Elements.Water);
            }
        }

        /* Create a fire */
        x = conf.getChunkBlockSize()/4;
        y = floorHeight + 1;
        setElement(x,y, Material.Elements.Fire);
        setElement(x-1,y, Material.Elements.Fire);
        setElement(x+1,y, Material.Elements.Fire);
        setElement(x,y+1, Material.Elements.Fire);
    }

    public void provideElementsTo(FloatBuffer[] inputs, int inputIndex){
        inputs[inputIndex] = elements;
    }

    public Color getColor(int x, int y, World parent){
        return Material.getColor(getElement(x,y), parent.getUnit(x,y)).cpy();
    }

    public Material.Elements getElement(int x, int y){
        return ElementalAspectStrategy.getElementEnum(x,y,conf.getChunkBlockSize(), elements);
    }

    public void setElement(int x, int y, Material.Elements element){
        ElementalAspectStrategy.setElement(x,y,conf.getChunkBlockSize(),elements,element);
    }

    public void debugMeasure(World parent){
//        for(int x = 0;x < conf.getChunkBlockSize(); ++x){ /* create the ground floor */
//            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
//                unitDebugVariable[x][y] = parent.getUnit(x,y);
//                if(getElement(x,y) == Material.Elements.Fire){
//                }
//            }
//        }
    }

    public void debugPrint(World parent){

    }

    public void setDebugViewPercent(float percent){
        debugViewPercent = percent;
    }

    private boolean showForces = false;
    public void setShowForces(boolean show){
        showForces = show;
    }

    float debugViewPercent = 0;
    public Color getDebugColor(int x, int y, World parent){
        Color defColor = getColor(x,y, parent).cpy(); /*  TODO: Use spellUtil getColorOf */
//        if(0 < touchedByMechanics[x][y]){ /* it was modified.. */
//            defColor.lerp(Color.GREEN, 0.5f); /* to see if it was touched by the mechanics */
        float unitsDiff = 0;
        if( x == conf.getChunkBlockSize()/2 &&  y == conf.getChunkBlockSize() / 3)
            unitsDiff = 0.8f;

        Color debugColor;
        debugColor = new Color(
        ElementalAspectStrategy.getVelocityTick(x,y, conf.getChunkBlockSize(), gpuBackend.getOutput(arbitrateChangesPhaseIndex))
            /(float)ElementalAspectStrategy.velocityMaxTicks,
        RealityAspectStrategy.getOffsetCode(x,y, conf.getChunkBlockSize(), gpuBackend.getOutput(arbitrateChangesPhaseIndex))
            /8.0f,
        0.5f * ElementalAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), gpuBackend.getOutput(arbitrateChangesPhaseIndex)),
        1
        );

        /* Red <-> Blue: left <-> right */
        float offsetX = (RealityAspectStrategy.getTargetX(x,y, conf.getChunkBlockSize(), proposedChanges) - x + 1) / 2.0f;
        float offsetY = (RealityAspectStrategy.getTargetY(x,y, conf.getChunkBlockSize(), proposedChanges) - y + 1) / 2.0f;
//        debugColor = new Color(1.0f - offsetX, offsetY, offsetX, 1.0f); /* <-- proposed offsets *///        float prio = getPriority(x,y, conf.getChunkBlockSize(), elements)/maxPrio;
//        debugColor = new Color(
//////                prio, prio, prio, 1.0f
//////                    offsetR,
//                parent.getEtherealPlane().getReleasedNether(x,y)/parent.getEtherealPlane().netherValueAt(x,y),
//////                //-Math.max(0.0f, Math.min(-5.0f, forces[x][y].y))/5.0f,
////////                    (0 == touchedByMechanics[x][y])?0.0f:1.0f,
//                    0f,//unitsDiff,
//                parent.getEtherealPlane().getReleasedAether(x,y)/parent.getEtherealPlane().aetherValueAt(x,y),//Math.max(1.0f, Math.min(0.0f, forces[x][y].x)),
//////                    offsetB,
//                1.0f
//            );
             defColor.lerp(debugColor,debugViewPercent);
//        }
        return defColor;
    }

}
