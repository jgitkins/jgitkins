package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityJpaEntity, Long> {

    Optional<UserIdentityJpaEntity> findFirstByProviderNameAndProviderSubOrderByIdDesc(
            String providerName, String providerSub);

    List<UserIdentityJpaEntity> findAllByUserIdOrderByIdDesc(Long userId);
}
