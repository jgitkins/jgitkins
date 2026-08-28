# Repository READ/WRITE 권한 명세 (Web)

## 1. 목적
- Public 저장소는 누구나 `READ` 가능해야 한다.
- 쓰기 기능(브랜치 생성/파일 생성/디렉터리 생성)은 `WRITE` 권한 사용자만 가능해야 한다.
- UI 제어와 서버 강제 정책이 동일한 기준을 가져야 한다.

## 2. 용어
- `READ`: 저장소 상세/트리/검색 등 조회 권한
- `WRITE`: 브랜치 생성, 파일 생성(커밋), 디렉터리 생성(커밋) 권한
- `Actor`: 현재 로그인 사용자(비로그인 포함)

## 3. 권한 매트릭스

| Repository Visibility | Actor | READ | WRITE |
|---|---|---|---|
| Public | Anonymous | O | X |
| Public | Authenticated (비멤버) | O | X |
| Public | Owner / Org Member / Repo Member | O | O |
| Private | Anonymous | X | X |
| Private | Authenticated (비멤버) | X | X |
| Private | Owner / Org Member / Repo Member | O | O |

## 4. UI 정책 (Web)
- 상세 진입 시 권한 정보를 기반으로 `canRead`, `canWrite`를 계산한다.
- `canWrite=false`이면 아래 기능을 비활성화(또는 비노출)한다.
- `New branch`
- `New file`
- `New directory`
- 사용자 조작으로 강제 호출하더라도 서버 `403`을 그대로 처리한다.

## 5. API 오류 처리
- `403 Forbidden`: 요청자가 볼 수 있는 저장소인데 해당 조작 권한이 없을 때
  (WRITE, DELETE, MANAGE_MEMBER 공통). 서버 명세 §4 를 따른다.
- `404 Not Found`: 저장소가 없거나, 요청자에게 비가시일 때 (조건이 아니라 규칙이다)
- 화면 공통 문구: `정의되지 않은 오류입니다. 관리자에게 문의하세요`
- 단, 권한 오류는 가능하면 사용자 친화 메시지(`권한이 없습니다`)를 우선 노출한다.

## 6. 설계 대안 비교
- 대안 A: Web UI에서만 차단
- 장점: 구현이 빠름
- 단점: 보안 경계가 아님(직접 API 호출 우회 가능)
- 대안 B: Server에서만 차단
- 장점: 보안상 안전
- 단점: UX가 떨어짐(버튼은 보이는데 실패)
- 대안 C: Web 선제 제어 + Server 최종 강제
- 장점: UX/보안 균형, 헥사고날 경계 명확
- 단점: 양 모듈 동시 반영 필요

선택: `대안 C`

## 7. 수용 기준 (2.5)
- Public 비로그인 사용자는 상세 조회 가능, 쓰기 기능 사용 불가
- Private 비권한 사용자는 상세 조회 불가 또는 정책 기반 오류 처리
- 권한 없는 쓰기 API 호출 시 반드시 `403`
- Web 버튼 상태와 서버 결과가 불일치하지 않음

## 8. 후속 개선안 3가지
- 권한 조회 전용 API(`GET /repositories/{ns}/{repo}/permissions`) 표준화
- 오류 응답 스키마(code/message/requestId) 통일
- 감사 로그(actor/repo/action/result) 표준 필드 적용

이번 반영 선택: 오류 응답 스키마 통일을 우선 과제로 채택(서버 2.6 구현 시 포함)
