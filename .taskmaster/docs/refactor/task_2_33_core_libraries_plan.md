# Task 2.33 Core Libraries 분리 계획

### 범위
- 이번 문서는 `core-*` library module만 먼저 분리하는 실행 계획이다.
- `context-*`, `app-*` rename 또는 source 이동은 이번 범위에서 제외한다.
- 기존 실행 모듈 이름 `server`, `web`, `runner`는 유지한다.
- Boot plugin은 기존 실행 모듈에만 유지하고, 신규 `core-*`는 `java-library`만 사용한다.

### 1차 목표 모듈
```text
core-common
core-web
core-security
core-persistence
core-grpc
```

### 분리 원칙
- `core-*`는 business context를 의존하지 않는다.
- `core-*`는 `server`, `web`, `runner`를 의존하지 않는다.
- `core-*`는 `bootJar`를 만들지 않는다.
- `core-*`는 Boot plugin을 쓰지 않으므로 Spring dependency version은 Spring Boot BOM으로 명시한다.
- `core-persistence`에는 설정만 둔다. mapper/entity/adapter는 이동하지 않는다.
- `core-web`은 JSON API transport 공통과 MVC 화면 공통을 구분해서 둔다. controller는 이동하지 않는다.
- `ApiResponse`는 Thymeleaf MVC 화면 응답용이 아니다. server API 호출/응답과 REST API envelope에만 사용한다.
- `core-security`에는 재사용 가능한 보안 helper/handler만 둔다. app별 `SecurityFilterChain`은 이동하지 않는다.
- `core-grpc`는 공통 gRPC dependency/version/config 기준만 먼저 제공한다. proto/stub 이동은 후속 작업으로 둔다.

### 최종 의존 방향
```text
server
├── core-common
├── core-web
├── core-security
├── core-persistence
└── core-grpc

web
├── core-common
├── core-web
└── core-security

runner
├── core-common
└── core-grpc
```

### 후속 연계
- `server-api-client`는 이번 core library 분리 범위가 아니라 `app-web` 전용 client SDK로 후속 계획에서 다룬다.
- `app-server`는 server-api-client를 의존하지 않는다.

### `settings.gradle`
```gradle
rootProject.name = 'jgitkins'

include 'core-common'
include 'core-web'
include 'core-security'
include 'core-persistence'
include 'core-grpc'

include 'runner'
include 'server'
include 'web'
```

### 공통 library build template
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven {
        url "https://repo.spring.io/milestone"
    }
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### `core-common`

#### 책임
- 공통 error/problem/exception contract
- context와 app에 독립적인 작은 utility
- Spring Web, MyBatis, Security 의존 금지

#### 1차 이동 후보
```text
FROM server/src/main/java/io/jgitkins/server/common/**
TO   core-common/src/main/java/io/jgitkins/core/common/**
```

#### `core-common/build.gradle`
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

#### package 예시
```java
package io.jgitkins.core.common.problem;

import io.jgitkins.core.common.error.ErrorCode;

public interface ProblemSpec<T extends ErrorCode> {

    T getErrorCode();

    String getCode();

    String getDefaultMessage();

    String getMessageKey();
}
```

```java
package io.jgitkins.core.common.exception;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.problem.ProblemSpec;

public abstract class JgitkinsException extends RuntimeException {

    private final ProblemSpec<? extends ErrorCode> problemSpec;

    protected JgitkinsException(ProblemSpec<? extends ErrorCode> problemSpec, String message) {
        super(message);
        this.problemSpec = problemSpec;
    }

    public ErrorCode getErrorCode() {
        return problemSpec.getErrorCode();
    }

    public String getProblemCode() {
        return problemSpec.getCode();
    }

    public String getDefaultMessage() {
        return problemSpec.getDefaultMessage();
    }
}
```

### `core-web`

#### 책임
- JSON API envelope
- 공통 API error body
- REST response factory
- server API client가 사용하는 response envelope
- MVC 화면 공통 util은 별도 하위 패키지로 분리한다.

