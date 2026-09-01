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
     * <p>Exists because {@code repository} needs it to build a visibility filter, and
     * {@code ORGANIZE_MEMBER} belongs to this context. Task 2.72 faced the same need and chose
     * between two options -- duplicate the {@code ORGANIZE_MEMBER} mapping inside {@code repository},
     * or let {@code repository} read this table through this context's mapper -- and took the second,
     * recording the choice on {@code OrganizeMemberJpaRepository#findAllByUserId} as keeping "the
     * coupling where it already was". This method is the third option: the question is answered by
     * the context that owns the table, and the caller learns ids and nothing else.
     *
     * <p>Ids, not memberships. The caller filters repositories by owner and has no business with a
     * role, a join date, or a membership entity whose invariants this context enforces.
     *
     * <p>Empty for a null user, and for a user in no organization. Neither is an error --
     * {@code repository} calls this with the requester id, and an anonymous requester is null.
     *
     * <p>No duplicates, guaranteed by {@code UK_ORGANIZE_MEMBER_USER (ORGANIZE_ID, USER_ID)}: one
     * row per pair. The JPA implementation projects the column; the MyBatis one reads rows through the
     * generated mapper and keeps its {@code distinct()}, because adding a hand-written select to a
     * generated mapper is not worth it for a list bounded by the organizations one person belongs to.
     * The two are asserted to agree in {@code OrganizeIdsByUserBothProvidersMariaDbTest}.
     */
    List<Long> findOrganizeIdsByUserId(Long userId);
}
