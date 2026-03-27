# Task ID: 1

**Title:** 신규기능

**Status:** pending

**Dependencies:** None

**Priority:** high

**Description:** 사용자 가치 전달을 위한 기능 개발/확장 작업

**Details:**

기존 기능별 Task를 카테고리 기반(신규기능/리팩토링/보안)으로 재구성함.

**Test Strategy:**

카테고리별 우선순위에 따라 하위 작업을 순차 수행하고 회귀 테스트를 적용한다.

## Subtasks

### 1.1. [runner] 복잡한 Jenkinsfile 실행 지원 범위 검증 및 Job/Runner 보완

**Status:** pending  
**Dependencies:** None  

복잡한 Jenkinsfile 시나리오를 기준으로 Job 생성부터 Runner 실행까지 현재 지원 범위를 검증하고, 필요한 기능 보완 포인트를 도출한다.

**Details:**

[source: jgitkins-server, original subtask: 1.16]
이 작업은 단순 테스트가 아니라 향후 Job 생성 및 Runner 실행 기능 확장을 위한 신규기능 성격의 검증 과제다. 우선 대표적인 복잡한 Jenkinsfile 시나리오를 선정한다. 예: scripted pipeline, declarative pipeline, parallel stage, matrix/conditional, shared library 의존, shell/script step 다수, credential 사용, agent/docker 지정, 아티팩트/워크스페이스 활용 등. 각 시나리오별로 현재 서버가 push 이후 Job 을 어떻게 생성하는지, 어떤 payload 로 Runner 에 전달하는지, Runner 가 어떤 runtime/plugin/script 제약으로 실패하는지 확인한다. 검토 및 구현 범위는 다음을 포함한다. 1) 복잡한 Jenkinsfile 샘플 세트 정의. 2) Job 생성 입력 모델과 Jenkinsfile 선택/해석 규칙이 복잡한 스크립트 구조를 수용하는지 점검. 3) Runner 측 실행 환경, plugin, 이미지, credential, filesystem/workspace 요구사항 정리. 4) 성공/실패/skip/timeout 케이스별 상태 반영과 로그 수집 전략 확인. 5) 실제 검증 결과를 바탕으로 서버/runner 어느 쪽에 기능 보완이 필요한지 후속 Task 로 분리한다. 산출물은 지원 가능 시나리오, 실패 시나리오, 즉시 보완 항목, 후속 개발 항목 목록이다.

### 1.2. [web, server] Develop File Content Viewer UI Component with WYSIWYG

**Status:** pending  
**Dependencies:** None  

Create a frontend UI component to display file content, integrating a WYSIWYG editor for rendering various file types (e.g., Markdown, code, plain text).

**Details:**

[source: jgitkins-server, original subtask: 1.19]
Implement a component (e.g., `FileContentViewer`) that accepts raw file content and its type (derived from file extension).
1.  For Markdown files (`.md`, `.markdown`), integrate a Markdown renderer library (e.g., `react-markdown`, `marked.js`) to display rendered HTML.
2.  For code files (e.g., `.java`, `.js`, `.py`, `.xml`), integrate a code highlighter/editor (e.g., Monaco Editor, CodeMirror, Prism.js) to display syntax-highlighted code.
3.  For plain text files, display the content directly.
4.  For unknown or binary files, display a message indicating content cannot be rendered directly.

The component should act as a viewer (read-only) as per the PRD's '읽을 수 있는 기능을 제공한다' (provides the ability to read).

### 1.3. [server] gRPC 공통 예외 매핑 및 Status 응답 표준화

**Status:** pending  
**Dependencies:** None  

gRPC controller에서 전파되는 application/domain/infrastructure 예외를 공통 계층에서 gRPC Status 코드로 변환하고, HTTP GlobalExceptionHandler와 별도로 gRPC 예외 처리 정책을 표준화한다

**Details:**

[source: jgitkins-server, original subtask: 1.24]

### 1.4. [server, web] Repository 화면 기능 버튼 인증/인가 제어

**Status:** pending  
**Dependencies:** 1.2  

Public repository 조회는 허용하되 쓰기성 기능은 인증/권한 사용자로 제한한다.

**Details:**

[source: jgitkins-web, original subtask: 1.3]
브랜치 생성/파일 업로드/디렉터리 생성 버튼 노출 및 서버 호출 권한을 일관되게 통제한다.

### 1.5. [server, runner] Feature Plugin Management

**Status:** pending  
**Dependencies:** None  

Add a first-class plugin management workflow so each runner profile can declare the Jenkins plugins required for a pipeline, persist those definitions in the CLI config, and ensure the Docker executor mounts the resolved bundle before calling the server.

**Details:**

[source: jgitkins-runner, original subtask: 1.2]
- Teach the CLI config loader to read/write a `plugins` section in `~/.jgitkins-runner/config.yml`, keep named bundles per profile, and expose Picocli flags that let `run` reference a specific `plugins.yaml` manifest.
- Introduce a parser (e.g., `PluginBundleLoader` under `src/main/java/io/jgitkins/runner/infrastructure/plugins`) that reads the YAML, downloads/cache `.jpi` files, and mirrors Jenkinsfile Runner’s expected directory layout before execution.
- Extend `JobExecuteRequest` (`src/main/java/io/jgitkins/runner/presentation/api/dto/JobExecuteRequest.java`) and related DTO/command objects so `/pipelines/run` can accept an optional plugin bundle identifier or manifest path; default to the active profile’s bundle.
- Update `DockerRunnerAdapter.run` (`src/main/java/io/jgitkins/runner/infrastructure/adapter/DockerRunnerAdapter.java`) so the `Bind` list includes both the workspace and the resolved plugin volume (mount to `/usr/share/jenkins/ref/plugins` or configurable `runner.plugins.mountPath`).
- Provide CLI subcommands such as `plugins list`, `plugins sync`, and `plugins use` to inspect/update bundles, persisting changes back to the YAML config alongside existing registration data.
- Document plugin workflow in `README.md`, including how to author `plugins.yaml`, where bundles are cached, and how to override bundles per `run` invocation.
Pseudo:
```
PluginBundle bundle = pluginBundleLoader.load(activeProfile);
Bind plugins = new Bind(bundle.getPath(), new Volume(config.getPluginsMountPath()));
CreateContainerResponse container = dockerClient.createContainerCmd(image)
    .withBinds(workspaceBind, plugins)
    .exec();
```

