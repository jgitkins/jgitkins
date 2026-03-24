# Job 문서 인덱스

## 목적
- Job 도메인 문서를 상태 흐름 중심 문서와 도메인 모델 구조 문서로 분리하여 관리하기 위한 인덱스 문서임.
- Push 기반 Job 생성과 실행 라이프사이클은 시간 축의 상태 변화와 객체 구조를 분리해서 설명할 필요가 있음.

## 문서 구성
- `job-event-storming.md`
  - Push 수신부터 Job 생성 요청, 큐 대기, 실행 완료까지의 상태 변화와 도메인 이벤트를 정리함.
- `job-domain-model.md`
  - Job aggregate, JobHistory, RunnerAssignment, JobTrigger 등 핵심 객체의 경계와 관계를 정리함.

## 참고 문서
- `.taskmaster/docs/refactor/task_2_34_plan.md`
  - `PushEventHandleService` 헥사고날 경계 재정렬과 Job 이벤트 흐름 정리를 위한 리팩토링 계획 문서임.

## 비고
- 본 문서는 인덱스 역할만 수행하며, 세부 다이어그램은 분리된 문서에서 관리함.
