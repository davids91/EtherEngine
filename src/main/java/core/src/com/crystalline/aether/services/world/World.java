package com.crystalline.aether.services.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.spells.SpellAction;
import com.crystalline.aether.models.world.RealityAspectStrategy;
import com.crystalline.aether.services.computation.CPUBackend;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.world.Material;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


/**TODO:
 *  - Heat gate
 *  - Speed gate ( to block or enchance the speed of objects )
 */
public class World {
    Config conf;

    EtherealAspect etherealPlane;
    ElementalAspect elementalPlane;

    private final CPUBackend backend;
    private final int switchScalarsPhaseIndex;
    private final FloatBuffer[] switchScalarsPhaseInputs;

    /**
     * A texture image representing the different scalars of reality
     * - R: units : determines mass
     * - G: not used yet..
     * - B: not used yet..
     * - A: not used yet..
     */
    private final FloatBuffer scalars;

    public World(Config conf_){
        conf = conf_;
        scalars = ByteBuffer.allocateDirect(Float.BYTES * Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        etherealPlane = new EtherealAspect(conf);
        elementalPlane = new ElementalAspect(conf);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);

        backend = new CPUBackend();
        switchScalarsPhaseIndex = backend.addPhase(this::switchScalarsPhase, (Config.bufferCellSize * conf.getChunkBlockSize() * conf.getChunkBlockSize()));
        switchScalarsPhaseInputs = new FloatBuffer[2];
        reset();
    }

