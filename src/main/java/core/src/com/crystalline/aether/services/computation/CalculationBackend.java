package com.crystalline.aether.services.computation;

import java.util.ArrayList;

public abstract class CalculationBackend<Program, Data> implements CalculationPlatform<Program, Data>{
    protected final ArrayList<Program> phases;
    protected final ArrayList<Data> outputs;
    protected Data[] inputs = null;
    public CalculationBackend(){
        outputs = new ArrayList<>();
        phases = new ArrayList<>();
    }

    @Override
    public void setInputs(Data[] inputs) {
        this.inputs = inputs;
    }

    @Override
    public Data getOutput(int index) {
        return outputs.get(index);
    }

}
