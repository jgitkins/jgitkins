# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.10 RepositoryLifecycleService 분리 및 Load/Management 책임 재구성 계획서

### 배경 (왜?)
- 현재 `RepositoryLifecycleService`는 생성, 삭제, 단건 조회, 경로 조회, 목록 조회를 함께 담당한다.
- 변경 유스케이스와 조회 유스케이스가 한 클래스에 섞여 있어 응집도가 낮다.
- `RepositoryLoadUseCase`라는 포트 이름과 `get*` 메서드명, `RepositoryLifecycleService`라는 구현체 이름 사이의 표현도 일관되지 않는다.

### 목표 (Goals)
- `RepositoryLifecycleService`를 `RepositoryManagementService`로 rename한다.
- 조회 메서드는 신규 `RepositoryLoadService`로 이관한다.
- `RepositoryLoadUseCase` 구현 책임은 `RepositoryLoadService`가 맡는다.
- 포트와 메서드 명명은 `load*` 기준으로 정리한다.
- Controller와 DTO 계약은 유지한다.

### 용어 정리
- 서비스명, 포트명, 메서드명은 모두 `Load` 기준으로 통일한다.
- 즉, 조회 구현체는 `RepositoryLoadService`, 포트는 `RepositoryLoadUseCase`, 메서드는 `load*`를 사용한다.

### 범위 (Scope)
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryLifecycleService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryManagementService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryLoadService.java`
- **수정 대상**: `server/src/main/java/io/jgitkins/server/application/port/in/RepositoryLoadUseCase.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryManagementController.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/presentation/api/rest/RepositoryContentController.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/presentation/api/web/WebRepositoryController.java`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/application/service/RepositoryOverviewService.java`
- **수정 대상**: `server/src/test/java/io/jgitkins/server/application/service/RepositoryLifecycleServiceTest.java`
- **수정 대상**: `server/src/test/java/io/jgitkins/server/application/ArchitecturePackageConventionTest.java`
- **수정 제외 대상**: API URL, 응답 DTO, 인증 정책, `RepositoryLookupService` 내부 알고리즘은 변경하지 않는다.

### 방법 조사 및 선택
- **방안 1**: 클래스명만 `RepositoryManagementService`로 바꾸고 조회 메서드는 유지한다.
  장점은 변경 범위가 작다.
  단점은 책임 분리가 되지 않는다.
- **방안 2**: `RepositoryLifecycleService`를 `RepositoryManagementService`로 바꾸고, 조회 메서드를 `RepositoryLoadService`로 이관한다.
  장점은 책임과 이름이 명확해진다.
  단점은 호출부와 테스트 정리가 필요하다.
- **방안 3**: facade를 새로 두고 내부에서 management/load를 조합한다.
  장점은 외부 호출부 변경이 적다.
  단점은 facade가 다시 비대해질 수 있다.
- **선택 방안**: 방안 2를 선택한다.

### 메서드 명명 검토
- **안 1**: `get*` 유지.
  장점은 수정량이 적다.
  단점은 `RepositoryLoadUseCase`와 어울리지 않는다.
- **안 2**: `load*`로 변경.
  장점은 포트 이름과 메서드 의미가 맞춰진다.
  단점은 호출부 수정 범위가 넓어진다.
- **안 3**: `find*`로 변경.
  장점은 조회 의미가 익숙하다.
  단점은 Optional 반환 관례와 충돌한다.
- **선택 방안**: 안 2를 선택한다.

### 변경 목록
- `RepositoryLifecycleService`는 제거한다.
- `RepositoryManagementService`는 `RepositoryCreateUseCase`, `RepositoryDeleteUseCase`를 구현한다.
- `RepositoryLoadService`는 `RepositoryLoadUseCase`를 구현한다.
- `getRepository`는 `loadRepository`로 변경한다.
- `getRepositoryByPath`는 `loadRepositoryByPath`로 변경한다.
- `getRepositories`는 `loadRepositories`로 변경한다.
- `getRepositoriesByUsername`는 `loadRepositoriesByUsername`로 변경한다.
- `RepositoryManagementController`, `RepositoryContentController`, `WebRepositoryController`, `RepositoryOverviewService`의 호출부를 함께 변경한다.
- 테스트는 역할 기준으로 분리하거나 rename한다.

