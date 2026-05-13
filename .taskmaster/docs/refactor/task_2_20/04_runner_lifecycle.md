# 04. Runner Lifecycle 상세 계획

## 목적

Runner 등록, 조회, 삭제, 활성화 흐름을 Execution Context application service와 domain repository 기준으로 재정렬한다.

## AS-IS

```text
server/src/main/java/io/jgitkins/server/application/service/RunnerManagementService.java
server/src/main/java/io/jgitkins/server/application/service/RunnerReadService.java
server/src/main/java/io/jgitkins/server/application/mapper/RunnerApplicationMapper.java
server/src/main/java/io/jgitkins/server/application/support/RunnerRuntimeConfigProvider.java
server/src/main/java/io/jgitkins/server/application/exception/RunnerNotFoundException.java
server/src/main/java/io/jgitkins/server/application/dto/command/RunnerRegisterCommand.java
server/src/main/java/io/jgitkins/server/application/dto/result/RunnerRegistrationResult.java
server/src/main/java/io/jgitkins/server/application/dto/result/RunnerDetailResult.java
server/src/main/java/io/jgitkins/server/application/dto/result/RunnerActivateResult.java
server/src/main/java/io/jgitkins/server/application/dto/RunnerRuntimeConfig.java
server/src/main/java/io/jgitkins/server/application/dto/RunnerExecutionConfig.java
```

## TO-BE

```text
server/src/main/java/io/jgitkins/server/execution/application/service/RunnerManagementService.java
server/src/main/java/io/jgitkins/server/execution/application/service/RunnerReadService.java
server/src/main/java/io/jgitkins/server/execution/application/mapper/RunnerApplicationMapper.java
server/src/main/java/io/jgitkins/server/execution/application/support/RunnerRuntimeConfigProvider.java
server/src/main/java/io/jgitkins/server/execution/application/exception/RunnerNotFoundException.java
server/src/main/java/io/jgitkins/server/execution/application/contract/command/RunnerRegisterCommand.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/RunnerRegistrationResult.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/RunnerDetailResult.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/RunnerActivateResult.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/RunnerRuntimeConfig.java
server/src/main/java/io/jgitkins/server/execution/application/contract/result/RunnerExecutionConfig.java
```

## 책임

- `RunnerManagementService`: register/delete/activate
- `RunnerReadService`: load/list
- `RunnerRepository`: Runner aggregate 저장/조회/삭제
- `RunnerRuntimeConfigProvider`: activation response용 runtime config 조립
- `Runner` aggregate: token 검증, activation state transition

## RunnerManagementService 스니펫

```java
package io.jgitkins.server.execution.application.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunnerManagementService implements RunnerRegisterUseCase, RunnerDeleteUseCase, RunnerActivateUseCase {

    private final RunnerRepository runnerRepository;
    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRuntimeConfigProvider runtimeConfigProvider;

    @Override
    @Transactional
    public RunnerRegistrationResult register(RunnerRegisterCommand command) {
        Runner runner = Runner.create(command.description(), command.scopeType(), command.targetId());
        Runner savedRunner = runnerRepository.save(runner);
        log.info("Runner registered. runnerId={}", savedRunner.getId());
        return runnerApplicationMapper.toRegistrationResult(savedRunner);
    }

    @Override
    @Transactional
    public void deleteRunner(Long runnerId) {
        runnerRepository.findById(runnerId)
                .orElseThrow(() -> new RunnerNotFoundException(runnerId));
        runnerRepository.deleteById(runnerId);
    }

    @Override
    @Transactional
    public RunnerActivateResult activate(String token, String remoteIp) {
        Runner runner = runnerRepository.findByToken(token)
                .orElseThrow(() -> new RunnerNotFoundException("Runner not found"));

        Runner activated = runner.activate(token, remoteIp);
        Runner persisted = runnerRepository.save(activated);

        log.info("Runner activated. runnerId={}", persisted.getId());
        return new RunnerActivateResult(
                runtimeConfigProvider.createConfig(),
                RunnerExecutionConfig.defaultConfig());
    }
}
```

## RunnerReadService 스니펫

```java
package io.jgitkins.server.execution.application.service;

@Service
@RequiredArgsConstructor
public class RunnerReadService implements RunnerLoadUseCase {

    private final RunnerApplicationMapper runnerApplicationMapper;
    private final RunnerRepository runnerRepository;

    @Override
    @Transactional(readOnly = true)
    public RunnerDetailResult getRunner(Long runnerId) {
        Runner runner = runnerRepository.findById(runnerId)
                .orElseThrow(() -> new RunnerNotFoundException(runnerId));
        return runnerApplicationMapper.toActivationResult(runner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunnerDetailResult> getRunners() {
        return runnerRepository.findAll().stream()
                .map(runnerApplicationMapper::toActivationResult)
                .toList();
    }
}
```

## RunnerApplicationMapper 스니펫

```java
package io.jgitkins.server.execution.application.mapper;

@Component
public class RunnerApplicationMapper {

    public RunnerRegistrationResult toRegistrationResult(Runner runner) {
        return new RunnerRegistrationResult(runner.getId(), runner.getToken());
    }

    public RunnerDetailResult toActivationResult(Runner runner) {
        return new RunnerDetailResult(
                runner.getId(),
                runner.getDescription(),
                runner.getStatus(),
                runner.getScopeType(),
                runner.getScopeTargetId(),
                runner.getIpAddress(),
                runner.getLastHeartbeatAt(),
                runner.getCreatedAt());
    }
}
```

## 구현 순서

1. Runner command/result/config DTO를 execution contract로 이동한다.
2. `RunnerApplicationMapper`, `RunnerRuntimeConfigProvider`를 execution application으로 이동한다.
3. `RunnerManagementService`, `RunnerReadService`를 execution service로 이동한다.
4. `RunnerPersistencePort` 의존을 `RunnerRepository`로 바꾼다.
5. presentation controller import를 새 use case/DTO로 바꾼다.
6. 테스트 import를 이동한다.

## 테스트 기준

- register는 `Runner.create(...)`와 repository save를 호출한다.
- delete는 runner 존재 확인 후 delete한다.
- activate는 token으로 runner를 조회하고 `runner.activate(...)`를 호출한다.
- token missing/mismatch/already active domain exception은 재포장하지 않는다.
- load/list는 mapper 결과를 반환한다.

## 완료 기준

- runner lifecycle application code가 execution package에 있다.
- `RunnerPersistencePort` 참조가 제거됐다.
- `RunnerManagementServiceTest`, `RunnerReadServiceTest`가 통과한다.
