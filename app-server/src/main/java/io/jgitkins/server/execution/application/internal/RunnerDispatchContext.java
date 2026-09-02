package io.jgitkins.server.execution.application.internal;

public record RunnerDispatchContext(Long runnerId,
                                    JobDispatchScope dispatchScope,
                                    Long scopeTargetId) {
    public RunnerDispatchContext {
        if (dispatchScope == null) {
            throw new IllegalArgumentException("dispatchScope is required");
        }
        if (dispatchScope != JobDispatchScope.GLOBAL && scopeTargetId == null) {
            throw new IllegalArgumentException("scopeTargetId is required for scoped dispatch");
        }
    }
}
