# Task 2.35 Plan: server/application 잔재 bounded context 분류

### 목적
- `server/application` 은 더 이상 하나의 공용 application layer 로 남기지 않는다.
- top-level `application` 에 남은 객체를 seam 기준으로 `shared`, `repository`, `change/review`, `execution`, `identity/access` 로 분류한다.
- 이번 작업의 목표는 package tree 가 “무엇이 공통이고 무엇이 컨텍스트 소유인지”를 숨기지 않게 만드는 것이다.

### Scope Update
- 이번 pass 는 `server/application` 만 다룬다.
- `domain` 과 `presentation` 은 별도 task 로 처리한다.
- infrastructure 객체나 controller routing 은 이번 pass 에서 손대지 않는다.

### 배경
- `application` 아래에는 common/error, dto, exception, mapper, port, service, support, validate 가 혼재해 있다.
- 일부 타입은 여러 bounded context 에서 동시에 쓰이는 shared primitive 이고, 일부는 repository/change-review/execution/identity-access 전용이다.
- 현재 형태는 책임 경계를 패키지명만 보고는 식별하기 어렵다.

### 분류 원칙
1. 여러 context 가 공유하는 base exception, error spec, command, helper 는 `shared/application` 또는 `shared/common` 으로 보낸다.
2. repository 전용 조회/파일/커밋/생성 DTO 는 repository application 으로 보낸다.
3. merge / pull-request 성격의 타입은 change/review application 으로 보낸다.
4. push event / job dispatch 성격의 타입은 execution application 으로 보낸다.
5. authentication / credential 성격의 타입은 identity/access application 으로 보낸다.
6. `server/application` 은 migration 완료 후 empty 혹은 compatibility shell 이 되는 것을 목표로 한다.

### current -> target inventory

#### 1) application/common
| Current file | 성격 | Target package |
|---|---|---|
| `common/error/ApplicationErrorCode.java` | application-wide error code | `io.jgitkins.server.shared.application.error` |
| `common/error/ApplicationProblemSpec.java` | application-wide problem spec | `io.jgitkins.server.shared.application.error` |
| `common/event/DomainEventPublisher.java` | domain event publication seam | `io.jgitkins.server.shared.application.event` |
| `common/GitConstants.java` | git ref prefix / shared constant | `io.jgitkins.server.shared.common` |

#### 2) application/dto
| Current file | 성격 | Target package |
|---|---|---|
| `dto/BranchInfo.java` | repository branch read model | `repository.application.contract.result` |
| `dto/CommitFile.java` | commit tree write helper | `repository.application.contract.result` |
| `dto/CommitHistory.java` | commit read model | `repository.application.contract.result` |
| `dto/FileEntry.java` | file tree read model | `repository.application.contract.result` |
| `dto/FileIndexEntry.java` | presentation projection | `repository.presentation.dto` |
| `dto/FileUploadInfo.java` | repository upload command | `repository.application.contract.command` |
| `dto/FileUploadRequest.java` | multipart request wrapper | `repository.presentation.dto` |
| `dto/MergeRequest.java` | merge command | `change.review.application.dto.command` |
| `dto/RepositoryCreationContext.java` | repository creation context | `repository.application.contract.internal` |
| `dto/RepositoryKey.java` | repository path resolution helper | `repository.application.contract.internal` |
| `dto/result/MergeResult.java` | merge evaluation result | `change.review.application.dto.result` |
| `dto/result/UserCredentialSummary.java` | PAT summary read model | `identity.access.application.dto.result` |
| `dto/command/PushEventCommand.java` | push execution command | `shared.application.command` |
| `dto/command/PushHookRequest.java` | hook payload command | `shared.application.command` |
| `dto/command/UpdateRepositoryCommand.java` | repository mutation command | `repository.application.contract.command` |

#### 3) application/exception
| Current file | 성격 | Target package |
|---|---|---|
| `exception/ApplicationException.java` | shared base exception | `shared.application.exception` |
| `exception/InvalidNamespaceException.java` | repository namespace validation | `repository.application.exception` |
| `exception/InvalidOwnerContextException.java` | repository owner validation | `repository.application.exception` |
| `exception/MemberIdentifierRequiredException.java` | repository member validation | `repository.application.exception` |
| `exception/RepositoryAccessDeniedException.java` | repository access policy failure | `repository.application.exception` |
| `exception/RepositoryAlreadyExistsException.java` | repository creation conflict | `repository.application.exception` |
| `exception/RepositoryNotInitializedException.java` | repository init guard | `repository.application.exception` |
| `exception/UnauthenticatedException.java` | generic auth failure | `shared.application.exception` |
| `exception/UsernameAlreadyExistsException.java` | identity/access signup conflict | `identity.access.application.exception` |
| `exception/UserNotFoundException.java` | identity/access lookup failure | `identity.access.application.exception` |

#### 4) application/mapper
| Current file | 성격 | Target package |
|---|---|---|
| `mapper/BranchApplicationMapper.java` | repository contract mapper | `repository.application.mapper` |
| `mapper/RepositoryApplicationMapper.java` | repository contract mapper | `repository.application.mapper` |

