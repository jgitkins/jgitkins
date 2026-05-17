# Task 2.33 Follow-up Plan: app 모듈의 core library 주입 및 모듈 rename

### 목적
- `core-common`, `core-web`, `core-security`, `core-persistence`, `core-grpc`로 공통 의존성을 먼저 분리한 뒤, 실행 가능한 `bootJar` 모듈들이 그 공통 라이브러리를 직접 참조하도록 정렬한다.
- `server`, `web`, `runner`라는 현재 모듈명은 실행 역할을 드러내지 못하므로 `app-server`, `app-web`, `app-runner`로 rename한다.
- 이 단계의 목표는 단순한 이름 변경이 아니라, app 모듈이 조립자 역할만 갖고 core library와 client SDK를 통해 기능을 구성하도록 경계를 고정하는 것이다.

### 현재 전제
- `core-*` 분리는 이미 완료되었거나, 최소한 이 계획의 선행 단계로 간주한다.
- 이 문서는 `context-*` 세부 이관보다 app module의 의존성 주입과 bootstrap 정리에 집중한다.
- `app-web`은 domain context를 직접 의존하지 않는 방향을 유지한다.
- `app-server`와 `app-runner`는 각자의 boot application 역할만 남기고, 공통 기술 코드는 core library로 위임한다.
- rename 범위에는 `settings.gradle`, 각 모듈 `build.gradle`, 각 모듈 `Dockerfile`, `docker-compose-config`, `.github/workflows/*`, `.github/scripts/detect-changed-modules.sh`, `web/settings.gradle`까지 포함한다.

### 3가지 방법 검토
1. 모듈 rename 없이 의존성만 먼저 바꾸는 방식
   - 장점: 변경 폭이 작다.
   - 단점: 기존 `server/web/runner` 이름이 계속 남아 구조가 더 혼탁해진다.
2. 모듈 rename만 먼저 하고 core 주입은 나중에 하는 방식
   - 장점: 실행 모듈 역할은 빨리 정리된다.
   - 단점: rename 직후에도 의존성 혼선이 남아, 결과적으로 두 번 흔든다.
3. rename과 core 주입을 같은 계획 아래 단계적으로 같이 정리하는 방식
   - 장점: 최종 구조가 한 번에 보이고, bootstrap/Gradle/패키지 경계를 같은 기준으로 맞출 수 있다.
   - 단점: 계획 난이도가 높다.

선택: **3번**

이유는 이름과 의존성이 함께 바뀌지 않으면, 이후 패키지 이동과 component scan 정리에서 구버전 명칭이 계속 잔존하기 때문이다. 특히 boot application module은 이름, 디렉터리, Gradle include, package root, test import가 함께 움직여야 drift가 적다.

### 목표 구조
```text
jgitkins
├── core-common
├── core-web
├── core-security
├── core-persistence
├── core-grpc
├── server-api-client
├── context-*
├── app-server
├── app-web
└── app-runner
```

### rename 대상
```text
server     -> app-server
web        -> app-web
runner     -> app-runner
```

### 경로/설정 갱신 대상
```text
settings.gradle
web/settings.gradle
server/build.gradle
web/build.gradle
runner/build.gradle
server/Dockerfile
web/Dockerfile
runner/Dockerfile
server/docker-compose-config/**
web/docker-compose-config/**
runner/docker-compose-config/**
.github/workflows/*.yml
.github/scripts/detect-changed-modules.sh
```

### package root 권장안
```text
io.jgitkins.server  -> io.jgitkins.app.server
io.jgitkins.web     -> io.jgitkins.app.web
io.jgitkins.runner  -> io.jgitkins.app.runner
```

이 계획은 모듈명만 바꾸는 수준이 아니라, boot application의 package root와 component scan 기준까지 같이 맞추는 방향을 기본안으로 둔다.

### 의존 방향
```text
app-server
├── core-common
├── core-web
├── core-security
├── core-persistence
└── core-grpc

app-web
├── core-common
├── core-web
├── core-security
└── server-api-client

app-runner
├── core-common
└── core-grpc
```

### Gradle 기준
```gradle
// settings.gradle
include 'core-common'
include 'core-web'
include 'core-security'
include 'core-persistence'
include 'core-grpc'
include 'server-api-client'

include 'app-server'
include 'app-web'
include 'app-runner'
```

```gradle
// app-server/build.gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-web')
    implementation project(':core-security')
    implementation project(':core-persistence')
    implementation project(':core-grpc')
}
```

```gradle
// app-web/build.gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-web')
    implementation project(':core-security')
    implementation project(':server-api-client')
}
```

```gradle
// app-runner/build.gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-grpc')
}
```

### 핵심 원칙
- `app-*`는 bootJar와 조립 책임만 가진다.
- `core-*`는 실행 app에 종속되지 않는다.
- `app-web`은 `context-*`를 직접 import하지 않는다.
- `app-server`는 `core-*`와 `context-*`를 wiring만 한다.
- `app-runner`는 실행기 역할과 gRPC/client wiring만 가진다.
- rename은 Gradle include, directory name, package root, test import를 함께 바꾸는 것을 원칙으로 한다.

### Subtasks