    public void reset(){
        etherealPlane.reset();
        elementalPlane.reset();
        elementalPlane.defineBy(etherealPlane);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);
        elementalPlane.addMiniPool(this);
        etherealPlane.defineBy(elementalPlane, this);
    }

    public void pondWithGrill(){
        elementalPlane.pondWithGrill(this,(int)(conf.getChunkBlockSize()/2.0f));
        /* elementalPlane.determineUnits(this); *//* Included in @pondWithGrill */
        etherealPlane.defineBy(elementalPlane, this);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: scalars
     * @param output elements buffer
     */
    private void switchScalarsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){ for(int y = 0; y < conf.getChunkBlockSize(); ++y){
            float unit = getUnit(x,y, conf.getChunkBlockSize(), inputs[1]);
            if(0 != RealityAspectStrategy.getOffsetCodeFromOffsetVector(x,y, conf.getChunkBlockSize(), inputs[0])){
                int targetX = RealityAspectStrategy.getTargetX(x,y, conf.getChunkBlockSize(), inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y, conf.getChunkBlockSize(), inputs[0]);
                int toApply = (int) RealityAspectStrategy.getToApply(x,y, conf.getChunkBlockSize(), inputs[0]);
                if(
                    (0 < x)&&(conf.getChunkBlockSize() -1 > x)&&(0 < y)&&(conf.getChunkBlockSize()-1 > y)
                    &&(0 < toApply)
                    &&(targetX >= 0)&&(targetX < conf.getChunkBlockSize())
                    &&(targetY >= 0)&&(targetY < conf.getChunkBlockSize())
                ){
                    unit = getUnit(targetX,targetY, conf.getChunkBlockSize(), inputs[1]);
                }
            }
            setUnit(x,y, conf.getChunkBlockSize(), output, unit);
        }}
    }

    public void switchValues(FloatBuffer proposals){
        etherealPlane.switchValues(proposals);
        elementalPlane.switchValues(proposals);
        switchScalarsPhaseInputs[0] = proposals;
        switchScalarsPhaseInputs[1] = scalars;
        backend.setInputs(switchScalarsPhaseInputs);
        backend.runPhase(switchScalarsPhaseIndex);
        BufferUtils.copy(backend.getOutput(switchScalarsPhaseIndex), scalars);
    }

    /* TODO: Fill in the edge of the chunk from neighbouring chunks */
    public void mainLoop(float step){
        elementalPlane.debugMeasure(this);

        /* ============= PROCESS UNITS ============= */
        etherealPlane.processUnits(this);
        elementalPlane.processUnits(this);

        /* ============= PROCESS MECHANICS ============= */
        /* Elemental calculates pressures and forces */
        elementalPlane.processMechanics(this);
        etherealPlane.processMechanics(this);

        /* ============= PROCESS TYPES ============= */
        elementalPlane.processTypes(this);
        etherealPlane.processTypes(this); /* Ethereal tries to take over type changes from Elemental */

        /* ============= POST PROCESS ============= */
        etherealPlane.postProcess(this);
        elementalPlane.postProcess(this); /* Elemental takes over finalised type changes from Ethereal */
        elementalPlane.debugPrint(this);
    }

    public void pushState(){
        etherealPlane.pushState();
        elementalPlane.pushState();
    }

    public void popState(){
        etherealPlane.popState();
        elementalPlane.popState();
    }

    public void doAction(SpellAction action, Vector3 offset){
        if(action.active()){
            /* not very punctual... */ /* TODO: Make Spells of different levels; Lower level mages shall use the less punctual algorithm */
//            if(action.aetherActive())
//                addAetherTo((action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedAether);
//            if(action.netherActive())
//                addNetherTo((action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedNether);
//            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
//                getEtherealPlane().setTargetRatio(
//                        (action.pos.x + offset.x),(int)(action.pos.y + offset.y), Material.ratioOf(action.targetElement)
//                );
//                getElementalPlane().setElement(
//                        (action.pos.x + offset.x), (int)(action.pos.y + offset.y), action.targetElement
//                );
//            }

            if(Material.Elements.Nothing != action.targetElement){ /* Most likely equalize action is attempted */
                tryToEqualize(
                    (int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y),
                    action.usedAether, action.usedNether, action.targetElement
                );
            }else{
                if(action.aetherActive())
                    addAetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedAether);
                if(action.netherActive())
                    addNetherTo((int)(action.pos.x + offset.x),(int)(action.pos.y + offset.y), action.usedNether);
            }
        }
    }

    /* TODO: Guard against modifications */
    public void provideScalarsTo(FloatBuffer[] inputs, int inputIndex){
        inputs[inputIndex] = scalars;
    }
    public void setScalars(FloatBuffer value){
        BufferUtils.copy(value, scalars);
    }
    public static float getUnit(int x,int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,0, buffer);
    }
    public float getUnit(int x, int y) {
        return getUnit(x,y, conf.getChunkBlockSize(),scalars);
    }
    public static void setUnit(int x,int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0, buffer, value);
    }
    public void setUnit(int x, int y, float value){
        setUnit(x,y, conf.getChunkBlockSize(),scalars,value);
    }
    public void offsetUnit(int x, int y, float value){
        BufferUtils.set(x,y, conf.getChunkBlockSize(),Config.bufferCellSize,0, scalars, (value + getUnit(x,y)));
    }
    private void addAetherTo(int x, int y, float value){
        etherealPlane.addAetherTo(x,y,value);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);
    }
    private void addNetherTo(int x, int y, float value){
        etherealPlane.addNether(x,y,value);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);
    }
    private void tryToEqualize(int x, int y, float aetherToUse, float netherToUse, Material.Elements target) {
        etherealPlane.tryToEqualize(x,y,aetherToUse,netherToUse, Material.ratioOf(target));
        elementalPlane.defineBy(etherealPlane);
        BufferUtils.copy(etherealPlane.determineUnits(this), scalars);
    }

    public EtherealAspect getEtherealPlane(){
        return etherealPlane;
    }
    public ElementalAspect getElementalPlane(){ return elementalPlane; }

    public Pixmap getWorldImage(){
        Pixmap worldImage = new Pixmap(conf.getChunkBlockSize(),conf.getChunkBlockSize(), Pixmap.Format.RGB888);
        for(int x = 0; x < conf.getChunkBlockSize(); ++x){
            for(int y = 0; y < conf.getChunkBlockSize(); ++y){
                Color finalColor;
//                finalColor = elementalPlane.getColor(x,(conf.getChunkBlockSize() - 1 - y),this);
                finalColor = elementalPlane.getDebugColor(x,(conf.getChunkBlockSize() - 1 - y), this);
                worldImage.drawPixel(x,y, Color.rgba8888(finalColor));
            }
        }
        return worldImage;
    }

    public Config getConf() {
        return conf;
    }

}