#### 5) application/port/in
| Current file | 성격 | Target package |
|---|---|---|
| `port/in/CommitLoadUseCase.java` | repository read use case | `repository.application.port.in` |
| `port/in/FileLoadUseCase.java` | repository file read use case | `repository.application.port.in` |
| `port/in/FileTreeLoadUseCase.java` | repository tree read use case | `repository.application.port.in` |
| `port/in/FileUploadUseCase.java` | repository file write use case | `repository.application.port.in` |
| `port/in/GitRepositoryAccessUseCase.java` | repository access seam | `repository.application.port.in` |
| `port/in/MergeabilityCheckUseCase.java` | merge review seam | `change.review.application.port.in` |
| `port/in/MergeabilityEvaluationUseCase.java` | merge review seam | `change.review.application.port.in` |
| `port/in/MergeUseCase.java` | merge execution seam | `change.review.application.port.in` |
| `port/in/PushEventHandleUseCase.java` | push/job execution seam | `execution.application.port.in` |

#### 6) application/port/out
| Current file | 성격 | Target package |
|---|---|---|
| `port/out/FileGitPort.java` | repository git adapter seam | `repository.application.port.out` |
| `port/out/MergeGitPort.java` | merge adapter seam | `change.review.application.port.out` |
| `port/out/PushEventRequestResolver.java` | hook payload resolver seam | `shared.application.port.out` |
| `port/out/RuntimeConfigPort.java` | runner/runtime config seam | `execution.application.port.out` |

#### 7) application/service
| Current file | 성격 | Target package |
|---|---|---|
| `service/CommitService.java` | repository read service | `repository.application.service` |
| `service/MergeService.java` | merge review service | `change.review.application.service` |
| `service/PushEventHandleService.java` | push event orchestration | `execution.application.service` |
| `service/RepositoryFileService.java` | repository file service | `repository.application.service` |

#### 8) application/support
| Current file | 성격 | Target package |
|---|---|---|
| `support/change/BranchChangeRecorder.java` | shared push-change recorder | `shared.application.support.change` |
| `support/CloneUrlBuilder.java` | repository URL helper | `repository.application.support` |
| `support/PushEventCommandResolver.java` | push command seam | `shared.application.support` |

#### 9) application/validate
| Current file | 성격 | Target package |
|---|---|---|
| `validate/BranchCreationValidator.java` | repository branch validation | `repository.application.validate` |
| `validate/RepositoryAccessValidator.java` | repository access validation | `repository.application.validate` |
| `validate/RepositoryMemberValidator.java` | repository member validation | `repository.application.validate` |
| `validate/RepositoryValidator.java` | repository creation/update validation | `repository.application.validate` |

### test inventory
`server/application` 아래 테스트도 함께 분류한다. 실제 이관 시에는 source code 와 같은 seam 기준을 따른다.

| Current file | 성격 | Target package |
|---|---|---|
| `ArchitecturePackageConventionTest.java` | package policy guard | `io.jgitkins.server.application` 유지 |
| `factory/CommitFileFactoryTest.java` | common factory test | `io.jgitkins.server.common.factory` |
| `service/BranchLoadServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/BranchManagementServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/CommitServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/PushEventHandleServiceIntegrationTest.java` | execution service integration test | `io.jgitkins.server.execution.application.service` |
| `service/PushEventHandleServiceTest.java` | execution service test | `io.jgitkins.server.execution.application.service` |
| `service/RepositoryFileServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/RepositoryLoadServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/RepositoryManagementServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `service/RepositoryMemberServiceTest.java` | repository service test | `io.jgitkins.server.repository.application.service` |
| `support/change/BranchChangeRecorderTest.java` | shared change recorder test | `io.jgitkins.server.shared.application.support.change` |
| `support/change/MergeabilityAssessmentAssemblerTest.java` | shared mergeability assembler test | `io.jgitkins.server.shared.application.change` |
| `support/PushEventCommandResolverTest.java` | shared push command resolver test | `io.jgitkins.server.shared.application.support` |
| `support/RepositoryProvisionerTest.java` | repository provisioning test | `io.jgitkins.server.repository.application.support.provisioning` |
| `validate/BranchCreationValidatorTest.java` | repository validator test | `io.jgitkins.server.repository.application.validate` |
| `validate/RepositoryAccessValidatorTest.java` | repository validator test | `io.jgitkins.server.repository.application.validate` |

### 3가지 방법
#### 방법 A: top-level application 유지
- 장점: 이동량이 작다.
- 단점: package tree 가 계속 공용 저장소처럼 보인다.
- 평가: 2/10.

#### 방법 B: shared/application 과 context application 으로만 분리
- 장점: 공용과 전용이 분리된다.
- 단점: 일부 presentation DTO 와 contract DTO 가 여전히 섞일 수 있다.
- 평가: 7/10.

#### 방법 C: shared/application + context application + presentation contract 까지 함께 정리
- 장점: 책임 경계가 가장 선명하다.
- 장점: 다음 단계 domain/presentation 정리에 필요한 seam 이 생긴다.
- 단점: 파일 이동량이 많다.
- 평가: 10/10.

**권장안:** 방법 C.

### 구현 순서
1. shared/application 에 남길 base error / event / exception 을 먼저 고정한다.
2. repository contract / service / validate / mapper 를 먼저 옮긴다.
3. change/review, execution, identity/access 의 application 타입을 차례로 이관한다.
4. top-level `server/application` 이 더 이상 소유권을 숨기지 않도록 package policy test 를 추가한다.

### 검증
- `ArchitecturePackageConventionTest` 에 top-level application 금지 규칙을 추가한다.
- bounded context 별 application test 는 자신의 contract / service / mapper / validate 경계를 확인한다.
- `./gradlew :app-server:test` 로 리팩토링 후 회귀를 검증한다.

### NOT in scope
- `domain` 이동
- `presentation` 이동
- infrastructure 이동
- 비즈니스 동작 변경
- API route 변경
