package com.crystalline.aether.services;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CPUBackend implements CalculationPlatform<BiConsumer<FloatBuffer[], FloatBuffer>, FloatBuffer>{
    private final ArrayList<FloatBuffer> outputs;
    private final ArrayList<BiConsumer<FloatBuffer[], FloatBuffer>> phases;
    private FloatBuffer[] inputs = null;

    public CPUBackend(){
        outputs = new ArrayList<>();
        phases = new ArrayList<>();
    }

    @Override
    public int addPhase(BiConsumer<FloatBuffer[], FloatBuffer> phase, int outputSize) {
        int phaseIndex = outputs.size();
        outputs.add(ByteBuffer.allocateDirect(Float.BYTES * outputSize).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());
        phases.add(phase);
        return phaseIndex;
    }

    @Override
    public void setInputs(FloatBuffer[] inputs) {
        this.inputs = inputs;
    }

    @Override
    public FloatBuffer getOutput(int index) {
        return outputs.get(index);
    }

    @Override
    public void runPhase(int index) {
        phases.get(index).accept(inputs, outputs.get(index));
    }
}
