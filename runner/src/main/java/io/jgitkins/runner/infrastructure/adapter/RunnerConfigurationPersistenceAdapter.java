package io.jgitkins.runner.infrastructure.adapter;

import io.jgitkins.runner.application.port.out.RunnerConfigurationPort;
import io.jgitkins.runner.domain.RunnerConfiguration;
import io.jgitkins.runner.infrastructure.mapper.RunnerDomainMapper;
import io.jgitkins.runner.infrastructure.persistence.mapper.RunnerConfigEntityMbgMapper;
import io.jgitkins.runner.infrastructure.persistence.mapper.RunnerConfigFileEntityMbgMapper;
import io.jgitkins.runner.infrastructure.persistence.mapper.RunnerEntityMbgMapper;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigEntity;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigEntityCondition;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigFileEntity;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerConfigFileEntityCondition;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerEntity;
import io.jgitkins.runner.infrastructure.persistence.model.RunnerEntityCondition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RunnerConfigurationPersistenceAdapter implements RunnerConfigurationPort {

    private final RunnerEntityMbgMapper runnerMapper;
    private final RunnerConfigEntityMbgMapper configMapper;
    private final RunnerConfigFileEntityMbgMapper configFileMapper;
    private final RunnerDomainMapper domainEntityMapper;

    @Override
    public Optional<RunnerConfiguration> loadConfiguration() {
        RunnerEntity runner = findRunner().orElse(null);
        if (runner == null) {
            return Optional.empty();
        }

        Map<String, String> configMap = loadConfigs(runner.getId());
        return Optional.of(mapToDomain(runner, configMap));
    }

    @Override
    public void save(RunnerConfiguration configuration) {
        RunnerEntity runner = domainEntityMapper.toEntity(configuration);
        Optional<RunnerEntity> existingRunner = findRunner();
        runner.setStatus("ACTIVE");
        runner.setUpdatedAt(LocalDateTime.now());
        if (existingRunner.isPresent()) {
            RunnerEntity current = existingRunner.get();
            runner.setId(current.getId());
            runner.setCreatedAt(current.getCreatedAt());
            runnerMapper.updateByPrimaryKeySelective(runner);
        } else {
            runner.setCreatedAt(LocalDateTime.now());
            runnerMapper.insertSelective(runner);
        }

        Long runnerId = runner.getId();
        Map<String, String> runtimeConfigMap = domainEntityMapper.toRuntimeConfigMap(configuration);
        Map<String, String> executionConfigMap = domainEntityMapper.toExecutionConfigFileMap(configuration);
        runtimeConfigMap.forEach((key, value) -> upsertRuntimeConfig(runnerId, key, value));
        executionConfigMap.forEach((filename, content) -> upsertExecutionConfig(runnerId, filename, content));

    }

    private Optional<RunnerEntity> findRunner() {
        RunnerEntityCondition condition = new RunnerEntityCondition();
        condition.setOrderByClause("ID ASC");
        List<RunnerEntity> runners = runnerMapper.selectByCondition(condition);
        if (runners.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(runners.get(0));
    }

    private Map<String, String> loadConfigs(Long runnerId) {
        RunnerConfigEntityCondition condition = new RunnerConfigEntityCondition();
        condition.createCriteria().andRunnerIdEqualTo(runnerId);
        return configMapper.selectByCondition(condition)
                           .stream()
                           .collect(Collectors.toMap(RunnerConfigEntity::getConfigKey,
                                                     RunnerConfigEntity::getConfigValue,
                                                     (existing, replacement) -> replacement));
    }

    private RunnerConfiguration mapToDomain(RunnerEntity runner, Map<String, String> configMap) {
        return domainEntityMapper.toDomain(runner, configMap);
    }

    private void upsertRuntimeConfig(Long runnerId, String key, String value) {
        RunnerConfigEntityCondition condition = new RunnerConfigEntityCondition();
        condition.createCriteria()
                 .andRunnerIdEqualTo(runnerId)
                 .andConfigKeyEqualTo(key);
        List<RunnerConfigEntity> current = configMapper.selectByCondition(condition);

        RunnerConfigEntity entity = new RunnerConfigEntity();
        entity.setRunnerId(runnerId);
        entity.setConfigKey(key);
        entity.setConfigValue(value);
        entity.setUpdatedAt(LocalDateTime.now());

        if (current.isEmpty()) {
            configMapper.insertSelective(entity);
        } else {
            entity.setId(current.get(0).getId());
            configMapper.updateByPrimaryKeySelective(entity);
        }
    }

    private void upsertExecutionConfig(Long runnerId, String filename, String contents) {
        RunnerConfigFileEntityCondition condition = new RunnerConfigFileEntityCondition();
        condition.createCriteria()
                 .andRunnerIdEqualTo(runnerId)
                 .andFilenameEqualTo(filename);
        List<RunnerConfigFileEntity> current = configFileMapper.selectByCondition(condition);

        RunnerConfigFileEntity entity = new RunnerConfigFileEntity();
        entity.setRunnerId(runnerId);
        entity.setFilename(filename);
        entity.setContents(contents);
        entity.setUpdatedAt(LocalDateTime.now());

        if (current.isEmpty()) {
            configFileMapper.insertSelective(entity);
        } else {
            entity.setId(current.get(0).getId());
            configFileMapper.updateByPrimaryKeySelective(entity);
        }
    }
}
