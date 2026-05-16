# jgitkins-web

jgitkins-server와 연동되는 서버 렌더링용 웹 모듈입니다.

## 실행

```bash
./gradlew bootRun
```

기본 포트는 `8081`이며, API 서버 주소는 `application.yml`의 `jgitkins.server.base-url`에서 변경할 수 있습니다.

## Google OAuth 설정

`.env` 또는 실행 환경에 아래 값을 설정하세요.

```bash
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
```

OAuth 리다이렉트 URI는 기본값 기준으로 `http://localhost:8081/login/oauth2/code/google` 입니다.

## PR/MR · 이슈 링크

외부 시스템 링크가 있다면 `application.yml`의 템플릿을 설정하세요.

- `jgitkins.web.pr-url-template`: `{namespace}`, `{repo}` 치환 지원
- `jgitkins.web.issue-url-template`: `{namespace}`, `{repo}` 치환 지원

