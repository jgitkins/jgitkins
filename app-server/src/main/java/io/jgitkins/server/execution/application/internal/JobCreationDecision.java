package io.jgitkins.server.execution.application.internal;

public record JobCreationDecision(
        boolean creatable,
        String reason
) {

    public static JobCreationDecision create() {
        return new JobCreationDecision(true, null);
    }

    public static JobCreationDecision skip(String reason) {
        return new JobCreationDecision(false, reason);
    }

    public boolean isSkipped() {
        return !creatable;
    }
}
