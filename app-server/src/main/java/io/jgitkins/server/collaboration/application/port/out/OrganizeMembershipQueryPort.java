package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import java.util.List;
import java.util.Optional;

public interface OrganizeMembershipQueryPort {
    Optional<OrganizeMemberRole> findRoleByOrganizeIdAndUserId(Long organizeId, Long userId);
    long countOwnersByOrganizeId(Long organizeId);

    /**
     * Every organization the user belongs to, by id.
     *
     * <p>Exists because {@code repository} needs it to build a visibility filter and
     * {@code ORGANIZE_MEMBER} belongs to this context. Task 2.72 let {@code repository} read this
     * table through this context's mapper instead; the question is answered here now, and the caller
     * learns ids and nothing else.
     *
     * <p>Ids, not memberships. The caller filters repositories by owner and has no business with a
     * role, a join date, or a membership entity whose invariants this context enforces.
     *
     * <p>Empty for a null user, and for a user in no organization. Neither is an error --
     * {@code repository} calls this with the requester id, and an anonymous requester is null.
     *
     * <p>No duplicates, guaranteed by {@code UK_ORGANIZE_MEMBER_USER (ORGANIZE_ID, USER_ID)}: one
     * row per pair. The implementation projects the column rather than loading membership rows and
     * mapping them. What it answers is asserted in {@code OrganizeIdsByUserMariaDbTest}, over a real
     * database, because the uniqueness this relies on is the table's and not the code's.
     */
    List<Long> findOrganizeIdsByUserId(Long userId);
}
