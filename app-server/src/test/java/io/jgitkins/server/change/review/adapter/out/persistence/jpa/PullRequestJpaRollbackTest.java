package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.change.review.adapter.out.persistence.PullRequestPersistenceAdapter;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.change.review.infrastructure.config.ChangeReviewPersistenceSelectorConfiguration;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** The rollback half of the change-review cutover contract. */
class PullRequestJpaRollbackTest {

    private static final String PROPERTY = PersistenceImplementationSelector
            .propertyName("app-server", "change-review-pull-request-reference");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PullRequestJpaSelectorTest.Stubs.class,
                    ChangeReviewPersistenceSelectorConfiguration.class);

    @Test
    void rollsBackPullRequestToMybatis() {
        runner.withPropertyValues(PROPERTY + "=jpa").run(context ->
                assertThat(context.getBean(PullRequestRepository.class))
                        .isInstanceOf(PullRequestJpaPersistenceAdapter.class));

        runner.withPropertyValues(PROPERTY + "=mybatis").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PullRequestRepository.class))
                    .isInstanceOf(PullRequestPersistenceAdapter.class);
            assertThat(context).doesNotHaveBean(PullRequestJpaPersistenceAdapter.class);
        });
    }

    @Test
    void removingTheSelectorEntirelyIsAlsoARollback() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PullRequestRepository.class))
                    .as("an operator who deletes the property rather than resetting it must land on "
                            + "MyBatis too, not on an unbound context")
                    .isInstanceOf(PullRequestPersistenceAdapter.class);
        });
    }
}
