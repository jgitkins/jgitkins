# 리팩토링 계획서

## 개요
- 문서: Task 2.18 Shared / Cross-Cutting Topics 리팩토링 계획서
- 참조: `docs/modeling/contexts/shared-cross-cutting/shared-cross-cutting-topics.md`
- 상세 문서 인덱스: [task_2_18/README.md](./task_2_18/README.md)

## 목적
- Shared / Cross-Cutting Topics 개념을 코드에 일관된 구조로 반영한다.
- `shared` 패키지 선생성과 1차 seam 이관 기준을 확정한다.
- `RepositoryLookupService`, `GitRepositoryAccessService`의 2차 책임 분리 기준을 확정한다.

## 채택안
- 개념별 책임 경계를 먼저 고정한 뒤, 각 단계마다 최소 코드 이동과 이름 정리를 수행한다.
- 한 번에 전체 재배치하지 않고, 개념 하나씩 순차적으로 정리한다.
- 기존 동작 보존과 테스트 고정을 우선한다.
- 문서 context 기반 코드 구조 전환을 준비하기 위해 `shared` 패키지를 먼저 생성하고, seam 성격이 명확한 리소스부터 1차 이관한다.

## 우선순위
1. Namespace
2. Repository Lookup
3. Repository Access Resolution
4. Mergeability Assessment
5. Pipeline Policy

## 작업 원칙
- `shared`는 임시 적치장이 아니라 공통 seam 전용 패키지로 유지한다.
- context 소유가 분명한 코드와 shared seam 코드를 구분해서 이동한다.
- 패키지 이동만 먼저 할 수 있는 클래스와, 책임 분리 후 이동해야 하는 클래스를 분리해서 다룬다.

## 문서 분리
- [패키지 분리 전략](./task_2_18/package_strategy.md)
- [Namespace](./task_2_18/namespace.md)
- [Repository Lookup](./task_2_18/repository_lookup.md)
- [Repository Access Resolution](./task_2_18/repository_access_resolution.md)
- [Mergeability Assessment](./task_2_18/mergeability_assessment.md)
- [Pipeline Policy](./task_2_18/pipeline_policy.md)

## 현재 결정
- 1차 shared 이관 후보:
  `RepositoryNamespaceResolver`, `RepositoryAccessibilityService`, `RepositoryPathHelper`, `MergeabilityAssessmentAssembler`, `PushJobCreationPolicy`, `EventPolicyResolver`
- 2차 책임 분리 대상:
  `RepositoryLookupService`, `GitRepositoryAccessService`

## 수행 순서
1. `shared` 패키지 구조와 1차 이관 기준 확정
2. 1차 shared 이관 대상 이동
3. import 정리와 관련 테스트 고정
4. Namespace 리팩토링 수행
5. `RepositoryLookupService` 책임 분리 수행
6. `GitRepositoryAccessService` 책임 분리 수행
7. Repository Lookup 목표 패키지 재판단
8. Repository Access Resolution과 `RepositoryPermission` 목표 위치 확정
9. Mergeability Assessment 정리
10. Pipeline Policy 정리

## 완료 기준
- 1차 shared 이관 후보는 새 패키지 구조 아래에서 동일 동작을 유지해야 한다.
- `RepositoryLookupService`는 repository 조회 전용 책임만 남아야 한다.
- `GitRepositoryAccessService`는 permission 계산 전용 책임만 남아야 한다.
