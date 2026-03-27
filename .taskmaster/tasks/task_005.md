# Task ID: 5

**Title:** 버그/핫픽스

**Status:** done

**Dependencies:** None

**Priority:** high

**Description:** 사용자 영향 버그 및 긴급 수정 작업

**Details:**

카테고리 기반 상위 Task

**Test Strategy:**

재현/수정/회귀 테스트

## Subtasks

### 5.1. repositories/new owner namespace 초기화 계약 복구 핫픽스

**Status:** done  
**Dependencies:** None  

repositories/new 진입 시 owner 영역에 로그인 사용자 namespace 가 노출되지 않는 문제에 대해 원인 분석, view-model 계약 복구 기준 정리, owner slug 소스 분리, 최소 변경 범위 정의를 수행하는 핫픽스 단위로 관리한다.

**Details:**

[source: jgitkins-server, original subtask: 5.2]
<info added on 2026-03-18T04:52:03.578Z>
repositories/new 생성 화면의 owner namespace 초기화 계약 복구를 완료했습니다. io.jgitkins.web.presentation.support.RepositoryViewSupport.populateCreateModel(...) 메서드에서 ownerLabel, ownerSlug, organizes, organizeError 속성을 평면 모델로 다시 주입하도록 수정되었습니다. io.jgitkins.web.presentation.controller.RepositoryController는 io.jgitkins.web.support.SessionSupport를 통해 세션 username을 읽어 io.jgitkins.web.facade.RepositoryFacade.getInitData(...) 메서드에 전달하도록 변경했습니다. io.jgitkins.web.facade.RepositoryFacade는 개인 owner slug 계산 시 OAuth 표시명 대신 세션 username을 namespace 소스로 사용하도록 조정되었습니다. io.jgitkins.web.presentation.controller.RepositoryControllerTest에 newRepository 진입 시 세션 username이 getInitData로 전달되는 회귀 테스트를 추가했으며, `./gradlew test --tests io.jgitkins.web.presentation.controller.RepositoryControllerTest` 검증을 통과했습니다.
</info added on 2026-03-18T04:52:03.578Z>
