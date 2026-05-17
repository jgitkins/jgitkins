package io.jgitkins.runner.infrastructure.helper;

public final class RunnerActivationEndpointHelper {

    private RunnerActivationEndpointHelper() {
    }

    public static String build(String baseUrl) {
        String normalized = baseUrl;
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://" + normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/api/runners/activate";
    }
}
