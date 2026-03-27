# 기능/구조 분석서

### 1. 분석 대상 개요 (Overview)
- **분석 대상**: `jgitkins/.github/workflows/` 하위 GitHub Actions CI/CD 워크플로
- **분석 목적**: 모노레포 기준 현재 CI/CD 구조를 이해하고 `4.1 모노레포변경으로 인한 ci 설정 최적화`의 기준선을 정리함
- **분석 배경**: 현재 저장소는 `server`, `web`, `runner`를 한 저장소에서 함께 운영하므로, 변경 감지와 모듈별 검증/배포 경계가 의도대로 작동하는지 확인이 필요함

### 2. 현행 시스템 구조 및 동작 방식 (AS-IS)
- **주요 책임**:
  - `verify.yml`: `develop` push 또는 수동 실행 시 변경 파일 기준으로 모듈별 검증 분기
  - `deploy.yml`: 수동 실행 시 변경 파일 기준으로 모듈별 배포 분기
  - `server-verify.yml`, `web-verify.yml`: 각 모듈 `clean test bootJar`
  - `server-release.yml`, `web-release.yml`: Docker 빌드/푸시 및 원격 서버 배포
- **동작 흐름**:
  - 루트 워크플로가 `git diff`로 변경 파일을 계산하고 `server/web/runner` 여부를 판별함
  - `server`, `web`는 reusable workflow로 위임됨
  - `runner`는 verify/release 모두 실제 구현 없이 메시지만 출력함
- **의존성 및 연관 요소**:
  - GitHub Actions reusable workflow
  - Gradle + Java 17
  - GHCR, Docker Buildx
  - SSH/SCP 기반 원격 배포
- **입출력 및 계약**:
  - 루트 워크플로는 `server`, `web`, `runner` boolean output을 생성함
  - `server`와 `web`는 서로 다른 배포 시크릿/경로를 사용함

### 3. 현행 특성 및 이슈 식별 (Current Findings)
- **기능적 특성**:
  - 모노레포 라우터 + 모듈별 reusable workflow 구조는 이미 잡혀 있음
  - `server`, `web`는 검증/배포가 구현되어 있고 `runner`는 미구현 상태임
- **코드/구조 특성**:
  - `verify.yml`과 `deploy.yml`의 detect 로직이 거의 동일하게 중복됨
  - `server-verify.yml`과 `web-verify.yml`도 구조가 거의 같음
- **성능/운영 특성**:
  - verify/deploy 모두 `concurrency`를 사용함
  - `server-release`는 `arm64`, `web-release`는 `amd64, arm64`를 빌드함
- **확인된 문제 또는 의문점**:
  - `workflow_dispatch`에서 `github.event.before`가 비어 있으면 최초 커밋 기준 diff를 사용해 수동 실행 시 과검출 가능성이 있음
  - `runner`는 detect 대상이지만 실제 workflow 파일이 없어 계약과 구현이 불일치함
  - 배포 정책과 시크릿 규칙이 워크플로 파일에만 흩어져 있어 운영 가시성이 낮음

### 4. 영향도 분석 (Impact Analysis)
- **비즈니스 영향도**:
  - `runner` 검증/배포 공백은 운영 신뢰성을 낮춤
  - 수동 배포 시 과도한 변경 감지는 불필요한 배포로 이어질 수 있음
- **기술적 파급 효과**:
  - 루트 detect 로직 수정은 전체 CI 진입 조건에 영향을 줌
  - reusable workflow 공통화는 `server/web` 전체 파이프라인에 영향을 줌
- **변경 민감 지점**:
  - `git diff` base 계산
  - `if: needs.detect.outputs.* == 'true'`
  - 배포 경로, 시크릿, 이미지 플랫폼 정책

### 5. 개선 또는 활용 방향성 (Direction)
- **유지/개선 판단**:
  - 현재 구조 방향은 유지할 가치가 있으나, `runner` 미구현과 detect 중복은 개선 필요
- **개선 방향 또는 후속 액션**:
  - `runner` verify/release를 구현하거나 detect 대상에서 제외해 계약을 맞춤
  - 수동 실행은 변경 감지 대신 입력 파라미터 기반 선택 실행으로 전환 검토
  - detect 로직과 verify 로직 공통화 검토
  - 모듈별 배포 정책과 시크릿 사용 규칙을 문서화
- **우선순위**:
  - 즉시: `runner` 지원 범위 명확화, 수동 실행 조건 재검토
  - 단기: detect 중복 제거, 운영 정책 문서화
  - 중장기: 공통 reusable workflow 기반 통합

### 6. 위험 요소 및 고려사항 (Risk Assessment)
- **예상 리스크**:
  - detect 로직 수정 시 필요한 파이프라인이 누락될 수 있음
  - 공통화 과정에서 모듈별 예외 정책이 빠질 수 있음
  - `runner`를 서둘러 붙이면 임시 운영 정책이 고착될 수 있음
- **검증 전략**:
  - `develop` push와 `workflow_dispatch`를 분리해 실행 조건을 검증함
  - server-only, web-only, workflow 파일 변경 시나리오를 각각 확인함
  - release 변경 시 GHCR 푸시, 원격 pull/up 로그를 단계별로 검증함

### 결론 (추후 작성)
- 현재 CI/CD는 모노레포의 기본 골격은 갖췄지만, `runner` 미구현과 수동 실행 시 변경 감지 한계가 남아 있다. `4.1`은 detect 계약 정리, `runner` 지원 범위 확정, 중복 워크플로 공통화를 우선 목표로 두는 것이 적절하다.
