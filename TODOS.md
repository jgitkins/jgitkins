# TODOS

리포 전반의 이연 항목. 태스크 관리는 `.taskmaster/` 가 하고, 여기는 그 바깥의
방향성 메모를 둔다.

## 조직·저장소 삭제를 비동기 지연 삭제로 전환

**현재**: 조직이 저장소를 소유하고 있으면 조직 삭제를 거부한다. 스키마에 FK 가 없어
그냥 지우면 `OWNER_ID` 가 사라진 조직을 가리키는 고아 저장소가 남고, 조직 id 를 되살릴
수 없으므로 복구 경로가 없다. 막는 것이 지금 할 수 있는 정직한 답이다.

**가야 할 곳**: GitLab 형 지연 삭제. 삭제 요청은 `MARKED_FOR_DELETION_AT` 만 찍고,
주기 스위퍼가 보존 기간(예: 7일) 뒤에 소유 저장소 → 멤버십 → 조직 순으로 정리한다.
그 사이에는 사용자가 취소할 수 있다.

GitHub 형(즉시 cascade + 지원팀 복구) 대신 GitLab 형을 고르는 이유는 이 프로젝트에
지원 프로세스가 없어서다. 대역 외 복구는 복구가 아니다.

**메시지 인프라를 기다리지 않는다.** 상태 컬럼 + 주기 조회는 구조적으로 크래시 안전하다
(상태가 큐가 아니라 행에 있으므로 스위퍼가 죽어도 다음 실행이 같은 행을 다시 집는다).
transactional outbox 는 전제가 아니다.

**완료 조건에 "조직 삭제 거부 가드 제거" 를 포함한다.** 되돌릴 수 있게 되면 막을 이유가
없어진다. 가드가 남아 있으면 그 작업은 끝난 것이 아니다.

**실제 비용은 스위퍼가 아니라 읽기 경로다.** 조직 조회 10곳에 필터가 필요하고, 하나라도
빠뜨리면 "삭제 예정" 조직이 계속 보인다. 이름 유일성
(`OrganizationNameUniquenessAclAdapter`)이 특히 미묘하다 — 삭제 예정 조직의 이름을
즉시 해제할지 보존 기간 동안 잠글지 정해야 한다.

설계 근거: `~/.gstack/projects/jgitkins/hrk-main-design-20260828-three-verified-p0s.md`
의 "T1 후속 — 배치/지연 삭제 검토" 절.

**이벤트 발행 제약 (2026-09-01 eng review).** 스위퍼가 한 트랜잭션에서 조직 여러
개를 처리하면서 도메인 이벤트를 발행할 계획이라면, `DomainEventPublisher.publish()`
를 루프 안에서 부르지 말 것. `CollaborationSpringDomainEventPublisher` 는 호출마다
새 `TransactionSynchronization` 을 등록하고 각각이 자기 이벤트 스냅샷을 커밋까지
붙들기 때문에, N번 호출하면 N개의 동기화 객체와 N개의 리스트가 쌓인다. 배치가
필요해지는 시점에 트랜잭션당 1회 등록 + 버퍼 누적으로 바꾸거나, 이벤트를 모아
한 번만 넘길 것. 오늘은 호출자가 `OrganizeService` 하나뿐이라 도달 불가능한
경로이므로 미리 고치지 않는다.

## 아키텍처 가드레일을 ArchUnit 으로 옮기는 것 검토

**현재**: `ArchitectureScanner` 가 자바 소스를 문자열로 읽어 정규식으로 규칙을 검사하고,
`app-server/src/test/java/io/jgitkins/server/architecture/` 아래 6개 테스트가 그 위에 서 있다.
`app-server/build.gradle` 에 archunit 의존성은 없다.

**왜**: 같은 계열의 버그가 반복해서 나온다. 소스를 글자로 읽으면 (a) 주석이 같이 읽히고
(b) 긴 이름의 일부가 짧은 이름으로 오인된다.

- (a) 2.77 / 2.65 / 2.66 — "이 기술을 일부러 쓰지 않는다"고 설명하는 javadoc 이 가드레일에
  걸렸다. 한 세션에 세 번. 그래서 `stripComments()` 가 생겼다.
- (b) 2.107 계획 리뷰 — `@RestController` 패턴이 `@RestControllerAdvice` 안에서 부분일치한다.
  `ArchitectureScanner.scan():141` 이 `find()` 를 쓰기 때문이다.

ArchUnit 은 바이트코드를 본다. 바이트코드에는 주석이 없고 애노테이션은 타입이므로 두 부류가
**구조적으로 불가능**해진다. 덤으로 `freeze()` 가 기존 위반을 잠가주므로 손으로 유지하는
allowlist 자체가 필요 없어진다 — 2.107 이 스캔으로 바꾸려는 그 목록이 사라진다.

**지금 안 하는 이유**: 105개 파일 패키지 개명(2.131) 중에 테스트 기반까지 갈아끼우면
구조 변경과 동작 변경을 동시에 하는 것이 된다. 그리고 6개 테스트가 이미 regex 스캐너 위에
있어서, 중간 상태에서는 두 관용구가 공존한다.

**시작점**: `app-server/build.gradle` 에 `testImplementation 'com.tngtech.archunit:archunit-junit5'`
를 추가하고 `CrossContextPersistenceCouplingArchitectureTest` **하나만** 이식해 두 관용구를
나란히 놓고 비교한다. 나머지 5개는 그 비교 결과를 보고 판단한다.

**선후**: 의존성은 없지만 패키지·이름 통일 클러스터(2.107/2.101/2.129/2.131/2.132) 랜딩
뒤가 자연스럽다. 2.107 이 컨트롤러 목록을 하나로 모은 뒤여야 이전 대상이 명확해진다.
설계 근거: `~/.gstack/projects/jgitkins/hrk-refactor-package-naming-design-20260901-dto-to-contract-cluster.md`.

## ArchitectureScanner 의 Violation 은 이제 위반만 담지 않는다

**현재**: `ArchitectureScanner` 의 상수는 `FORBIDDEN_*` 열넷과 `CONTROLLER` 하나다. 앞의 열넷은
금지된 import 를 찾고, `CONTROLLER` 는 2.107 이 추가한 것으로 금지가 아니라 **인벤토리**다 —
`ControllerAllowlistCompletenessTest` 가 "디스크에 있는 컨트롤러 집합"을 얻는 데 쓴다.

**문제**: `Category` 의 javadoc 은 "A forbidden category" 라 적혀 있고 결과 타입은 `Violation`
이다. `CONTROLLER` 로 스캔하면 위반이 아닌 것이 `Violation` 으로 18개 나온다. 각 자리에
javadoc 을 달아 설명해뒀지만, 설명이 필요하다는 것 자체가 이름이 안 맞는다는 뜻이다.

**지금 안 한 이유**: `Violation` → `Match`/`Finding` 개명은 `architecture/` 아래 테스트 6개를
건드린다. 그 파일들은 main 에서 다른 에이전트가 같은 시기에 작업 중이었고, 이 클러스터는
충돌을 피하려고 "`ArchitectureScanner` 는 추가만" 규칙을 지켰다.

**시작점**: `Violation` 을 중립적인 이름으로 바꾸고, `Category` javadoc 을 "금지" 대신
"패턴과 그 의미"로 다시 쓴다. 소비자 6개는 전부 카테고리를 명시적으로 나열하므로
기계적 개명이고 컴파일러가 누락을 잡는다.
