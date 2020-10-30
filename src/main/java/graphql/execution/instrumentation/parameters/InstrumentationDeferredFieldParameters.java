package graphql.execution.instrumentation.parameters;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.instrumentation.InstrumentationState;

import java.util.function.Supplier;

/**
 * Parameters sent to {@link graphql.execution.instrumentation.Instrumentation} methods
 */
public class InstrumentationDeferredFieldParameters extends InstrumentationFieldParameters {

    private final ExecutionStrategyParameters executionStrategyParameters;

    public InstrumentationDeferredFieldParameters(ExecutionContext executionContext, ExecutionStrategyParameters executionStrategyParameters, Supplier<ExecutionStepInfo> executionStepInfo) {
        this(executionContext, executionStrategyParameters, executionContext.getInstrumentationState(), executionStepInfo);
    }

    InstrumentationDeferredFieldParameters(ExecutionContext executionContext, ExecutionStrategyParameters executionStrategyParameters, InstrumentationState instrumentationState, Supplier<ExecutionStepInfo> executionStepInfo) {
        super(executionContext, executionStepInfo, instrumentationState);
        this.executionStrategyParameters = executionStrategyParameters;
    }

    /**
     * Returns a cloned parameters object with the new state
     *
     * @param instrumentationState the new state for this parameters object
     *
     * @return a new parameters object with the new state
     */
    @Override
    public InstrumentationDeferredFieldParameters withNewState(InstrumentationState instrumentationState) {
        return new InstrumentationDeferredFieldParameters(
                this.getExecutionContext(), this.executionStrategyParameters, instrumentationState, this::getExecutionStepInfo);
    }

    public ExecutionStrategyParameters getExecutionStrategyParameters() {
        return executionStrategyParameters;
    }
}
