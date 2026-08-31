# 등록 요청: 2.127-B — git smart-HTTP 인증 설계와 서블릿 배선

`.taskmaster/tasks/tasks.json` 은 다른 에이전트 소유라 직접 등록하지 않았다. 아래
필드를 그대로 subtask 로 등록하면 된다. 이 문서는 등록되면 지워도 된다.

## 왜 이 문서가 있는가

2.127 은 "git 신원을 클라이언트가 보낸 X-User-Id 헤더에서 읽고, 자격증명은 검증하지
않는다" 였다. 이번 커밋(`c2b0856`)이 **노출은 닫았지만 인증은 설계하지 않았다.** 미룬
쪽을 잃어버리지 않으려고 남긴다.

## 2.127 중 닫힌 것

`c2b0856 fix(git): close the git chain instead of letting it look guarded`

| 2.127 의 testStrategy 항목 | 상태 |
|---|---|
| X-User-Id 헤더를 위조해도 다른 사용자로 해석되지 않는지 | **닫힘.** 해석하는 코드 자체가 삭제됐다. |
| httpBasic 설치 후 잘못된 자격증명이 401 인지 | 미룸 → 2.127-B |
| PAT provider 가 실제 체인에 연결됐는지 | 미룸 → 2.127-B |

구체적으로 한 일:

- `/git/**`, `/**/*.git`, `/**/*.git/**` 체인이 `permitAll` → **`denyAll`**.
- `GitRequestAuthSupport` 삭제. `request.getHeader("X-User-Id")` 를 파싱해 그대로
  신원으로 쓰던 클래스였고, 그것이 git 인가의 유일한 신원 출처였다.
- `GitSmartHttpAuthorizer` 는 이제 `SecurityContext` 의 `AuthenticatedUser` 를 읽는다.
  API 체인과 신원 표현이 하나가 됐다.
- `GitSmartHttpAuthFilter` 삭제. 체인이 전부 거부하는데도 첫 동작이 저장소 조회여서,
  아무것도 서비스하지 않는 경로로 온 미인증 요청이 DB 까지 닿고 있었다. 등록만 해제하고
  클래스를 남기는 선택은 하지 않았다 — 보안 장치처럼 읽히지만 아닌 코드를 남기는 것이
  이 커밋이 닫으려는 문제 그 자체다.
- `GitChainDeniesByDefaultTest` 가 fence 를 단정한다. `denyAll` 을 `permitAll` 로
  되돌리면 5개 중 4개가 깨진다(측정함).

## 왜 지금 인증을 설계하지 않았나

이 경로를 서비스하는 것이 없다. 프로덕션 코드에 `GitServlet` 도
`ServletRegistrationBean` 도 없고(주석 제외 grep 0건), 그것을 구동할 두 pack factory
(`FetchEventUploadPackFactory`, `PushEventReceivePackFactory`)는 서로와
`GitSmartHttpAuthorizer` 밖에 소비자가 없다. 즉 이 결함은 "지금 털린다" 가 아니라
"서블릿을 배선하는 순간 인증이 없다" 였고, 2.127 원문도 그렇게 적고 있었다.

그 상태에서 httpBasic 을 설치하고 PAT provider 를 연결하는 것은 **PAT-over-Basic 이냐
SSH 키냐, `ROLE_GIT` 이 무엇을 뜻하냐, 공개 저장소의 익명 fetch 를 어떻게 유지하냐** 를
죽은 코드에 대고 정하는 일이 된다. end-to-end 로 확인할 방법이 없으니 틀려도 틀린 줄
모른다. 그래서 서블릿 작업과 같은 묶음으로 미뤘다.

`denyAll` 은 "실수로 열리지 않는" 형태의 닫힘이다. 누군가 그 한 줄을 의도적으로 고치지
않고 서블릿만 등록하면 전부 403 이 되고, 조용히 무인증으로 열리지 않는다.

## 아직 남아 있는 것 (2.127-B 의 범위)

- `PatAuthenticationProvider`, `PatTokenAuthenticationService` (BCrypt PAT 검증) 는
  `@Component` 로 등록만 되고 어떤 `AuthenticationManager` / `ProviderManager` 에도
  연결되지 않은 고아다. 실측: `authenticationProvider(` grep 0건.
- 공개 저장소의 익명 fetch 규칙은 코드에서 사라지지 않았다.
  `GitSmartHttpAuthorizer.authorizeRead` 가 `resolveVisibility` 로 직접 판정한다.
  다만 receive-pack 은 항상 challenge 한다는 나머지 절반은 `GitSmartHttpAuthFilter` 와
  함께 지워졌으므로 2.127-B 가 다시 세워야 한다.