### 계획 (Plan)
- **단계 1**: `RepositoryLifecycleService` 메서드와 의존성을 Management/Load로 분리한다.
- **단계 2**: `RepositoryManagementService`와 `RepositoryLoadService`의 생성자와 구현 포트를 확정한다.
- **단계 3**: `RepositoryLoadUseCase`와 호출부 메서드명을 `load*`로 정리한다.
- **단계 4**: 테스트와 아키텍처 검증 코드를 rename 또는 분리한다.
- **단계 5**: 컴파일과 회귀 테스트 범위를 검증한다.

### 검증 기준
- 생성과 삭제 동작은 기존과 동일해야 한다.
- 단건 조회, 경로 조회, 목록 조회, 사용자별 목록 조회 결과는 기존과 동일해야 한다.
- `RepositoryLoadUseCase` 호출부 컴파일이 모두 통과해야 한다.
- Load Service는 상태 변경 포트를 직접 호출하지 않아야 한다.
- Management Service는 조회용 public 메서드를 가지지 않아야 한다.

### 개선 사항 점검
- **개선안 1**: 테스트를 `Management`와 `Load` 기준으로 분리한다.
- **개선안 2**: 포트 이름과 메서드 이름을 모두 `load` 기준으로 맞춘다.
- **개선안 3**: `RepositoryOverviewService`와 `RepositoryLoadService`의 경계는 후속 과제로 검토한다.
- **선택 개선안**: 개선안 1, 2를 반영한다.

### 기대효과 (Expected Benefits)
- 조회와 변경 책임이 분리되어 응집도가 높아진다.
- 클래스명, 포트명, 메서드명이 더 일관되게 정리된다.
- 테스트 책임도 명확해져 회귀 검증이 쉬워진다.

### 예시 (방안 2 기준)

#### AS-IS
```java
public class RepositoryLifecycleService implements RepositoryCreateUseCase,
        RepositoryLoadUseCase,
        RepositoryDeleteUseCase {

    public RepositoryResult create(RepositoryCreateCommand command) { ... }
    public RepositoryResult getRepository(Long repositoryId) { ... }
    public List<RepositoryResult> getRepositoriesByUsername(String username) { ... }
    public void deleteRepository(Long repositoryId) { ... }
}
```

#### TO-BE
```java
public class RepositoryManagementService implements RepositoryCreateUseCase, RepositoryDeleteUseCase {

    public RepositoryResult create(RepositoryCreateCommand command) { ... }
    public void deleteRepository(Long repositoryId) { ... }
}
```

```java
public class RepositoryLoadService implements RepositoryLoadUseCase {

    public RepositoryResult loadRepository(Long repositoryId) { ... }
    public RepositoryResult loadRepositoryByPath(String namespace, String repoName) { ... }
    public List<RepositoryResult> loadRepositories() { ... }
    public List<RepositoryResult> loadRepositoriesByUsername(String username) { ... }
}
```

```java
public interface RepositoryLoadUseCase {
    RepositoryResult loadRepository(Long repositoryId);
    RepositoryResult loadRepositoryByPath(String namespace, String repoName);
    List<RepositoryResult> loadRepositories();
    List<RepositoryResult> loadRepositoriesByUsername(String username);
}
```

### 주의사항
- **포맷팅 금지**: 계획 단계에서 포맷팅은 수행하지 않는다.
- **기존 기능 보장**: API 계약과 응답 DTO는 유지한다.
- **계획우선**: 문서 작성 단계에서는 구현하지 않는다.
- **호출부 누락 금지**: `RepositoryLoadUseCase` 사용처를 빠짐없이 반영한다.

### 결론 (추후작성)
