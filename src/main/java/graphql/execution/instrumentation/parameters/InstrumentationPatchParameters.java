package graphql.execution.instrumentation.parameters;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.Patch;
import graphql.execution.instrumentation.InstrumentationState;

import java.util.function.Supplier;

public class InstrumentationPatchParameters {
    private final Patch patch;
    private final InstrumentationState instrumentationState;
    private final ExecutionContext executionContext;
    private final Supplier<ExecutionStepInfo> executionStepInfo;

    public InstrumentationPatchParameters(ExecutionContext executionContext, Supplier<ExecutionStepInfo> executionStepInfo, Patch patch, InstrumentationState instrumentationState) {
        this.patch = patch;
        this.instrumentationState = instrumentationState;
        this.executionContext = executionContext;
        this.executionStepInfo = executionStepInfo;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     *
     * @return a new parameters object with the new state
     */
    public InstrumentationPatchParameters withNewState(InstrumentationState instrumentationState) {
        return new InstrumentationPatchParameters(executionContext, executionStepInfo, patch, instrumentationState);
    }

    public Patch getPatch() {
        return patch;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo.get();
    }

    public <T extends InstrumentationState> T getInstrumentationState() {
        //noinspection unchecked
        return (T) instrumentationState;
    }
}
