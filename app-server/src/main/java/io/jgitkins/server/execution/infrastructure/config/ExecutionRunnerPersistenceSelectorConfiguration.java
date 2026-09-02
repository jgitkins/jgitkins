package io.jgitkins.server.execution.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.PersistenceImplementation;
import io.jgitkins.server.common.infrastructure.config.PersistenceImplementationSelector;
import io.jgitkins.server.execution.adapter.out.persistence.RunnerPersistenceAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.RunnerAssignmentJpaRepository;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.RunnerJpaPersistenceAdapter;
import io.jgitkins.server.execution.adapter.out.persistence.jpa.RunnerJpaRepository;
import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerAssignmentDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.support.RunnerDomainMapper;
import io.jgitkins.server.execution.adapter.out.persistence.translator.RunnerAssignmentEntityMbgMapper;
import io.jgitkins.server.execution.adapter.out.persistence.translator.RunnerEntityMbgMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Composition root for the execution-runner persistence technology.
 *
 * <p>Task 2.74. Separate from the execution-job selector even though both live in the execution context:
 * runners and jobs share no table, and a runner-registration regression should be rollback-able without
 * also reverting the job write path that carries the dispatch lock. One selector per set of tables is
 * what keeps a rollback proportionate to the failure.
 */
@Configuration
public class ExecutionRunnerPersistenceSelectorConfiguration {

    static final String MODULE_SLUG = "app-server";
    static final String CAPABILITY_SLUG = "execution-runner-reference";
    static final String PROPERTY_NAME =
            PersistenceImplementationSelector.propertyName(MODULE_SLUG, CAPABILITY_SLUG);

    public record ExecutionRunnerPersistenceSelection(PersistenceImplementation implementation) {
    }

    @Bean
    ExecutionRunnerPersistenceSelection executionRunnerPersistenceSelection(Environment environment) {
        return new ExecutionRunnerPersistenceSelection(
                PersistenceImplementationSelector.resolve(PROPERTY_NAME, environment.getProperty(PROPERTY_NAME)));
    }

    @Bean
    RunnerRepository runnerRepository(
            ExecutionRunnerPersistenceSelection selection,
            RunnerEntityMbgMapper runnerEntityMbgMapper,
            RunnerAssignmentEntityMbgMapper runnerAssignmentEntityMbgMapper,
            RunnerDomainMapper runnerDomainMapper,
            RunnerAssignmentDomainMapper runnerAssignmentDomainMapper,
            RunnerJpaRepository runnerJpaRepository,
            RunnerAssignmentJpaRepository runnerAssignmentJpaRepository) {
        return switch (selection.implementation()) {
            case MYBATIS -> new RunnerPersistenceAdapter(runnerEntityMbgMapper,
                    runnerAssignmentEntityMbgMapper, runnerDomainMapper, runnerAssignmentDomainMapper);
            case JPA -> new RunnerJpaPersistenceAdapter(runnerJpaRepository, runnerAssignmentJpaRepository);
        };
    }
}
