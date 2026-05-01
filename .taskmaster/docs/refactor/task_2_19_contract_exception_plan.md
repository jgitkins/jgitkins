# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Repository Context Contract / Exception 이관 계획서

### 배경 (왜?)
- `repository` 패키지로 서비스와 포트는 이동했지만, 여전히 다수의 입력/출력 타입과 예외가 `application.dto`, `application.exception`에 남아 있다.
- 이 상태는 동작상 문제는 없지만, Repository Context가 어떤 계약을 소유하는지 코드 구조에 드러나지 않는다.
- 특히 `RepositoryCreateCommand`, `RepositoryResult`, `BranchSearchResult`, `RepositoryNotFoundException`은 사실상 Repository Context 전용인데, 현재 위치만 보면 전역 application 공용 타입처럼 읽힌다.

### 목표 (Goals)
- Repository Context가 소유하는 입력/출력 계약과 예외를 분리한다.
- 진짜 전역 공용 타입과 Repository 전용 타입을 구분한다.
- presentation 계층과 application/service 계층에 미치는 충격을 최소화하는 점진 이관 순서를 정의한다.

### 용어 정리
- `DTO`는 Data Transfer Object의 약자다.
- `Data`는 데이터, `Transfer`는 전송/이동, `Object`는 객체를 뜻한다.
- 즉 DTO는 원래 “계층이나 프로세스 사이에서 데이터를 옮기기 위한 객체”라는 의미다.
- `Contract`는 계약이라는 뜻이다.
- 코드에서 contract는 “이 계층/컨텍스트가 외부에 약속하는 입력과 출력의 형태”를 뜻한다.
- 이번 케이스의 `RepositoryCreateCommand`, `RepositoryResult`는 단순 전송 객체이기도 하지만, 더 중요한 성격은 “Repository Context의 use case 계약”이다.

### 왜 `dto`가 아니라 `contract`인가?
- `dto`라고 두면 “그냥 데이터 담는 객체”라는 의미가 강하다.
- `contract`라고 두면 “use case, port, adapter가 맞춰야 하는 명시적 경계”라는 의미가 더 선명하다.
- 현재 대상 타입들은 내부 엔티티 대체물이 아니라, controller/use case/application service 사이에서 오가는 호출 시그니처다.
- 따라서 이관 목적이 “클래스 이동”이 아니라 “Repository Context 계약의 소유권 명시”라면 `contract`가 더 맞다.

### 업계에서 어떻게 쓰는가?
- 전통적인 Spring MVC 프로젝트는 `dto`, `request`, `response`, `payload`를 많이 쓴다.
- Hexagonal/Clean Architecture 성향이 강한 팀은 `command`, `query`, `result`, `contract`, `model`을 더 엄격히 나눠 쓴다.
- 외부 API 스펙 중심 팀은 `api.model`, `api.contract`, `schema` 같은 이름을 쓴다.
- 이벤트 중심 시스템은 `message`, `event`, `payload`를 쓴다.
- 즉 `dto`가 틀린 표현은 아니지만, 컨텍스트 경계를 드러내려는 리팩토링에서는 `contract`가 더 설명력이 높다.

### 방법 조사 및 선택
- **방안 1**: `application.dto` 유지
  - 변경 비용은 가장 낮다.
  - 단점은 Repository Context 전용 계약과 전역 공용 계약이 계속 섞인다.
- **방안 2**: `repository.application.contract` 도입
  - 컨텍스트 소유권이 드러난다.
  - port/use case와 함께 읽기 좋다.
  - 이번 계획의 채택안이다.
- **방안 3**: `presentation.request/response`와 `service.command/result`로 더 잘게 분리
  - 장기적으로는 가장 정교하다.
  - 다만 현재 코드베이스에 비해 변화 폭이 크다.

### 선택 방안
- **채택안**: 방안 2
- `repository.application.contract.command`
- `repository.application.contract.result`
- `repository.application.exception`

### 범위 (Scope)
- **이동 후보 command**
  - `RepositoryCreateCommand`
  - `BranchCreateCommand`
  - `BranchCreationContext`
  - `RepositoryMemberAddCommand`
- **이동 후보 result**
  - `RepositoryResult`
  - `BranchSearchResult`
  - `RepositoryMemberSummary`
- **판단 보류 contract**
  - `CommitFile`
  - `CommitHistory`
- **이동 후보 exception**
  - `RepositoryNotFoundException`
  - `BranchNotFoundException`
  - `BranchAlreadyExistsException`
  - `SourceBranchNotFoundException`
  - `CommitNotFoundException`
- **유지 대상 exception**
  - `UserNotFoundException`

### 분류 기준
- Repository 생성/조회/브랜치/멤버 흐름에서만 쓰이면 Repository Context contract 후보로 본다.
- 여러 context가 함께 참조하거나 공통 조회 의미가 있으면 `application` 유지 후보로 본다.
- Git adapter 내부 세부 동작에 치우친 타입은 `repository.infrastructure.git.contract` 후보로 별도 검토한다.

