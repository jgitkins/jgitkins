package io.jgitkins.server.execution.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.JobDispatchQueryAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobDispatchJpaQueryAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobDispatchJpaRepository;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.JobHistoryJpaRepository;
import io.jgitkins.server.execution.application.port.out.JobDispatchQueryPort;
import io.jgitkins.server.execution.infrastructure.mapper.JobDomainMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobDispatchQueryMapper;
import io.jgitkins.server.execution.infrastructure.persistence.mapper.JobHistoryEntityMbgMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Composition root for the job-dispatch query technology.
 *
 * <p>Task 2.76. This closes the split that task 2.73 opened and documented: the job write path moved to
 * JPA while the dispatch query stayed on MyBatis, because the query is hand-written SQL rather than a
 * mapped read and needed its own translation and its own evidence.
 *
 * <p>It keeps its own property rather than joining the execution-job selector. The two are genuinely
 * independent failure modes — a bad projection translation returns the wrong job, a bad write
 * translation loses a transition — and an operator should be able to roll back the one that broke.
 * Task 2.77 is where both are retired together, not here.
 */
@Configuration
public class ExecutionJobDispatchQuerySelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "execution-job-dispatch-query-reference";
    static final String PROPERTY_NAME =
            PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    public record ExecutionJobDispatchQuerySelection(PersistenceImplementation implementation) {
    }

    @Bean
    ExecutionJobDispatchQuerySelection executionJobDispatchQuerySelection(Environment environment) {
        return new ExecutionJobDispatchQuerySelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    JobDispatchQueryPort jobDispatchQueryPort(
            ExecutionJobDispatchQuerySelection selection,
            JobDispatchQueryMapper jobDispatchQueryMapper,
            JobHistoryEntityMbgMapper jobHistoryEntityMbgMapper,
            JobDomainMapper jobDomainMapper,
            JobDispatchJpaRepository jobDispatchJpaRepository,
            JobHistoryJpaRepository jobHistoryJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new JobDispatchQueryAdapter(
                    jobDispatchQueryMapper, jobHistoryEntityMbgMapper, jobDomainMapper);
            case JPA -> new JobDispatchJpaQueryAdapter(jobDispatchJpaRepository, jobHistoryJpaRepository);
        };
    }
}