- `app-web/src/main/resources/templates/repositories/detail.html:190` 이 clone URL 을
  복사 입력창에 보여준다. `RepositoryPersistenceAdapter:376` 이
  `"/" + String.join("/", segments) + ".git"` 를 만들고 `CloneUrlBuilder` 가
  `{scheme}://{host}{clonePath}` 로 조립한다. 즉 UI 는 이 체인으로 떨어지는 URL 을
  광고하고 있다. 이번 커밋으로 그 URL 의 응답이 바뀌었다:

  | 대상 | 이전 | 지금 |
  |---|---|---|
  | 공개 저장소 / 없는 저장소 | 404 (서블릿이 없어서) | 403 |
  | 비공개 저장소 | 401 + `WWW-Authenticate: Basic` | 403 |

  비공개 쪽 401 은 브라우저에 basic-auth 대화상자를 띄웠지만 그 자격증명을 검증하는
  코드는 없었다 — 값을 파싱조차 하지 않았다. 그러니 403 으로 바뀐 것은 손실이 아니다.
  다만 clone 을 시도하는 사용자가 보는 문구가 달라진다는 점은 2.127-B 가 UX 로 다뤄야
  한다. 이 표면을 지우지 않고 fence 로 둔 이유이기도 하다.

## 등록용 필드

```
id:           2.127-B  (또는 다음 순번)
title:        [security][git][app-server] git smart-HTTP 인증을 설계하고 서블릿을 배선한다 — 2.127 의 미룬 절반
priority:     high
dependencies: [2.126]   ← 아래 "순서" 참고
status:       pending
```

**description / details**

> 2.127 에서 노출은 닫았고(`c2b0856`, git 체인 `denyAll` + X-User-Id 신원 삭제) 인증
> 설계는 미뤘다. 이 task 가 그 절반이다. 할 일: (1) git 체인에 실제 인증 기구를
> 설치한다 — `http.httpBasic(...)` 은 위치 지정이 아니라 설치여야 하고,
> `addFilterBefore(..., BasicAuthenticationFilter.class)` 는 그 필터를 추가하지 않는다는
> 점을 다시 밟지 말 것. (2) 고아 상태인 `PatAuthenticationProvider` /
> `PatTokenAuthenticationService` 를 `AuthenticationManager` 에 연결하거나, PAT 를 쓰지
> 않기로 정했다면 삭제한다 — 연결도 삭제도 안 된 상태가 2.127 을 만든 조건이었다.
> (3) `GitServlet` 을 `ServletRegistrationBean` 으로 배선하고 두 pack factory
> (`FetchEventUploadPackFactory`, `PushEventReceivePackFactory`)를 소비자에 연결한다.
> (4) 공개 저장소의 익명 fetch 는 열고 receive-pack 은 항상 challenge 하는 규칙을 다시
> 세운다 — 읽기 절반은 `GitSmartHttpAuthorizer.authorizeRead` 의 `resolveVisibility`
> 로 남아 있고, receive-pack 절반은 `GitSmartHttpAuthFilter` 와 함께 지워졌다.
> (5) `SecurityConfig` 의 `denyAll` 을 이 task 안에서 **의도적으로** 교체한다. 그 한 줄과
> `GitChainDeniesByDefaultTest` 를 같이 고치지 않으면 빌드가 깨지도록 만들어 뒀다.
> 결정해야 할 설계 질문: PAT-over-Basic 이냐 SSH 키냐(둘 다면 우선순위), `ROLE_GIT` 의
> 의미, PAT 스코프 모델. 이것들을 죽은 코드에 대고 정하지 않으려고 미룬 것이므로,
> 서블릿을 먼저 세우고 실제 `git clone` / `git push` 로 확인하면서 정한다.

**testStrategy**

> `git clone` / `git push` 를 실제로 수행하는 통합 테스트. 잘못된 자격증명이 401 이고
> 올바른 PAT 가 통과하는지. 비공개 저장소를 익명으로 clone 하면 401 인지. 공개 저장소는
> 익명 clone 이 되고 push 는 401 인지. 쓰기 권한 없는 사용자의 push 가 403 인지.
> `GitChainDeniesByDefaultTest` 는 이 task 에서 "무엇이 열렸는지" 를 단정하는 테스트로
> 대체된다 — 삭제만 하고 대체하지 않으면 회귀 감지가 사라진다.

## 순서

2.133(api 체인을 `authenticated()` 로 뒤집기)은 개별 라우트 분류가 끝난 뒤에 해야
한다고 스스로 적고 있고 그 목록에 2.127 이 있다. git 체인은 이제 분류가 끝났다(전부
거부). **따라서 2.133 은 2.127-B 를 기다리지 않는다.**

2.133 이 실제로 기다리는 것은 **2.126(runner 인증)** 이다. runner 라우트에는 지금 어느
계층에도 가드가 없어서, 기본을 `authenticated()` 로 뒤집으면 runner 가 즉시 깨지거나
공개 목록에 무근거로 올라간다. 2.126 은 사용자 요청으로 보류 중이므로, 2.133 을 하려면
그 보류를 먼저 풀어야 한다.
