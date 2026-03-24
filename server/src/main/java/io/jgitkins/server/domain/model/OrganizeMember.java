package io.jgitkins.server.domain.model;

import io.jgitkins.server.domain.exception.DomainException;
import io.jgitkins.server.domain.error.DomainErrorCode;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
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
            throw new DomainException(DomainErrorCode.ORGANIZE_MEMBER_INVALID,
                    "OrganizeMember requires organizeId, userId and role");
        }
        return new OrganizeMember(organizeId, userId, role, joinedAt != null ? joinedAt : LocalDateTime.now());
    }
}