#### 중요한 제한
- `ApiResponse`는 `@RestController` 또는 `RestClient`/`WebClient` transport boundary에서만 사용한다.
- `web` 모듈의 Thymeleaf `@Controller`는 `String viewName`, `Model`, `ModelAndView`를 계속 사용한다.
- 화면 controller가 `ApiResponse`를 모델에 넣는 방식은 금지한다.
- `web` 모듈이 `core-web`을 의존하더라도, 주된 사용처는 server API client의 JSON envelope 역직렬화와 공통 MVC utility다.

#### 1차 이동 후보
```text
FROM server/src/main/java/io/jgitkins/server/presentation/common/ApiResponse.java
TO   core-web/src/main/java/io/jgitkins/core/web/api/response/ApiResponse.java

FROM server/src/main/java/io/jgitkins/server/presentation/common/ApiError.java
TO   core-web/src/main/java/io/jgitkins/core/web/api/response/ApiError.java

FROM server/src/main/java/io/jgitkins/server/presentation/util/LocationUriBuilder.java
TO   core-web/src/main/java/io/jgitkins/core/web/api/uri/LocationUriBuilder.java
```

#### 이번 범위 제외
```text
server/src/main/java/io/jgitkins/server/presentation/api/**
server/src/main/java/io/jgitkins/server/repository/presentation/api/**
server/src/main/java/io/jgitkins/server/execution/presentation/api/**
```

#### `core-web/build.gradle`
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

