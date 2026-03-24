package io.jgitkins.server.domain.model.vo;

public enum OwnerType {
    USER,
    ORGANIZATION;

    public static OwnerType from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }
        if ("ORGANIZE".equals(normalized) || "ORG".equals(normalized)) {
            return ORGANIZATION;
        }
        return OwnerType.valueOf(normalized);
    }
}
