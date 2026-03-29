# 리팩토링 계획서

### 제목
- **리팩토링 계획**: `4.3 [runner] 모노레포 runner release/deploy 연결`

### 배경 (왜?)
- `runner` 모듈은 monorepo로 이관되었고 verify workflow도 연결되었으나, release/deploy 경로는 아직 스텁 상태다.
- 현재 `runner/Dockerfile`, runner 배포 자산, `.github/workflows/runner-release.yml`이 없어서 `deploy.yml`에서 실제 배포 흐름을 연결할 수 없다.
- 따라서 `server`, `web`와 동일한 수준의 최소 release 경로를 `runner`에도 정의해야 한다.

### 목표 (Goals)
- `runner` 이미지 빌드 경로를 명확히 한다.
- `deploy.yml`에서 `runner` 변경 시 실제 release workflow를 호출하도록 정리한다.
- 코드 스니펫만이 아니라, 어떤 파일을 생성·수정해야 하는지 문서에서 바로 식별 가능하게 만든다.

### 범위 (Scope)
- **수정 대상**
  - `.github/workflows/deploy.yml`
  - `.github/workflows/runner-release.yml`
  - `runner/Dockerfile`
  - `runner/docker-compose-config/apps/docker-compose.yml` 또는 동등 배포 자산
- **수정 제외 대상**
  - runner 애플리케이션 로직
  - 원격 서버 신규 구축
  - 운영 시크릿 발급 및 서버 프로비저닝

### 계획 (Plan)
- **단계 1**: 기존 `server-release.yml`, `web-release.yml` 구조를 기준으로 `runner` release 최소 기준을 정리한다.
- **단계 2**: `runner/Dockerfile`과 배포 자산 경로를 정의한다.
- **단계 3**: `.github/workflows/runner-release.yml`을 추가하고 GHCR 빌드/푸시 및 필요 시 배포 단계를 연결한다.
- **단계 4**: `.github/workflows/deploy.yml`의 runner 스텁을 제거하고 reusable workflow 호출로 교체한다.
- **단계 5**: workflow의 `context`, `file`, `source`, 원격 실행 경로가 실제 파일 구조와 일치하는지 검증한다.

### 기대효과 (Expected Benefits)
- `runner`도 `server`, `web`와 동일한 배포 진입점을 갖게 된다.
- 변경 대상 파일이 명확해져 구현 및 리뷰 비용이 줄어든다.
- 스텁 상태의 deploy 흐름을 제거하여 배포 누락 위험을 줄인다.

### 예시 (설정 파일 기준 코드 스니펫)

#### AS-IS (현재 구조)
```yaml
# 파일: .github/workflows/deploy.yml
runner:
  needs: detect
  if: needs.detect.outputs.runner == 'true'
  runs-on: ubuntu-latest
  steps:
    - run: echo "runner release is skipped: Dockerfile and deployment assets are not prepared yet."
```

#### TO-BE (개선 제안 구조)
```text
.github/workflows/
  deploy.yml
  runner-release.yml
runner/
  Dockerfile
  docker-compose-config/
    apps/
      docker-compose.yml
```

```yaml
# 파일: .github/workflows/deploy.yml
runner:
  needs: detect
  if: needs.detect.outputs.runner == 'true'
  uses: ./.github/workflows/runner-release.yml
  secrets: inherit
```

```yaml
# 파일: .github/workflows/runner-release.yml
with:
  context: ./runner
  file: ./runner/Dockerfile
```

```yaml
# 파일: runner/docker-compose-config/apps/docker-compose.yml
services:
  jgitkins-runner:
    image: ghcr.io/<owner>/jgitkins-runner:latest
```

### 주의사항
- **계획우선**: 문서 작성 단계에서는 구현을 진행하지 않는다.
- **기존 패턴 준수**: `server-release.yml`, `web-release.yml`의 구조를 우선 참조한다.
- **파일 매핑 명시**: 모든 스니펫은 대상 파일과 함께 제시한다.
- **문서 간결성 유지**: 구현 세부값보다 파일, 경로, 연결 지점을 우선 기록한다.

### 결론 (추후작성)
- 본 작업은 `runner`의 release/deploy 공백을 메우기 위한 최소 배포 경로 정의 작업이다.
