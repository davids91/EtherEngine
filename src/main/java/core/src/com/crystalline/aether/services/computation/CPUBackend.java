package com.crystalline.aether.services.computation;

import com.badlogic.gdx.Gdx;
import com.crystalline.aether.services.utils.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.BiConsumer;

public class CPUBackend extends CalculationBackend<BiConsumer<FloatBuffer[], FloatBuffer>, FloatBuffer>{

    @Override
    public int addPhase(BiConsumer<FloatBuffer[], FloatBuffer> phase, int outputSize) {
        int phaseIndex = outputs.size();
        outputs.add(ByteBuffer.allocateDirect(Float.BYTES * outputSize).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());
        phases.add(phase);
        return phaseIndex;
    }

    @Override
    public void runPhase(int index) {
        phases.get(index).accept(inputs, outputs.get(index));
    }
}