#### 1. 모듈 rename 및 Gradle include 전환
- 범위: `settings.gradle`, `web/settings.gradle`, root module directory rename, 각 module build script 경로 조정, 모듈별 group/rootProject.name 정렬
- 작업: `server`, `web`, `runner`를 `app-server`, `app-web`, `app-runner`로 rename하고, root include와 referenced project path를 새 이름으로 고친다.
- 완료 기준: `./gradlew projects` 기준 old module name이 사라지고 새 module name만 남으며, nested `web/settings.gradle`도 새 모듈명과 일치한다.

#### 2. app-server boot composition 정리
- 범위: `app-server` bootstrap class, profile config, component scan, security chain, datasource wiring, `server/Dockerfile`, `server/docker-compose-config/**`
- 작업: `core-common`, `core-web`, `core-security`, `core-persistence`, `core-grpc`를 app-server 조립 그래프로 연결한다.
- 완료 기준: app-server가 bootJar를 생산하고, core dependency를 직접 참조하며, 외부 API client 계약에 종속되지 않고, Dockerfile/compose가 새 app-server 경로를 사용하며, 중복 bootstrap code가 남지 않는다.

#### 3. app-web 의존성 수축 및 API client 주입
- 범위: `app-web` presentation, view model mapper, server API client wiring, `web/Dockerfile`, `web/docker-compose-config/**`
- 작업: `app-web`이 domain context를 직접 의존하는 경로를 제거하고, `server-api-client`와 `core-web` envelope만 통해 server 응답을 처리한다.
- 완료 기준: app-web import graph에 `context-*`가 남지 않고, MVC 화면은 `Model`/`String viewName` 계약을 유지하며, web Docker/compose가 새 app-web 경로와 이미지명을 사용한다.

#### 4. app-runner core-grpc/core-common 주입
- 범위: app-runner bootstrap, executor wiring, gRPC client config, command/DTO import, `runner/Dockerfile`, `runner/docker-compose-config/**`, `runner/application-example.yml`
- 작업: runner가 공통 error/exception contract와 gRPC 공통 설정만 사용하도록 정리하고, runner 고유 실행 로직만 남긴다.
- 완료 기준: app-runner가 `core-grpc`와 `core-common`만으로 부팅 가능하고, Docker/compose/env 샘플이 새 app-runner 명칭을 사용하며, server/web 공용 코드 참조가 사라진다.

#### 5. package root 및 component scan 정리
- 범위: `io.jgitkins.app.*`, `@SpringBootApplication`, `@ComponentScan`, `@MapperScan`, test fixtures
- 작업: module rename에 맞춰 package root와 scan 기준을 정렬하고, package convention test를 갱신한다.
- 완료 기준: package root와 module name이 일치하고, scan 범위가 old namespace에 의존하지 않는다.

#### 6. 회귀 검증 및 구조 가드레일 추가
- 범위: architecture test, gradle build, app module boot tests, `.github/workflows/*.yml`, `.github/scripts/detect-changed-modules.sh`
- 작업: app module이 core dependency만 의존하는지, `app-web`이 context를 직접 import하지 않는지, `app-web`만 `server-api-client`를 의존하는지, `bootJar`가 app module에만 남는지 검증한다.
- 완료 기준: 핵심 모듈의 compile/test가 통과하고, GitHub Actions와 changed-module detector도 새 모듈명 기준으로 동작하며, 구조 가드레일이 다음 리팩토링에서 drift를 잡아낸다.

### 단계별 실행 순서
1. `settings.gradle`과 디렉터리 rename을 먼저 맞춘다.
2. `app-server`를 기준으로 boot composition과 core dependency 주입을 확정한다.
3. `app-web`을 client boundary 중심으로 정리한다.
4. `app-runner`를 core-grpc 중심으로 정리한다.
5. package root와 component scan을 한 번에 정리한다.
6. architecture test와 module test를 통과시켜 구조를 고정한다.

### 검증 기준
- `app-server`, `app-web`, `app-runner`가 각각 boot module로 부팅된다.
- `core-*`는 app module을 import하지 않는다.
- `server-api-client`는 `app-web`만 의존한다.
- `app-web`은 `context-*`를 직접 import하지 않는다.
- `bootJar`는 app module에만 남는다.
- `settings.gradle`의 include와 실제 디렉터리 이름이 일치한다.
- package convention test가 old namespace 잔존을 잡아낸다.

### 리스크
- rename과 dependency cut이 동시에 진행되면 import 정리가 한 번에 깨질 수 있다.
  - 대응: rename과 wiring을 phase로 나누고, 각 phase마다 compile/test를 고정한다.
- `app-web`이 급하게 context를 다시 직접 참조할 수 있다.
  - 대응: architecture test로 금지 경로를 고정한다.
- `server-api-client`가 server/app-server 쪽 런타임 의존성으로 번지면 transport boundary가 흐려질 수 있다.
  - 대응: `server-api-client`는 `app-web` 전용 의존으로 고정하고 app-server에는 포함하지 않는다.
- rename 적용 범위에서 Dockerfile, compose, workflow, nested settings를 놓치면 CI는 통과해도 배포가 깨질 수 있다.
  - 대응: rename checklist에 `.github`, Docker, docker-compose, nested settings를 명시하고 phase 1에서 함께 갱신한다.
- package root rename이 늦어지면 모듈명과 Java package명이 어긋난 상태가 남는다.
  - 대응: module rename과 package rename을 같은 계획 안에서 끝낸다.

### 후속 문서
- `.taskmaster/docs/refactor/task_2_33_plan.md`
- `.taskmaster/docs/refactor/task_2_33_core_libraries_plan.md`
