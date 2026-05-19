package io.jgitkins.server.collaboration.domain.vo;

/**
 * Organize member 역할 enum.
 */
public enum OrganizeMemberRole {
    OWNER,
    MAINTAINER,
    MEMBER;

    public static OrganizeMemberRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Organize member role is required");
        }
        return OrganizeMemberRole.valueOf(value.trim().toUpperCase());
    }
}
