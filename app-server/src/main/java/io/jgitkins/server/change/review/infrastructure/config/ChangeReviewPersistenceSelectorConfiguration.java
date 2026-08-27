package io.jgitkins.server.change.review.infrastructure.config;

import io.jgitkins.server.change.review.adapter.out.persistence.PullRequestPersistenceAdapter;
import io.jgitkins.server.change.review.adapter.out.persistence.jpa.PullRequestJpaPersistenceAdapter;
import io.jgitkins.server.change.review.adapter.out.persistence.jpa.PullRequestJpaRepository;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.change.review.adapter.out.persistence.support.PullRequestDomainMapper;
import io.jgitkins.server.change.review.adapter.out.persistence.mapper.PullRequestEntityMbgMapper;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Composition root for the change-review persistence technology. Task 2.75. */
@Configuration
public class ChangeReviewPersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "change-review-pull-request-reference";
    static final String PROPERTY_NAME =
            PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    public record ChangeReviewPersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    ChangeReviewPersistenceSelection changeReviewPersistenceSelection(Environment environment) {
        return new ChangeReviewPersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    PullRequestRepository pullRequestRepository(
            ChangeReviewPersistenceSelection selection,
            PullRequestEntityMbgMapper pullRequestEntityMbgMapper,
            PullRequestDomainMapper pullRequestDomainMapper,
            PullRequestJpaRepository pullRequestJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new PullRequestPersistenceAdapter(
                    pullRequestEntityMbgMapper, pullRequestDomainMapper);
            case JPA -> new PullRequestJpaPersistenceAdapter(pullRequestJpaRepository);
        };
    }
}
