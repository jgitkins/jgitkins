# 리팩토링 계획서

### 제목
- **리팩토링 계획**: Task 2.4 멀티모듈 로깅 Java Configuration 표준화 계획서

### 배경 (왜?)
- `server`는 이미 `LoggingConfigurator + META-INF/spring.factories` 기반 Java Configuration을 사용한다.
- `runner`와 `web`는 아직 `logback-spring.xml`을 사용하므로 모듈별 로깅 초기화 방식이 다르다.
- `runner/web` XML에는 `traceId` 패턴과 async appender가 없고, `runner`는 root level이 `OFF`라 운영 로그 가시성이 낮다.
- `web`는 `SopsEnvironmentPostProcessor`를 이미 `spring.factories`로 등록하고 있어 로깅 전환 시 공존 조건을 함께 고려해야 한다.

### 목표 (Goals)
- `runner`, `web`의 XML 로깅을 Java 기반 `LoggingConfigurator`로 전환한다.
- `server`를 기준선으로 traceId 패턴, async appender, 등록 방식을 정렬한다.
- 모듈별 root/logger level 차이는 필요한 경우 예외로 문서화한다.
- 구현 시 XML 제거 범위와 검증 기준을 명확히 고정한다.

### 범위 (Scope)
- **수정 대상**: `runner/src/main/resources/logback-spring.xml`
- **수정 대상**: `web/src/main/resources/logback-spring.xml`
- **수정 대상**: `runner/src/main/java/io/jgitkins/runner/infrastructure/config/LoggingConfigurator.java`
- **수정 대상**: `web/src/main/java/io/jgitkins/web/infrastructure/config/LoggingConfigurator.java`
- **수정 대상**: `runner/src/main/resources/META-INF/spring.factories`
- **수정 대상**: `web/src/main/resources/META-INF/spring.factories`
- **참조 대상**: `server/src/main/java/io/jgitkins/server/infrastructure/config/LoggingConfigurator.java`
- **참조 대상**: `server/src/main/resources/META-INF/spring.factories`
- **수정 제외 대상**: 로깅과 무관한 XML, `SopsEnvironmentPostProcessor` 내부 구현, 공통 모듈 추출 작업은 제외한다.

### 방법 조사 및 선택
- **방안 1**: `server` 구현을 `runner/web`에 그대로 복제한다.
  장점은 빠르다.
  단점은 모듈별 차이를 반영하기 어렵다.
- **방안 2**: 공통 로깅 모듈을 먼저 만든 뒤 세 모듈을 함께 정리한다.
  장점은 중복이 가장 적다.
  단점은 이번 작업 범위를 과도하게 키운다.
- **방안 3**: `server`를 기준선으로 삼되 `runner/web`에 모듈별 `LoggingConfigurator`를 먼저 도입한다.
  장점은 범위를 통제하면서 XML 제거와 초기화 방식 통일을 바로 달성할 수 있다.
  단점은 중복이 일부 남는다.
- **선택 방안**: 방안 3을 선택한다.
- **선택 이유**: 이번 Task의 핵심은 공통화보다 `runner/web`를 `server`와 같은 초기화 방식으로 옮기는 준비를 끝내는 데 있다.

### 모듈 현황 비교
- `server`: local `INFO`, non-local `WARN`, traceId 패턴, async console/file appender 사용
- `runner`: local/non-local 모두 `OFF`, traceId 패턴 없음, async appender 없음, server 전용 logger preset 일부 잔존
- `web`: local `INFO`, non-local `WARN`, traceId 패턴 없음, async appender 없음, `spring.factories`에 SOPS 등록 존재

