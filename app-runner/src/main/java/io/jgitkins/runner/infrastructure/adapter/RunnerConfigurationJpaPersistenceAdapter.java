package io.jgitkins.runner.infrastructure.adapter;

import io.jgitkins.runner.application.port.out.RunnerConfigurationPort;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigFileJpaEntity;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigFileJpaRepository;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigJpaEntity;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerConfigJpaRepository;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerJpaEntity;
import io.jgitkins.runner.infrastructure.persistence.jpa.RunnerJpaRepository;
import io.jgitkins.runner.infrastructure.translator.RunnerDomainMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The runner's local configuration store, over JPA.
 *
 * <p>Replaces {@code RunnerConfigurationPersistenceAdapter}, which was the last MyBatis adapter in
 * the codebase: app-server selects JPA for all seven of its capability slices, so leaving app-runner
 * on MyBatis would have kept the whole dependency alive for one class. There is no selector here and
 * no second implementation to fall back to -- app-server's dual-provider mechanism existed to make a
 * rollback possible, and that rollback has been given up deliberately, so building one for app-runner
 * would be a switch with nothing on the other side of it.
 *
 * <p>The schema is untouched. The runner keeps its own H2 file at {@code ~/runner}, and an already
 * activated runner has to come back activated after the upgrade rather than needing to be re-linked
 * to the server, so these entities map the existing tables exactly as the generated mappers saw them.
 *
 * <p><strong>One behaviour is deliberately different.</strong> The MyBatis adapter wrote the
 * execution settings to RUNNER_CONFIG_FILE and then read only RUNNER_CONFIG back, so
 * {@code runnerImageName} and {@code jenkinsPluginConfig} were always null on the load path. Nothing
 * noticed while the process stayed up, because activation keeps the configuration it received from the
 * server in memory. After a restart the runner reloaded from disk, got a null image name, and every
 * job it then picked up asked Docker to create a container from no image. This adapter reads both
 * tables. The alternative -- porting the read as written -- would have preserved a defect whose only
 * symptom is that restarted runners fail every build.
 */
@Component
@RequiredArgsConstructor
public class RunnerConfigurationJpaPersistenceAdapter implements RunnerConfigurationPort {

    private final RunnerJpaRepository runnerRepository;
    private final RunnerConfigJpaRepository configRepository;
    private final RunnerConfigFileJpaRepository configFileRepository;
    private final RunnerDomainMapper domainEntityMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<RunnerConfiguration> loadConfiguration() {
        return runnerRepository.findFirstByOrderByIdAsc()
                               .map(runner -> domainEntityMapper.toDomain(runner, loadSettings(runner.getId())));
    }

    @Override
    @Transactional
    public void save(RunnerConfiguration configuration) {
        RunnerJpaEntity runner = upsertRunner(configuration);

        Long runnerId = runner.getId();
        domainEntityMapper.toRuntimeConfigMap(configuration)
                          .forEach((key, value) -> upsertRuntimeConfig(runnerId, key, value));
        domainEntityMapper.toExecutionConfigFileMap(configuration)
                          .forEach((filename, contents) -> upsertExecutionConfig(runnerId, filename, contents));
    }

    /**
     * Both settings tables as one key-to-value map.
     *
     * <p>RUNNER_CONFIG and RUNNER_CONFIG_FILE are split by value size, not by meaning: the domain
     * mapper reads every setting out of a single map, and the write path chooses a table per key. So
     * the read has to put them back together or the file-backed keys come back missing.
     *
     * <p>The two key sets are disjoint by construction. Scalar rows are applied second so that a
     * scalar wins if that ever stops being true, RUNNER_CONFIG being the table a new setting lands in
     * by default.
     */
    private Map<String, String> loadSettings(Long runnerId) {
        Map<String, String> settings = new HashMap<>();
        configFileRepository.findAllByRunnerId(runnerId)
                            .forEach(file -> settings.put(file.getFilename(), file.getContents()));
        configRepository.findAllByRunnerId(runnerId)
                        .forEach(config -> settings.put(config.getConfigKey(), config.getConfigValue()));
        return settings;
    }

    private RunnerJpaEntity upsertRunner(RunnerConfiguration configuration) {
        LocalDateTime now = LocalDateTime.now();
        RunnerJpaEntity runner = domainEntityMapper.toEntity(configuration);
        runner.setStatus("ACTIVE");
        runner.setUpdatedAt(now);

        Optional<RunnerJpaEntity> existing = runnerRepository.findFirstByOrderByIdAsc();
        if (existing.isPresent()) {
            // Re-activation keeps the row's identity and its original creation time; only the token
            // the server issued and the timestamps move.
            RunnerJpaEntity current = existing.get();
            runner.setId(current.getId());
            runner.setCreatedAt(current.getCreatedAt());
        } else {
            runner.setCreatedAt(now);
        }
        return runnerRepository.save(runner);
    }

    private void upsertRuntimeConfig(Long runnerId, String key, String value) {
        RunnerConfigJpaEntity entity = configRepository.findByRunnerIdAndConfigKey(runnerId, key)
                                                       .orElseGet(RunnerConfigJpaEntity::new);
        entity.setRunnerId(runnerId);
        entity.setConfigKey(key);
        entity.setConfigValue(value);
        entity.setUpdatedAt(LocalDateTime.now());
        configRepository.save(entity);
    }

    private void upsertExecutionConfig(Long runnerId, String filename, String contents) {
        RunnerConfigFileJpaEntity entity = configFileRepository.findByRunnerIdAndFilename(runnerId, filename)
                                                               .orElseGet(RunnerConfigFileJpaEntity::new);
        entity.setRunnerId(runnerId);
        entity.setFilename(filename);
        entity.setContents(contents);
        entity.setUpdatedAt(LocalDateTime.now());
        configFileRepository.save(entity);
    }
}
