# 패키지 분리 전략

## 배경
- 현재 코드는 `application`, `domain`, `infrastructure`, `presentation` 계층 우선 구조다.
- 문서는 이미 `Repository`, `Change & Review`, `Execution`, `Identity & Access`, `Collaboration`, `Shared / Cross-Cutting` context로 나뉘어 있다.
- 이 상태에서 계층 패키지 내부에 코드가 계속 누적되면 같은 유스케이스를 따라갈 때 여러 계층 패키지를 반복 횡단해야 하므로 관리 비용이 증가한다.

## 방향
- 최종 방향은 `context 최상위 + 내부 4계층` 구조다.
- `Task 2.18`에서는 전체 context 이관을 바로 수행하지 않고, 먼저 `shared` 패키지를 선생성한 뒤 shared seam 후보를 점진적으로 이동한다.
- 이번 작업은 전체 재배치가 아니라 후속 context 분리의 기준선과 첫 이동 단위를 만드는 단계다.

## 목표 패키지 구조
```text
io.jgitkins.server.shared
io.jgitkins.server.shared.common
io.jgitkins.server.shared.application
io.jgitkins.server.shared.application.support
io.jgitkins.server.shared.application.policy
io.jgitkins.server.shared.application.change
```

## 1차 이관 원칙
- 현재 시점에서 공통 seam 성격이 명확한 리소스만 먼저 이동한다.
- package 이동만으로 의미가 유지되는 클래스만 1차 대상으로 본다.
- 이동 후 import 정리와 테스트 고정을 함께 수행한다.

## 1차 이관 후보
- `RepositoryNamespaceResolver`
- `RepositoryAccessibilityService`
- `RepositoryPathHelper`
- `MergeabilityAssessmentAssembler`
- `PushJobCreationPolicy`
- `EventPolicyResolver`

## 2차 이관 후보
- `RepositoryLookupService`
- `GitRepositoryAccessService`

## 2차 이관 보류 이유
- `RepositoryLookupService`는 현재 repository 조회 책임이지만 repository context와 shared seam의 경계가 아직 완전히 고정되지 않았다.
- `GitRepositoryAccessService`는 repository 식별과 permission 계산 책임이 아직 함께 있다.
- 이 둘은 지금 패키지만 먼저 옮기면 `shared`가 공통 seam이 아니라 미완성 repository 코드 보관소처럼 보일 수 있다.
- 따라서 lookup/access resolution 책임 정리 이후 이동하는 것이 안전하다.

## 2차 책임 분리 계획
- 2차 대상은 `패키지 이동`보다 `책임 분리`가 먼저다.
- 먼저 `RepositoryLookupService`를 repository 식별 전용 컴포넌트로 고정한다.
- 그 다음 `GitRepositoryAccessService`에서 repository 식별 로직을 제거하고 permission 계산만 남긴다.
- 마지막으로 분리된 책임 기준으로 목표 패키지를 다시 확정한다.

## 2차 책임 분리 단계
1. `RepositoryLookupService`에 남아 있는 조회 규칙을 단일 정책으로 고정한다.
2. `GitRepositoryAccessService` 내부의 namespace 기반 repository 해석 로직을 lookup 위임으로 치환한다.
3. visibility 계산과 permission 계산이 섞여 있는 호출부를 분리한다.
4. `RepositoryPermission`을 use case 내부 record로 유지할지, shared read-side result로 이동할지 결정한다.
5. 책임 분리 후 `RepositoryLookupService`, `GitRepositoryAccessService`의 목표 패키지를 최종 확정한다.

## 주의 사항
- `shared`에는 진짜 공통 규칙, 공통 계산, 공통 읽기 모델만 둔다.
- repository 전용 규칙, pull request 전용 규칙, identity 전용 규칙은 shared로 보내지 않는다.
- `shared-cross-cutting` 문서의 범위와 실제 코드 패키지 범위가 계속 일치하도록 유지한다.

## seam 분류표
| seam | 1차 분류 | 현재 코드 위치 | 목표 위치 | 상태 |
|---|---|---|---|---|
| `RepositoryNamespaceResolver` | application-level support seam | `shared.application.support` | `shared.application.support` | 완료 |
| `RepositoryAccessibilityService` | application-level support seam | `shared.application.support` | `shared.application.support` | 완료 |
| `RepositoryPathHelper` | shared utility / value helper | `shared.common` | `shared.common` | 완료 |
| `MergeabilityAssessmentAssembler` | read-side result assembler | `shared.application.change` | `shared.application.change` | 완료 |
| `PushJobCreationPolicy` | application-level policy | `shared.application.policy` | `shared.application.policy` | 완료 |
| `EventPolicyResolver` | application-level policy coordinator | `shared.application.policy` | `shared.application.policy` | 완료 |
| `RepositoryLookupService` | repository identifier resolver | `repository.application.support` | `repository.application.support` | 완료 |
| `GitRepositoryAccessService` | repository access evaluator | `repository.application.support` | `repository.application.support` | 완료 |
| `RepositoryPermission` | repository read-side result | `repository.application.result` | `repository.application.result` | 완료 |

