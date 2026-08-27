package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizeJpaRepository extends JpaRepository<OrganizeJpaEntity, Long> {

    Optional<OrganizeJpaEntity> findByName(String name);

    /**
     * The membership-mutation lock.
     *
     * <p>{@link LockModeType#PESSIMISTIC_WRITE} is what reproduces the MyBatis mapper's
     * {@code for update} clause. Without it this method would compile, return the same type, satisfy
     * every unit test, and silently drop the serialization that
     * {@code OrganizeMembershipConcurrencyIntegrationTest} and the owner invariant depend on. That
     * exact failure is what {@code OrganizeLockContractMariaDbTest} exists to catch, and it is why
     * this is a locked query rather than a plain {@code findById}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrganizeJpaEntity o where o.id = :id")
    Optional<OrganizeJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
