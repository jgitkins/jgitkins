package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialJpaEntity, Long> {

    Optional<UserCredentialJpaEntity> findFirstByUserIdAndProviderOrderByIdDesc(Long userId, String provider);

    List<UserCredentialJpaEntity> findAllByUserIdAndProviderOrderByIdDesc(Long userId, String provider);

    void deleteByIdAndUserId(Long id, Long userId);
}
