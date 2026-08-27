# Persistence Boundary

## TOC

- [Overview](#overview)
- [Ownership](#ownership)
- [Target package](#target-package)
- [Infrastructure responsibilities](#infrastructure-responsibilities)
- [Dependency rules](#dependency-rules)
- [Migration scope](#migration-scope)

## Overview

Persistence 기술과 bounded context의 업무 모델을 분리한다. JPA/MyBatis에 종속된 entity, mapper, repository 구현은 outbound persistence adapter가 소유하고, 애플리케이션 전체에 적용되는 datasource·transaction·migration 설정은 infrastructure가 소유한다.

현재 저장소는 이 원칙으로 점진적으로 이동 중이다. 예를 들어 `repository/infrastructure/persistence/model/RepositoryEntity`는 persistence 기술 모델이고, `repository/adapter/out/persistence/RepositoryPersistenceAdapter`가 model과 domain 사이의 mapping을 담당한다. 이 문서는 현재 위치를 즉시 변경했다는 의미가 아니라, 이후 이동과 검증의 기준을 정의한다.

## Ownership

| 자산 | 소유 위치 | 이유 |
| --- | --- | --- |
| 순수 domain entity/value object | `<context>/domain` | 외부 persistence 기술 없이 invariant를 보장해야 한다. |
| persistence entity/model | `<context>/adapter/out/persistence/model` | JPA/MyBatis 컬럼·매핑 구조는 outbound 기술 세부사항이다. |
| persistence mapper | `<context>/adapter/out/persistence/mapper` | persistence model과 저장소 호출을 context adapter가 조합한다. |
| persistence repository 구현 | `<context>/adapter/out/persistence/repository` | framework repository와 SQL 접근은 outbound adapter 책임이다. |
| persistence adapter | `<context>/adapter/out/persistence` | application/domain port를 기술 구현으로 연결한다. |
| datasource 설정 | `infrastructure` 또는 공통 기술 설정 | 애플리케이션 실행 환경의 연결 설정이며 특정 aggregate의 업무 규칙이 아니다. |
| transaction 설정 | `infrastructure` 또는 공통 기술 설정 | transaction manager와 경계 설정은 runtime composition 책임이다. |
| migration | `infrastructure` 또는 database migration 영역 | schema 변경 이력과 배포 순서는 운영 기술 책임이다. |
| generated persistence infrastructure | generated 전용 infrastructure 영역 | 생성 도구 산출물은 domain/application 계약이 아니다. |
| configuration/composition root | `infrastructure` 또는 각 context의 infrastructure/config | bean 조립과 framework wiring은 외부 기술 경계에서 수행한다. |

## Target package

장기 목표는 bounded context별로 다음 구조를 사용하는 것이다.

```text
<context>/adapter/out/persistence/
├── model/
├── mapper/
├── repository/
└── *PersistenceAdapter
```

`domain.repository`는 aggregate lifecycle port를 소유한다. `application.port.out`은 cross-context 단순 조회나 ACL/read contract처럼 application이 필요한 좁은 외부 계약을 소유한다. 두 port 모두 persistence entity/model, JPA repository, MyBatis mapper 타입을 노출하지 않는다.

## Infrastructure responsibilities

다음 책임은 `infrastructure`에 남긴다.

- configuration 및 composition root
- datasource 및 connection pool 설정
- transaction manager와 transaction 설정
- JPA/MyBatis global configuration
- Flyway/Liquibase 등 database migration
- generated persistence infrastructure
- framework, 운영, 배포 wiring

이 책임들은 특정 domain aggregate의 저장 구조가 아니라 애플리케이션 실행 환경을 구성한다. 따라서 bounded context의 persistence adapter와 같은 위치로 이동시키지 않는다.

## Dependency rules

허용되는 방향은 다음과 같다.

```text
inbound adapter -> application port/use case -> domain
                                         │
                                         └── outbound persistence port
                                                  ▲
                         adapter.out.persistence ─┘
```

금지한다.

- domain production source의 `jakarta.persistence`, JPA, MyBatis, Spring Data import
- application port가 persistence entity/model/JPA repository 타입을 반환하거나 인자로 받는 것
- controller가 persistence entity를 직접 응답하는 것
- `adapter.out.persistence` 밖으로 persistence model이 누출되는 것
- persistence adapter가 domain invariant를 우회해 기술 모델만 변경하는 것

persistence adapter는 model ↔ domain mapping을 담당한다. schema, table, API, wire, HTTP status/error contract는 이 경계 정리만으로 변경하지 않는다.

## Migration scope

별도 persistence boundary task에서 다음 순서로 점진적으로 정리한다.

1. bounded context별 entity/model, mapper, repository, adapter의 실제 caller와 Spring/MyBatis wiring을 inventory한다.
2. domain/application source에 남아 있는 persistence 기술 import와 model leak를 제거한다.
3. adapter ↔ domain mapping을 유지한 상태에서 기술 모델을 `<context>/adapter/out/persistence/model`로 이동한다.
4. mapper/repository 구현을 같은 outbound persistence 경계로 정리한다.
5. configuration, datasource, transaction, migration, generated asset은 infrastructure allowlist로 보존한다.
6. architecture test, compile, focused persistence tests, full regression으로 dependency direction과 동작 보존을 검증한다.

이 task는 단순 directory 이동만 수행하지 않는다. import direction, mapping ownership, Spring bean wiring, generated mapper 경계를 함께 검증한다. DB schema/table migration, API/wire contract, route/status/error behavior 변경은 제외한다.

관련 실행 계획은 `~/.gstack/plans/jgitkins-task-2.67-persistence-boundary-placement-20260824.md`에서 관리한다.