dependencies {
    api project(':core-common')

    api 'org.springframework:spring-web'
    implementation 'org.springframework:spring-webmvc'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

#### package 구분
```text
core-web/src/main/java/io/jgitkins/core/web/api/response/ApiResponse.java
core-web/src/main/java/io/jgitkins/core/web/api/response/ApiError.java
core-web/src/main/java/io/jgitkins/core/web/api/uri/LocationUriBuilder.java

core-web/src/main/java/io/jgitkins/core/web/mvc/support/ViewModelMapper.java
core-web/src/main/java/io/jgitkins/core/web/mvc/config/MvcViewConfiguration.java
```

#### `ApiResponse` 목표 형태
```java
package io.jgitkins.core.web.api.response;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.problem.ProblemSpec;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ApiResponse<T> {

    private final T data;
    private final ApiError error;

    private ApiResponse(T data, ApiError error) {
        this.data = data;
        this.error = error;
    }

    private static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    private static ApiResponse<Void> success() {
        return new ApiResponse<>(null, null);
    }

    private static ApiResponse<Void> failure(ErrorCode errorCode, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(errorCode, message, source));
    }

    private static ApiResponse<Void> failure(ProblemSpec<? extends ErrorCode> problemSpec,
                                             String message,
                                             String source) {
        return new ApiResponse<>(null, ApiError.of(problemSpec, message, source));
    }

    private static ApiResponse<Void> failure(String code, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(code, message, source));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T body) {
        return ResponseEntity.ok(success(body));
    }

    public static ResponseEntity<ApiResponse<Void>> ok() {
        return ResponseEntity.ok(success());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(Object resourceId, T body) {
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resourceId)
                .toUri();
        return ResponseEntity.created(location).body(success(body));
    }

    public static ResponseEntity<ApiResponse<Void>> created(URI location) {
        return ResponseEntity.created(location).body(success());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(URI location, T body) {
        return ResponseEntity.created(location).body(success(body));
    }

    public static ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(success());
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          ErrorCode errorCode,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(errorCode, message, source));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          ProblemSpec<? extends ErrorCode> problemSpec,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(problemSpec, message, source));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          String code,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(code, message, source));
    }

    public static ApiResponse<Void> errorBody(ProblemSpec<? extends ErrorCode> problemSpec,
                                              String message,
                                              String source) {
        return failure(problemSpec, message, source);
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
```

### `core-security`

#### 책임
- 재사용 가능한 security response handler
- JWT utility 후보
- OAuth/client/app별 security chain은 제외

#### 1차 이동 후보
```text
FROM server/src/main/java/io/jgitkins/server/infrastructure/config/security/handler/ApiAccessDeniedHandler.java
TO   core-security/src/main/java/io/jgitkins/core/security/handler/ApiAccessDeniedHandler.java

FROM server/src/main/java/io/jgitkins/server/infrastructure/config/security/handler/ApiAnauthorizeHandler.java
TO   core-security/src/main/java/io/jgitkins/core/security/handler/ApiUnauthorizedHandler.java
```

#### `core-security/build.gradle`
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

dependencies {
    api project(':core-common')
    api project(':core-web')

    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'jakarta.servlet:jakarta.servlet-api:6.0.0'
    implementation 'org.springframework:spring-web'
    implementation 'org.springframework.security:spring-security-web'
    implementation 'org.springframework.security:spring-security-core'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

#### handler 목표 형태
```java
package io.jgitkins.core.security.handler;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.problem.ProblemSpec;

public record ApiSecurityProblem(
        ProblemSpec<? extends ErrorCode> problemSpec,
        String message,
        String source
) {
}
```

```java
package io.jgitkins.core.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.core.web.api.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, int status, ApiResponse<Void> payload) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), payload);
    }
}
```

```java
package io.jgitkins.core.security.handler;

import io.jgitkins.core.web.api.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;
    private final ApiSecurityProblem forbiddenProblem;

    public ApiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter,
                                  ApiSecurityProblem forbiddenProblem) {
        this.responseWriter = responseWriter;
        this.forbiddenProblem = forbiddenProblem;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ApiResponse<Void> payload = ApiResponse.errorBody(
                forbiddenProblem.problemSpec(),
                forbiddenProblem.message(),
                forbiddenProblem.source());
        responseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, payload);
    }
}
```

#### app module wiring 예시
```java
package io.jgitkins.server.infrastructure.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.core.security.handler.ApiAccessDeniedHandler;
import io.jgitkins.core.security.handler.ApiSecurityProblem;
import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityHandlerConfiguration {

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    ApiAccessDeniedHandler apiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        ApiSecurityProblem forbiddenProblem = new ApiSecurityProblem(
                ApplicationProblemSpec.ACCESS_DENIED,
                ApplicationProblemSpec.ACCESS_DENIED.getDefaultMessage(),
                "application");
        return new ApiAccessDeniedHandler(responseWriter, forbiddenProblem);
    }
}
```

### `core-persistence`

#### 책임
- DataSource 공통 설정
- MyBatis 공통 설정
- transaction 공통 설정
- mapper scan helper 또는 base configuration

#### 1차 이동 후보
```text
FROM server/src/main/java/io/jgitkins/server/infrastructure/config/persistence/DataSourceConfig.java
TO   core-persistence/src/main/java/io/jgitkins/core/persistence/DataSourceConfig.java

FROM server/src/main/java/io/jgitkins/server/infrastructure/config/persistence/MybatisConfig.java
TO   core-persistence/src/main/java/io/jgitkins/core/persistence/MybatisConfig.java
```

#### 이번 범위 제외
```text
server/src/main/java/io/jgitkins/server/infrastructure/persistence/model/**
server/src/main/java/io/jgitkins/server/infrastructure/persistence/mapper/**
server/src/main/java/io/jgitkins/server/repository/infrastructure/adapter/persistence/**
server/src/main/java/io/jgitkins/server/execution/infrastructure/adapter/persistence/**
```

#### `core-persistence/build.gradle`
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

dependencies {
    api 'org.springframework:spring-jdbc'
    api 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'

    implementation 'com.zaxxer:HikariCP'

    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
    runtimeOnly 'org.bgee.log4jdbc-log4j2:log4jdbc-log4j2-jdbc4.1:1.16'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'com.h2database:h2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

#### driver dependency 기준
- `core-persistence`의 `runtimeOnly` driver는 실행 app의 runtimeClasspath에 전파되는지 구현 중 확인한다.
- 전파가 명확하지 않거나 운영 packaging에서 누락되면 app module에도 driver를 명시한다.
```gradle
// app module fallback
runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
runtimeOnly 'org.bgee.log4jdbc-log4j2:log4jdbc-log4j2-jdbc4.1:1.16'
```

#### configuration 목표 형태
```java
package io.jgitkins.core.persistence;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class CoreDataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource dataSource(HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
}
```

```java
package io.jgitkins.core.persistence;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreMybatisConfiguration {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        return sessionFactory.getObject();
    }
}
```

### `core-grpc`

#### 책임
- gRPC dependency/version 기준
- gRPC 공통 config
- status mapping helper
- proto/stub source 이동은 후속 작업

#### `core-grpc/build.gradle`
```gradle
plugins {
    id 'java-library'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'com.google.protobuf' version '0.9.4'
}

group = 'io.jgitkins.core'
version = '0.0.1'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.boot:spring-boot-dependencies:3.2.3"
    }
}

ext {
    grpcVersion = '1.62.2'
    protobufVersion = '3.25.3'
}

dependencies {
    api "io.grpc:grpc-protobuf:${grpcVersion}"
    api "io.grpc:grpc-stub:${grpcVersion}"
    api "io.grpc:grpc-netty-shaded:${grpcVersion}"
    api "com.google.protobuf:protobuf-java:${protobufVersion}"

    compileOnly 'javax.annotation:javax.annotation-api:1.3.2'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
}

tasks.named('test') {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${protobufVersion}"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
        }
    }
    generateProtoTasks {
        all().each { task ->
            task.plugins {
                grpc {}
            }
        }
    }
}
```

### 기존 모듈 의존성 조정

#### `server/build.gradle`
```gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-web')
    implementation project(':core-security')
    implementation project(':core-persistence')
    implementation project(':core-grpc')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'

    implementation 'org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r'
    implementation 'org.eclipse.jgit:org.eclipse.jgit.http.server:7.3.0.202506031305-r'

    implementation 'net.devh:grpc-server-spring-boot-starter:2.15.0.RELEASE'

    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
}
```

#### `web/build.gradle`
```gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-web')
    implementation project(':core-security')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.session:spring-session-data-redis'
    implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
}
```

#### `web` 모듈 사용 기준
```java
// OK: server API client에서 JSON envelope 역직렬화
package io.jgitkins.web.client.repository;

import io.jgitkins.core.web.api.response.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RepositoryApiClient {

    private final RestClient restClient;

    public RepositoryApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public RepositoryOverviewClientDto getOverview(String namespace, String repoName, String branch) {
        ApiResponse<RepositoryOverviewClientDto> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/internal/repositories/{namespace}/{repoName}/overview")
                        .queryParam("branch", branch)
                        .build(namespace, repoName))
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<RepositoryOverviewClientDto>>() {});

        return response.getData();
    }
}
```

```java
// OK: Thymeleaf MVC controller는 ViewModel과 Model을 사용
package io.jgitkins.web.presentation.repository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RepositoryPageController {

    private final RepositoryApiClient repositoryApiClient;
    private final RepositoryOverviewViewModelMapper viewModelMapper;

    public RepositoryPageController(RepositoryApiClient repositoryApiClient,
                                    RepositoryOverviewViewModelMapper viewModelMapper) {
        this.repositoryApiClient = repositoryApiClient;
        this.viewModelMapper = viewModelMapper;
    }

    @GetMapping("/{namespace}/{repoName}")
    public String overview(@PathVariable String namespace,
                           @PathVariable String repoName,
                           @RequestParam(required = false) String branch,
                           Model model) {
        RepositoryOverviewClientDto dto = repositoryApiClient.getOverview(namespace, repoName, branch);
        model.addAttribute("repository", viewModelMapper.toViewModel(dto));
        return "repository/overview";
    }
}
```

```java
// 금지: 화면 controller가 ApiResponse를 view model처럼 사용
@Controller
public class BadRepositoryPageController {

    @GetMapping("/{namespace}/{repoName}")
    public String overview(Model model) {
        ApiResponse<RepositoryOverviewClientDto> response = loadRepository();
        model.addAttribute("repository", response);
        return "repository/overview";
    }
}
```

#### `runner/build.gradle`
```gradle
dependencies {
    implementation project(':core-common')
    implementation project(':core-grpc')

    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-json'
    implementation 'org.springframework:spring-web'
}
```

### Import 변경 예시

#### Controller
```java
// AS-IS
import io.jgitkins.server.presentation.common.ApiResponse;

// TO-BE
import io.jgitkins.core.web.api.response.ApiResponse;
```

#### Common exception
```java
// AS-IS
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.common.problem.ProblemSpec;

// TO-BE
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.core.common.problem.ProblemSpec;
```

#### Security handler
```java
// AS-IS
import io.jgitkins.server.presentation.common.ApiResponse;

// TO-BE
import io.jgitkins.core.web.api.response.ApiResponse;
import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
```

### 실행 순서
1. `settings.gradle`에 `core-*` 모듈을 추가한다.
2. 각 `core-* / build.gradle`을 만든다.
   - Boot plugin 없이 `java-library`만 적용한다.
   - Spring Boot BOM을 dependencyManagement에 명시한다.
3. `core-common`을 먼저 이동하고 `server` 컴파일을 맞춘다.
4. `core-web`을 이동하고 모든 server controller/advice/security handler import를 맞춘다.
   - server REST controller와 server internal REST adapter는 `core-web.api.response.ApiResponse`를 사용한다.
   - web MVC 화면 controller는 `ApiResponse`를 사용하지 않는다.
   - web server API client만 `ApiResponse`를 역직렬화 envelope로 사용한다.
5. `core-security`는 handler 공통화까지만 적용한다.
6. `core-persistence`는 설정만 이동하고 mapper/entity는 그대로 둔다.
7. `core-grpc`는 dependency/version 기준만 만든다. proto/stub 이동은 보류한다.
8. `server`, `web`, `runner` dependency 중 core로 이동된 직접 dependency를 제거한다.

### 검증
```bash
./gradlew :core-common:test
./gradlew :core-web:test
./gradlew :core-security:test
./gradlew :core-persistence:test
./gradlew :core-grpc:test

./gradlew :server:compileJava
./gradlew :web:compileJava
./gradlew :runner:compileJava

./gradlew :server:test
./gradlew :web:test
./gradlew :runner:test
```

### 아키텍처 테스트 후보
```java
@Test
void coreModules_doNotImportApplicationModules() throws IOException {
    assertNoImports(Path.of("../core-common/src/main/java"), "import io.jgitkins.server.");
    assertNoImports(Path.of("../core-web/src/main/java"), "import io.jgitkins.server.");
    assertNoImports(Path.of("../core-security/src/main/java"), "import io.jgitkins.server.");
    assertNoImports(Path.of("../core-persistence/src/main/java"), "import io.jgitkins.server.");
    assertNoImports(Path.of("../core-grpc/src/main/java"), "import io.jgitkins.server.");
}

@Test
void corePersistence_doesNotOwnBusinessPersistenceModels() throws IOException {
    assertNoPath(Path.of("../core-persistence/src/main/java"), "model");
    assertNoPath(Path.of("../core-persistence/src/main/java"), "entity");
    assertNoPath(Path.of("../core-persistence/src/main/java"), "adapter");
}

@Test
void webMvcControllers_doNotUseApiResponseAsViewModel() throws IOException {
    assertNoImports(Path.of("../web/src/main/java/io/jgitkins/web/presentation"),
            "import io.jgitkins.core.web.api.response.ApiResponse;");
}
```

### 완료 기준
- `core-*` 모듈이 모두 Gradle project로 추가되어 있다.
- `core-*`에는 Boot plugin이 없다.
- `server`, `web`, `runner`는 필요한 `core-*`만 의존한다.
- `core-*`에서 `server`, `web`, `runner`, `context-*` 방향 import가 없다.
- 전체 `compileJava`와 테스트가 통과한다.

### 최종 검수 결과

#### 구현 진행 가능 여부
- 진행 가능하다.
- 단, 1차 구현은 `core-common`과 `core-web`까지만 먼저 진행하는 것을 권장한다.
- `core-security`, `core-persistence`, `core-grpc`는 모듈 shell과 dependency 기준을 먼저 만들고, 실제 source 이동은 각 모듈별 컴파일 기준선이 잡힌 뒤 진행한다.

#### 반드시 지킬 수정 기준
- 신규 `core-*` 모듈은 `java-library`만 사용한다.
- 신규 `core-*` 모듈은 Spring Boot plugin을 적용하지 않는다.
- Spring dependency version은 각 core module의 `dependencyManagement`에서 Spring Boot BOM으로 고정한다.
- `ApiResponse`는 web MVC 화면 controller에서 사용하지 않는다.
- `core-security`는 app별 `ApplicationProblemSpec`, `PresentationProblemSpec`를 직접 import하지 않는다.
- `core-security`는 `ApiSecurityProblem` 같은 중립 contract를 받고, app module이 문제 코드를 주입한다.
- `core-persistence`에는 mapper/entity/adapter를 넣지 않는다.
- `core-grpc`는 이번 단계에서 proto/stub 이동을 강제하지 않는다.

#### 구현 순서 최종안
```text
Step 1. settings.gradle에 core module 추가
Step 2. core-common/core-web/core-security/core-persistence/core-grpc build.gradle 생성
Step 3. core-common에 common error/problem/exception 이동
Step 4. server compileJava 통과
Step 5. core-web에 ApiResponse/ApiError/LocationUriBuilder 이동
Step 6. server controller/advice/security handler import 정리
Step 7. web server-api-client 사용처만 ApiResponse import 허용
Step 8. architecture test 추가
Step 9. core-security는 SecurityErrorResponseWriter와 neutral handler만 이동
Step 10. core-persistence/core-grpc는 dependency/config shell만 만들고 source 이동은 최소화
```

#### 구현 중 중단해야 하는 신호
- `core-*`에서 `io.jgitkins.server.*`, `io.jgitkins.web.*`, `io.jgitkins.runner.*` import가 필요해지는 경우
- `web`의 Thymeleaf controller에서 `ApiResponse` import가 필요해지는 경우
- `core-persistence`로 mapper/entity/model 이동이 필요해지는 경우
- `core-security`가 server application error spec을 직접 import해야 하는 경우
- `core-grpc` 이동 때문에 proto generated source 경로가 server/runner 양쪽에서 동시에 깨지는 경우

#### 검증 최종안
```bash
./gradlew :core-common:test
./gradlew :core-web:test
./gradlew :core-security:test
./gradlew :core-persistence:test
./gradlew :core-grpc:test

./gradlew :server:compileJava
./gradlew :web:compileJava
./gradlew :runner:compileJava

./gradlew :server:test
./gradlew :web:test
./gradlew :runner:test

git diff --check
```

#### 구현 Closeout
- `core-common`, `core-web`, `core-security`, `core-persistence`, `core-grpc` 모듈을 추가했다.
- `core-common`에는 순수 common contract인 `ErrorCode`, `ProblemSpec`, `JgitkinsException`만 이동했다.
- `CommitFileFactory`는 server application DTO와 infrastructure exception에 의존하므로 server에 유지했다.
- `core-web`에는 `ApiResponse`, `ApiError`, `LocationUriBuilder`를 `io.jgitkins.core.web.api` 하위로 이동했다.
- `web` MVC 화면 controller는 core `ApiResponse`를 사용하지 않는 상태를 유지했다.
- `core-security`에는 `SecurityErrorResponseWriter`만 추가하고, server의 access denied/unauthorized handler가 이를 사용하게 했다.
- `core-persistence`에는 `DataSourceConfig`, `MybatisConfig`를 이동하고 server application에서 명시 import했다.
- `core-grpc`는 dependency/version shell만 추가하고 proto/stub 이동은 보류했다.
- 검증은 `:core-common:test`, `:core-web:test`, `:core-security:test`, `:core-persistence:test`, `:core-grpc:test`, `:server:test`, `:web:test`, `:runner:test`를 통과했다.

### 후속 문서
- `.taskmaster/docs/refactor/task_2_33_injection_core_libraries_from_apps_plan.md`: app 모듈 rename과 core library 주입, bootstrap 정리의 후속 실행 계획을 다룬다.
