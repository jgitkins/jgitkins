package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.change.review.adapter.out.persistence.PullRequestPersistenceAdapter;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.change.review.infrastructure.config.ChangeReviewPersistenceSelectorConfiguration;
import io.jgitkins.server.change.review.infrastructure.config.ChangeReviewPersistenceSelectorConfiguration.ChangeReviewPersistenceSelection;
import io.jgitkins.server.change.review.adapter.out.persistence.support.PullRequestDomainMapper;
import io.jgitkins.server.change.review.adapter.out.persistence.mapper.PullRequestEntityMbgMapper;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.common.infrastructure.exception.InvalidPersistenceSelectorException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/** Selection behaviour for the change-review capability. */
class PullRequestJpaSelectorTest {

    private static final String PROPERTY = PersistenceImplementationSelector
            .propertyName("app-server", "change-review-pull-request-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Stubs.class, ChangeReviewPersistenceSelectorConfiguration.class);

    @Test
    void defaultsToMybatisAndRejectsInvalidSelector() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ChangeReviewPersistenceSelection.class).implementation())
                    .isEqualTo(PersistenceImplementation.MYBATIS);
            assertThat(context.getBean(PullRequestRepository.class))
                    .isInstanceOf(PullRequestPersistenceAdapter.class);
        });

        runner.withPropertyValues(PROPERTY + "=hibernate").run(context -> {
            assertThat(context).hasFailed();
            assertThatThrownBy(() -> {
                throw context.getStartupFailure();
            }).rootCause().isInstanceOf(InvalidPersistenceSelectorException.class);
        });
    }

    @Test
    void selectsTheJpaAdapterWhenAsked() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PullRequestRepository.class))
                    .isInstanceOf(PullRequestJpaPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(PullRequestPersistenceAdapter.class);
        });
    }

    static class Stubs {
        @Bean
        PullRequestEntityMbgMapper pullRequestEntityMbgMapper() {
            return Mockito.mock(PullRequestEntityMbgMapper.class);
        }

        @Bean
        PullRequestDomainMapper pullRequestDomainMapper() {
            return Mockito.mock(PullRequestDomainMapper.class);
        }

        @Bean
        PullRequestJpaRepository pullRequestJpaRepository() {
            return Mockito.mock(PullRequestJpaRepository.class);
        }
    }
}
