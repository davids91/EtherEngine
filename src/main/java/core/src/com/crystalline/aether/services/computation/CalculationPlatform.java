package com.crystalline.aether.services.computation;

/**
 * Calculation backend for game logic with configurable phases and inputs
 */
public interface CalculationPlatform<Program, Data> {
    /**
     * A calculation phase to do inside the GPU
     * @param phase the shader program to run
     * @param outputSize the size of the output, not in Bytes, but in number of elements
     * @return the index of the added calculation phase; it is to be used with @runPhase
     */
    int addPhase(Program phase, int outputSize) throws Exception;

    /**
     * Set the input array of the callable calculations phases. The inputs are not allocated
     * inside the function, the user must take care that enough bytes are allocated for the buffers
     * to be usable with the corresponding phases
     * @param inputs the buffers containing the input data
     */
    void setInputs(Data[] inputs);

    /**
     * Return with the output data of the calculation phase under the given index.
     * Each calculation phase has an output buffer assigned to it, which is updated every time
     * the phase is executed with the @runPhase or @runPhaseGetOutput functions
     * @param index the calculation phase to read the output from
     * @return the output of the calculation phase
     */
    Data getOutput(int index);

    /**
     * Execute the calculation phase on the given index
     * @param index the index as given by @addPhase
     */
    void runPhase(int index);
}
