package io.jgitkins.server.collaboration.domain.entity;

import io.jgitkins.server.domain.exception.DomainException;
import io.jgitkins.server.domain.error.DomainProblemSpec;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Organize 멤버 엔터티
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganizeMember {

    private final OrganizeId organizeId;
    private final UserId userId;
    private final OrganizeMemberRole role;
    private final LocalDateTime joinedAt;

    public static OrganizeMember create(OrganizeId organizeId,
            UserId userId,
            OrganizeMemberRole role,
            LocalDateTime joinedAt) {
        if (organizeId == null || userId == null || role == null) {
            throw new DomainException(DomainProblemSpec.ORGANIZE_MEMBER_INVALID,
                    "OrganizeMember requires organizeId, userId and role");
        }
        return new OrganizeMember(organizeId, userId, role, joinedAt != null ? joinedAt : LocalDateTime.now());
    }
}
