package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestJpaRepository extends JpaRepository<PullRequestJpaEntity, Long> {
}
