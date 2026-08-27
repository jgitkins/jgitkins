package io.jgitkins.server.execution.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.JobRepositoryAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobHistoryJpaRepository;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobJpaRepository;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobJpaRepositoryAdapter;
import io.jgitkins.server.execution.domain.repository.JobRepository;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobEntityMbgMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Composition root for the execution-job persistence technology.
 *
 * <p>Task 2.73. The slice is {@code JobRepository} only. {@code JobDispatchQueryAdapter} shares the
 * same two tables but is a hand-written projection query rather than a mapped read, so it migrates
 * separately in task 2.76 under its own selector — splitting it out here would mean shipping a
 * half-translated query with no test to hold it.
 *
 * <p>That does leave the two adapters on different providers between 2.73 and 2.76, which is normally
 * the split state the atomic-slice rule forbids. It is safe only because the dispatch query is
 * read-only and takes its own row lock: it never observes a partially written job, and it cannot
 * interleave with the compare-and-append the way two writers on one table could. This exception is
 * stated here rather than assumed, and 2.76 closes it.
 */
@Configuration
public class ExecutionJobPersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "execution-job-reference";
    static final String PROPERTY_NAME =
            PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    public record ExecutionJobPersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    ExecutionJobPersistenceSelection executionJobPersistenceSelection(Environment environment) {
        return new ExecutionJobPersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    JobRepository jobRepository(
            ExecutionJobPersistenceSelection selection,
            JobEntityMbgMapper jobEntityMbgMapper,
            JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper,
            JobDomainMapper jobDomainMapper,
            JobJpaRepository jobJpaRepository,
            JobHistoryJpaRepository jobHistoryJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new JobRepositoryAdapter(
                    jobEntityMbgMapper, jobHistoryEntityMbgMapper, jobDomainMapper);
            case JPA -> new JobJpaRepositoryAdapter(jobJpaRepository, jobHistoryJpaRepository);
        };
    }
}
