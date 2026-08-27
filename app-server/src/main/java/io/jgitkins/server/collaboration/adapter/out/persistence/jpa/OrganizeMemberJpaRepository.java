package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizeMemberJpaRepository extends JpaRepository<OrganizeMemberJpaEntity, Long> {

    Optional<OrganizeMemberJpaEntity> findByOrganizeIdAndUserId(Long organizeId, Long userId);

    boolean existsByOrganizeIdAndUserId(Long organizeId, Long userId);

    void deleteByOrganizeIdAndUserId(Long organizeId, Long userId);

    List<OrganizeMemberJpaEntity> findAllByOrganizeId(Long organizeId);

    /**
     * Added for task 2.72: the repository context resolves a requester's organization ids to build
     * its visibility filter. The MyBatis path did the same read through the shared
     * {@code OrganizeMemberEntityMbgMapper}, so this keeps the coupling where it already was rather
     * than introducing a second mapping of {@code ORGANIZE_MEMBER} in another context.
     */
    List<OrganizeMemberJpaEntity> findAllByUserId(Long userId);

    /** Counts owners without loading the rows, matching the MyBatis count query. */
    @Query("select count(m) from OrganizeMemberJpaEntity m where m.organizeId = :organizeId and m.role = :role")
    long countByOrganizeIdAndRole(@Param("organizeId") Long organizeId, @Param("role") String role);
}