## repository 패키지 결정
- `RepositoryLookupService`, `GitRepositoryAccessService`, `RepositoryPermission`은 `shared`보다 `repository` 소유가 더 강하다.
- 따라서 repository 전용 패키지를 신설해서 이관하는 방향은 타당하다.
- 현재 문서 기준 권장 구조는 아래와 같다.

```text
io.jgitkins.server.repository
io.jgitkins.server.repository.application
io.jgitkins.server.repository.application.support
io.jgitkins.server.repository.application.result
```

- `RepositoryLookupService` -> `io.jgitkins.server.repository.application.support`
- `GitRepositoryAccessService` -> `io.jgitkins.server.repository.application.support`
- `RepositoryPermission` -> `io.jgitkins.server.repository.application.result`

## package naming 결정
- 현 단계에서는 `context` prefix 없이 `repository` 최상위 패키지로 간다.
- 이유는 bounded context를 코드 구조에 직접 드러내는 것이 목적이고, `context` prefix는 의미를 더하지 않으면서 이름만 길게 만든다.
- `domain.repository`, `infrastructure...persistence.repository`와의 혼동은 내부 계층명과 import 경계로 관리한다.
- 이후 다른 bounded context도 같은 규칙으로 `execution`, `change_review`처럼 맞춘다.

## context x seam 사용 매트릭스
| context | 사용하는 seam | 사용 목적 |
|---|---|---|
| `Repository` | `RepositoryNamespaceResolver`, `RepositoryAccessibilityService`, `RepositoryPathHelper`, `RepositoryLookupService`, `GitRepositoryAccessService`, `RepositoryPermission` | repository 식별, path 구성, visibility, 권한 계산 |
| `Change & Review` | `RepositoryNamespaceResolver`, `MergeabilityAssessmentAssembler` | PR mergeability 계산, branch/merge preview 결과 조립 |
| `Execution` | `PushJobCreationPolicy`, `EventPolicyResolver`, `RepositoryPathHelper` | push 이벤트 정책 판정, pipeline file path 정규화 |
| `Identity & Access` | `GitRepositoryAccessService`, `RepositoryPermission` | 사용자 기준 읽기/쓰기 권한 판단 |

## 영향 범위
| seam | 주요 영향 서비스/구성요소 | 회귀 위험 포인트 |
|---|---|---|
| `RepositoryNamespaceResolver` | `PullRequestService`, `RepositoryManagementService`, `BranchManagementService`, `RepositoryProvisioner`, `PullRequestMergeabilityResolver` | owner -> namespace 변환 실패, org/user 조회 예외 |
| `RepositoryAccessibilityService` | `RepositoryLoadService` | 공개/비공개 노출 판정, 조직 membership cache |
| `RepositoryPathHelper` | `RepositoryManagementService`, `PushEventCommandResolver`, `RepositoryLookupService` | clone path 정규화, slash 처리 |
| `MergeabilityAssessmentAssembler` | `MergeService`, `PullRequestMergeabilityResolver` | status mapping, unknown topology 처리 |
| `PushJobCreationPolicy`, `EventPolicyResolver` | `PushEventHandleService`, git hook/push 흐름 | rule 선택, pipeline file 존재 검증, 정책 오류 fallback |
| `RepositoryLookupService` | `RepositoryLoadService`, `PullRequestService`, `GitRepositoryAccessService` | namespace 충돌, clone path 우선순위, 입력 trim 규칙 |
| `GitRepositoryAccessService` | `RepositoryOverviewService`, `RepositoryAccessValidator`, `GitSmartHttpAuthorizer`, `GitSmartHttpAuthFilter` | public/owner/member 권한 우선순위, facade 계약 |

## 테스트 전략
| seam | 필요한 테스트 |
|---|---|
| `RepositoryNamespaceResolver` | user/org owner 변환 성공, 미존재 예외, null 방어 |
| `RepositoryAccessibilityService` | public repository, private user-owned, private organization-owned, membership cache hit |
| `RepositoryPathHelper` | slash trimming, `.git` suffix 보정, namespace/path 조합 |
| `MergeabilityAssessmentAssembler` | mergeable/conflicting/no-common-ancestor/unknown 매핑 |
| `PushJobCreationPolicy` | rule match, no rule, missing pipeline file, policy error fallback |
| `EventPolicyResolver` | push policy 위임 |
| `RepositoryLookupService` | clone path 우선, user owner 조회, organization owner 조회, namespace 충돌, trim rules |
| `GitRepositoryAccessService` | anonymous/public/owner/repository member/organization member/namespace 충돌 |

## 남은 결정 사항
- `RepositoryLookupService`를 API facade, Git Smart HTTP facade, 공통 resolution core로 한 번 더 세분화할지 판단해야 한다.
- `repository` 외 다른 bounded context도 동일 규칙으로 최상위 패키지를 신설할지 후속 순서를 정해야 한다.
