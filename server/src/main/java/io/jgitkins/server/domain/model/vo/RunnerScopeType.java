package io.jgitkins.server.domain.model.vo;

public enum RunnerScopeType {
    GLOBAL(false),
    ORGANIZE(true),
    REPOSITORY(true);

    private final boolean targetRequired;

    RunnerScopeType(boolean targetRequired) {
        this.targetRequired = targetRequired;
    }

    public boolean requiresTargetId() {
        return targetRequired;
    }
}