### 목표 패키지 방향
- `repository.application.contract.command`
- `repository.application.contract.result`
- `repository.application.exception`

### TO-BE 구조
```text
server/src/main/java/io/jgitkins/server/repository/
  application/
    contract/
      command/
        RepositoryCreateCommand.java
        BranchCreateCommand.java
        BranchCreationContext.java
        RepositoryMemberAddCommand.java
      result/
        RepositoryResult.java
        BranchSearchResult.java
        RepositoryMemberSummary.java
    exception/
      RepositoryNotFoundException.java
      BranchNotFoundException.java
      BranchAlreadyExistsException.java
      SourceBranchNotFoundException.java
      CommitNotFoundException.java
```

### 상세 계획
1. command/result/exception 사용처를 전부 수집한다.
2. presentation controller가 직접 import하는 타입과 use case/service만 import하는 타입을 분리한다.
3. Repository Context 전용 타입부터 `repository.application.contract`와 `repository.application.exception`으로 이동한다.
4. presentation 계층 import를 새 패키지로 맞춘다.
5. `UserNotFoundException`, `CommitFile`, `CommitHistory`는 유지 또는 별도 후보로 남긴다.
6. 전체 테스트와 아키텍처 검증을 다시 수행한다.

### 단계별 이관 순서
1. `result` 계열부터 이동한다.
   - 읽기 전용이고 부작용이 적다.
2. `command` 계열을 이동한다.
   - controller, use case, service import를 함께 수정한다.
3. `Repository/Branch` 전용 exception을 이동한다.
   - presentation advice, service, adapter import를 함께 수정한다.
4. `CommitFile`, `CommitHistory`는 마지막에 재판단한다.

### 검증 기준
- controller 시그니처와 JSON 직렬화 결과가 변하지 않아야 한다.
- service/use case 메서드 시그니처 의미가 유지되어야 한다.
- Repository Context 외부에서 Repository 전용 exception을 잘못 참조하지 않아야 한다.
- import 순환이 생기지 않아야 한다.

### 예시 코드

#### 1. Command 이동 예시
```java
package io.jgitkins.server.repository.application.contract.command;

import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import lombok.Builder;

@Builder
public record RepositoryCreateCommand(
        String repoName,
        OwnerType ownerType,
        Long organizeId,
        String authorName,
        String authorEmail,
        String mainBranch,
        RepositoryVisibility visibility,
        String description,
        String credentialId,
        boolean readme,
        String message
) {
}
```

```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

public interface RepositoryCreateUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
}
```

#### 2. Result 이동 예시
```java
package io.jgitkins.server.repository.application.contract.result;

import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import java.time.LocalDateTime;

public record RepositoryResult(
        Long id,
        OwnerType ownerType,
        String name,
        String path,
        String defaultBranch,
        RepositoryVisibility visibility,
        String description,
        String clonePath,
        String credentialId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean requiresInitialContent,
        LocalDateTime lastSyncedAt,
        Long ownerId,
        Long organizeId
) {
}
```

```java
package io.jgitkins.server.presentation.api.rest;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RepositoryManagementController {

    private final RepositoryLoadUseCase repositoryLoadUseCase;

    @GetMapping("/api/repositories/{repositoryId}")
    public RepositoryResult load(@PathVariable Long repositoryId) {
        return repositoryLoadUseCase.loadRepository(repositoryId);
    }
}
```

#### 3. Exception 이동 예시
```java
package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.common.exception.ErrorCode;

public class RepositoryNotFoundException extends ApplicationException {

    public RepositoryNotFoundException(Long repositoryId) {
        super(ErrorCode.REPOSITORY_NOT_FOUND, "Repository not found. repositoryId=" + repositoryId);
    }

    public RepositoryNotFoundException(String namespace, String repositoryName) {
        super(ErrorCode.REPOSITORY_NOT_FOUND,
              "Repository not found. namespace=" + namespace + ", repositoryName=" + repositoryName);
    }
}
```

```java
package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryPersistencePort repositoryPort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        return null;
    }
}
```

### 보류 항목 판단
- `UserNotFoundException`
  - Repository 조회 흐름에서 쓰더라도 소유권은 User 조회 쪽에 더 가깝다.
  - 이번 이동 대상에서 제외한다.
- `CommitFile`, `CommitHistory`
  - application contract인지 git adapter contract인지 아직 애매하다.
  - 먼저 Repository service/exception 정리 후 별도 판단한다.

### 기대효과
- Repository Context가 외부에 어떤 계약을 노출하는지 구조상 명확해진다.
- `application.dto`를 공용 쓰레기통처럼 키우는 흐름을 막을 수 있다.
- 이후 `execution`, `change-review`, `identity-access`도 같은 방식으로 정리하기 쉬워진다.