### 변경 대상 BEFORE / AFTER 목록
- `runner/logback-spring.xml`은 제거한다.
- `runner/infrastructure/config/LoggingConfigurator.java`는 신규 추가한다.
- `runner/META-INF/spring.factories`는 신규 추가한다.
- `runner` 로깅은 XML 기반 sync 설정에서 Java 기반 async 설정으로 전환한다.
- `runner`의 server 전용 logger preset은 runner 기준으로 정리한다.
- `web/logback-spring.xml`은 제거한다.
- `web/infrastructure/config/LoggingConfigurator.java`는 신규 추가한다.
- `web/META-INF/spring.factories`는 SOPS 등록 유지 후 `ApplicationListener`를 추가한다.
- `web` 로깅은 XML 기반 sync 설정에서 Java 기반 async 설정으로 전환한다.
- `server` 설정은 구현 대상이 아니라 기준선으로 유지한다.

### 계획 (Plan)
- **단계 1**: `server`, `runner`, `web`의 root level, logger preset, 패턴, appender 차이를 확정한다.
- **단계 2**: `runner`용 `LoggingConfigurator` 구조와 `spring.factories` 등록 방식을 정한다.
- **단계 3**: `web`용 `LoggingConfigurator` 구조와 SOPS 공존 방식을 정한다.
- **단계 4**: XML 제거 순서와 모듈별 예외값을 확정한다.
- **단계 5**: local / non-local 부팅 검증 절차를 정리한다.

### 검증 기준
- `runner/web`는 XML 제거 후에도 부팅 시 로그가 정상 출력되어야 한다.
- `ApplicationEnvironmentPreparedEvent` 시점에 console appender가 초기화되어야 한다.
- non-local 환경에서는 file appender와 파일명 패턴이 유지되어야 한다.
- 공통 패턴에는 `%X{traceId:-}`가 포함되어야 한다.
- `web`는 SOPS와 LoggingConfigurator가 함께 등록되어도 충돌이 없어야 한다.
- `runner`의 잘못된 logger preset은 제거 또는 교체되어야 한다.

### 개선 사항 점검
- **개선안 1**: 모듈별 root/logger level 표를 별도로 정리한다.
- **개선안 2**: appender 생성 헬퍼를 두어 `runner/web` 구현 중복을 줄인다.
- **개선안 3**: 후속 작업으로 공통 로깅 모듈 추출 여부를 다시 검토한다.
- **선택 개선안**: 개선안 1, 2를 반영하고 개선안 3은 후속 과제로 남긴다.

### 기대효과 (Expected Benefits)
- 멀티모듈 로깅 초기화 방식이 통일된다.
- traceId와 async appender가 `runner/web`에도 적용되어 운영 관측성이 좋아진다.
- XML 제거로 설정 변경이 코드 리뷰 대상에 더 명확히 드러난다.
- `web`의 SOPS 공존 규칙이 명확해져 부팅 회귀 위험을 줄인다.

### 예시 (방안 3 기준 코드 스니펫)

#### AS-IS (현재 구조)
```xml
<SpringProfile name="!local">
    <root level="WARN">
        <appender-ref ref="RollingFile" />
        <appender-ref ref="STDOUT" />
    </root>
</SpringProfile>
```

#### TO-BE (개선 제안 구조)
```java
public class LoggingConfigurator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConfigurableEnvironment environment = event.getEnvironment();
        loggerContext.reset();
        configureCommonLoggers(loggerContext);
        configureConsole(loggerContext, environment);
        configureFileAppenderIfNeeded(loggerContext, environment);
    }
}
```

```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
io.jgitkins.web.infrastructure.config.SopsEnvironmentPostProcessor

org.springframework.context.ApplicationListener=\
io.jgitkins.web.infrastructure.config.LoggingConfigurator
```

### 주의사항
- **포맷팅 금지**: 코드 포맷팅은 수행하지 않는다.
- **기존 기능 보장**: 부팅, 로그 출력, SOPS 초기화는 유지되어야 한다.
- **계획우선**: 계획 문서 단계에서는 구현하지 않는다.
- **문서체규약**: 모든 문장은 공식 문서체로 유지한다.

### 결론
- Task 2.4는 `runner/web`의 XML 로깅을 `server`와 같은 초기화 메커니즘으로 옮기기 위한 준비 작업으로 정의한다.
- 구현은 모듈별 `LoggingConfigurator` 도입과 `spring.factories` 정렬을 우선하고, 공통화는 후속 과제로 분리한다.
