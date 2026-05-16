# Task 2.32 Detail 2: Contract와 Inbound Port 이관

### 목적
- 이 문서는 `RepositoryOverviewResult`와 `RepositoryOverviewUseCase`의 이동 절차를 정의한다.
- 목표는 Repository Overview의 application contract와 inbound port를 Repository Context 아래로 정렬하는 것이다.

### 이동 대상
```text
FROM server/src/main/java/io/jgitkins/server/application/dto/result/RepositoryOverviewResult.java
TO   server/src/main/java/io/jgitkins/server/repository/application/contract/result/RepositoryOverviewResult.java

FROM server/src/main/java/io/jgitkins/server/application/port/in/RepositoryOverviewUseCase.java
TO   server/src/main/java/io/jgitkins/server/repository/application/port/in/RepositoryOverviewUseCase.java
```

### `RepositoryOverviewResult` AS-IS
```java
package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import java.util.List;

public record RepositoryOverviewResult(
        RepositoryResult repository,
        List<BranchSearchResult> branches,
        List<FileEntry> tree,
        String selectedBranch,
        String role,
        boolean writable
) {
}
```

### `RepositoryOverviewResult` TO-BE
```java
package io.jgitkins.server.repository.application.contract.result;

import io.jgitkins.server.application.dto.FileEntry;
import java.util.List;

public record RepositoryOverviewResult(
        RepositoryResult repository,
        List<BranchSearchResult> branches,
        List<FileEntry> tree,
        String selectedBranch,
        String role,
        boolean writable
) {
}
```

### 주의점
- record component 이름을 바꾸지 않는다.
- JSON response field 이름이 바뀌면 web 화면과 API client가 깨질 수 있다.
- `FileEntry`는 이번 작업에서 유지한다.
- `RepositoryResult`와 `BranchSearchResult`는 같은 패키지에 있으므로 별도 import가 필요 없다.

### `RepositoryOverviewUseCase` AS-IS
```java
package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
```

### `RepositoryOverviewUseCase` 1차 TO-BE
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);
}
```

### `RepositoryOverviewUseCase` 2차 개선 후보
```java
package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;

public interface RepositoryOverviewUseCase {

    RepositoryOverviewResult getOverview(Long repositoryId, String branch);

    RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch);
}
```

### 1차와 2차를 나누는 이유
- 1차는 package 이동과 import 정리만 수행한다.
- 2차는 path 기반 조회 계약을 추가한다.
- 두 작업을 나누면 컴파일 실패 원인을 좁히기 쉽다.

### 호출부 import 변경
```java
// AS-IS
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
```

```java
// TO-BE
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
```

### 삭제 대상 확인
- 이관 후 old file은 남기지 않는다.
- old package에 동일 이름 타입이 남아 있으면 Spring bean wiring과 test mock이 혼재될 수 있다.

```bash
rg "io\\.jgitkins\\.server\\.application\\.port\\.in\\.RepositoryOverviewUseCase" server/src
rg "io\\.jgitkins\\.server\\.application\\.dto\\.result\\.RepositoryOverviewResult" server/src
```

### 완료 조건
- `RepositoryOverviewResult`는 `repository.application.contract.result`에만 존재한다.
- `RepositoryOverviewUseCase`는 `repository.application.port.in`에만 존재한다.
- controller와 service test는 새 package만 import한다.

