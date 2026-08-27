package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data replacement for the MBG {@code RepositoryEntityMbgMapper} reads.
 *
 * <p>The visibility queries come in three shapes rather than one, because the MyBatis condition
 * composed its OR groups conditionally: an anonymous caller got only the public group, a caller with
 * no organizations got two groups, and a caller with organizations got three. Collapsing them into a
 * single JPQL query would need an {@code in} clause over a possibly-empty collection, which is
 * exactly the case JPQL leaves implementation-defined, so the branch is kept explicit and visible
 * instead of hidden behind a sentinel value.
 *
 * <p>The owner-type discriminators are parameters, not literals in the JPQL, so {@code OwnerType}
 * stays the single source of those strings.
 */
public interface RepositoryJpaRepository extends JpaRepository<RepositoryJpaEntity, Long> {

    Optional<RepositoryJpaEntity> findFirstByOwnerTypeAndOwnerIdAndPath(String ownerType, Long ownerId, String path);

    Optional<RepositoryJpaEntity> findFirstByOwnerTypeAndOwnerIdAndName(String ownerType, Long ownerId, String name);

    Optional<RepositoryJpaEntity> findFirstByClonePath(String clonePath);

    Optional<RepositoryJpaEntity> findFirstByPath(String path);

    long countByOwnerTypeAndOwnerId(String ownerType, Long ownerId);

    List<RepositoryJpaEntity> findAllByOwnerTypeAndOwnerIdOrderByUpdatedAtDesc(String ownerType, Long ownerId);

    List<RepositoryJpaEntity> findAllByOwnerTypeAndOwnerIdAndVisibilityOrderByUpdatedAtDesc(
            String ownerType, Long ownerId, String visibility);

    @Query("select distinct r from RepositoryJpaEntity r where r.visibility = :publicVisibility "
            + "order by r.updatedAt desc")
    List<RepositoryJpaEntity> findVisibleToAnonymous(@Param("publicVisibility") String publicVisibility);

    @Query("select distinct r from RepositoryJpaEntity r where r.visibility = :publicVisibility "
            + "or (r.ownerType = :userOwnerType and r.ownerId = :requesterId) "
            + "order by r.updatedAt desc")
    List<RepositoryJpaEntity> findVisibleToUser(@Param("publicVisibility") String publicVisibility,
                                                @Param("userOwnerType") String userOwnerType,
                                                @Param("requesterId") Long requesterId);

    @Query("select distinct r from RepositoryJpaEntity r where r.visibility = :publicVisibility "
            + "or (r.ownerType = :userOwnerType and r.ownerId = :requesterId) "
            + "or (r.ownerType = :organizationOwnerType and r.ownerId in :organizeIds) "
            + "order by r.updatedAt desc")
    List<RepositoryJpaEntity> findVisibleToUserInOrganizations(
            @Param("publicVisibility") String publicVisibility,
            @Param("userOwnerType") String userOwnerType,
            @Param("requesterId") Long requesterId,
            @Param("organizationOwnerType") String organizationOwnerType,
            @Param("organizeIds") List<Long> organizeIds);
}
