package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    /** Newest-first, matching the MyBatis {@code order by id desc limit 1} on the same lookups. */
    Optional<UserJpaEntity> findFirstByEmailOrderByIdDesc(String email);

    Optional<UserJpaEntity> findFirstByUsernameOrderByIdDesc(String username);

    List<UserJpaEntity> findAllByOrderByIdDesc();

    /**
     * The activation lock, reproducing {@code selectByPrimaryKeyForUpdate}.
     *
     * <p>{@code PESSIMISTIC_WRITE} is load-bearing, not decorative: without it this compiles, returns
     * the same type, and passes every unit test while dropping the serialization that concurrent
     * activation depends on.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.id = :id")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
