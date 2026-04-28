# Task 2.18 상세 문서

## 진행 상태
- [x] `Namespace`, `Pipeline Policy`, `MergeabilityAssessment`의 current/target seam 문서화
- [x] `shared` 1차 이관 후보 식별 및 목표 패키지 구조 정의
- [x] `shared` 1차 이관 후보 실제 코드 반영
- [x] `RepositoryLookupService`, `GitRepositoryAccessService` 2차 책임 분리 계획 수립
- [x] 문서를 메인 인덱스 + 상세 문서 구조로 분리
- [x] seam 분류표 정리
- [x] `Execution`, `Repository`, `Change & Review` context의 seam 사용 매트릭스 정리
- [x] 영향 범위와 테스트 전략 정리
- [x] `RepositoryPermission`의 최종 분류와 목표 위치 확정
- [x] `RepositoryLookupService` 최종 목표 패키지 확정
- [x] `GitRepositoryAccessService` / facade 구조의 최종 목표 패키지 확정

## 문서 목록
- [패키지 분리 전략](./package_strategy.md)
- [Namespace](./namespace.md)
- [Repository Lookup](./repository_lookup.md)
- [Repository Access Resolution](./repository_access_resolution.md)
- [Mergeability Assessment](./mergeability_assessment.md)
- [Pipeline Policy](./pipeline_policy.md)

## 목적
- `task_2_18_plan.md`를 요약 인덱스로 유지하고, 긴 예시 코드와 단계별 계획은 상세 문서로 분리한다.
- `Shared / Cross-Cutting Topics` 리팩토링 기준선과 1차 shared 이관, 2차 책임 분리 계획을 문서 단위로 관리한다.

## 현재 결정 사항
- `RepositoryPermission`은 facade 계약 내부 record에서 분리하고 `io.jgitkins.server.repository.application.result`에 둔다.
- `RepositoryLookupService`, `GitRepositoryAccessService`, `GitRepositoryAccessUseCaseFacade`는 `io.jgitkins.server.repository.application.support`로 이관한다.
- `Task 2.18`의 남은 논점은 channel별 facade 추가 분리 여부와 후속 context 확장 순서다.
