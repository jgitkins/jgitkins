# Persistence Boundary

## TOC

- [Overview](#overview)
- [Ownership](#ownership)
- [Target package](#target-package)
- [Infrastructure responsibilities](#infrastructure-responsibilities)
- [Dependency rules](#dependency-rules)
- [Migration scope](#migration-scope)

## Overview

Persistence 기술과 bounded context의 업무 모델을 분리한다. JPA에 종속된 entity와 repository 구현은 outbound persistence adapter가 소유하고, 애플리케이션 전체에 적용되는 datasource·transaction·migration 설정은 공통 infrastructure가 소유한다.

이 문서가 처음 쓰였을 때는 목표 상태의 정의였다. 지금은 현재 상태의 서술이다. 그 사이에 두 가지가 끝났다.

첫째, MyBatis가 삭제되었다. 두 provider를 capability slice별 property로 골라 쓰던 selector 기계는 rollback을 위해 존재했고, rollback을 포기하기로 결정한 뒤에는 고를 것이 남지 않았다. 그래서 provider 중립을 위해 존재했던 구조 -- MBG entity를 담던 `model`, 생성된 mapper를 담던 `translator`, 둘을 domain으로 옮기던 MapStruct `support` -- 도 함께 사라졌다. 생성된 코드는 놓을 자리가 필요했지만 손으로 쓴 JPA entity와 adapter는 그렇지 않다.

둘째, bounded context별 `infrastructure` package가 없어졌다. 남아 있던 여섯 파일은 각자 유일한 소비처 옆으로 옮겨졌고, 새 package를 만들지 않았다. `common/infrastructure`는 남는다 -- bounded context가 아니고, framework 설정과 context 간 공통 error·exception 어휘를 담는다.

## Ownership

| 자산 | 소유 위치 | 이유 |
| --- | --- | --- |
| 순수 domain entity/value object | `<context>/domain` | 외부 persistence 기술 없이 invariant를 보장해야 한다. |
| JPA entity, Spring Data repository, persistence adapter | `<context>/adapter/out/persistence/jpa` | 컬럼·매핑 구조와 framework repository는 outbound 기술 세부사항이다. |
| cross-context ACL adapter | `<context>/adapter/out/acl` | 다른 context의 port를 자기 계약으로 번역한다. |
| datasource 설정 | `common/infrastructure/config` | 애플리케이션 실행 환경의 연결 설정이며 특정 aggregate의 업무 규칙이 아니다. |
| transaction 설정 | `common/infrastructure/config` | transaction manager와 경계 설정은 runtime composition 책임이다. |
| migration | `app-server/data` | schema 변경 이력과 배포 순서는 운영 기술 책임이다. |
| `@ConfigurationProperties` | 그 값을 읽는 adapter 옆 | 설정 클래스 전용 package를 만들면 소비처가 하나인 타입이 계층 하나를 더 건너게 된다. |
| framework wiring, filter chain, security 설정 | `common/infrastructure/config` | 특정 context의 것이 아니다. |

generated persistence infrastructure 항목은 삭제했다. MyBatis Generator 설정과 그 도구(`app-server/data/mbg`)도 같이 없앴다 -- 남겨두면 삭제한 코드를 다시 만들어내는 도구가 저장소에 남는다.

## Target package

장기 목표는 bounded context별로 다음 구조를 사용하는 것이다.

```text
<context>/adapter/out/persistence/
└── jpa/
    ├── *JpaEntity
    ├── *JpaRepository
    └── *JpaPersistenceAdapter
```

`jpa` 한 겹이다. `model`/`mapper`/`repository`로 나뉘어 있던 것은 provider가 둘일 때 각 provider의 산출물을 갈라놓기 위한 것이었다.

`domain.repository`는 aggregate lifecycle port를 소유한다. `application.port.out`은 cross-context 단순 조회나 ACL/read contract처럼 application이 필요한 좁은 외부 계약을 소유한다. 두 port 모두 JPA entity나 Spring Data repository 타입을 노출하지 않는다.

## Infrastructure responsibilities

다음 책임은 `common/infrastructure`에 남긴다.

- configuration 및 composition root
- datasource 및 connection pool 설정
- transaction manager와 transaction 설정
- JPA global configuration (`JpaPersistenceConfiguration`)
- database migration
- framework, security, 운영, 배포 wiring

이 책임들은 특정 domain aggregate의 저장 구조가 아니라 애플리케이션 실행 환경을 구성한다.

**bounded context는 `infrastructure` package를 갖지 않는다.** 규칙이 아니라 관찰이었다가 규칙이 되었다: 규칙 없는 directory는 다른 곳에 명백히 속하지 않는 것을 모아들이고, persistence model이 처음 거기 쌓인 경로가 정확히 그것이었다. `InfrastructureAllowlistArchitectureTest`가 이를 지키며, 그 allowlist는 이제 비어 있다 -- 무엇을 담아야 하는지에 대한 논거는 map 항목이 아니라 review에서 대야 한다.

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

- domain production source의 `jakarta.persistence`, JPA, Spring Data import (`org.mybatis`/`org.apache.ibatis`도 계속 금지한다 -- 우연히 돌아오는 것을 막는 ratchet이다)
- application port가 persistence entity/model/JPA repository 타입을 반환하거나 인자로 받는 것
- controller가 persistence entity를 직접 응답하는 것
- `adapter.out.persistence` 밖으로 persistence model이 누출되는 것
- persistence adapter가 domain invariant를 우회해 기술 모델만 변경하는 것

persistence adapter는 model ↔ domain mapping을 담당한다. schema, table, API, wire, HTTP status/error contract는 이 경계 정리만으로 변경하지 않는다.

## Migration scope

완료되었다. 순서는 다음과 같았다.

1. bounded context별 entity/model, mapper, repository, adapter의 실제 caller와 Spring/MyBatis wiring을 inventory했다.
2. domain/application source에 남아 있는 persistence 기술 import와 model leak를 제거했다.
3. 기술 모델을 `<context>/adapter/out/persistence`로 옮겼다 (task 2.67, 53개 파일).
4. bounded context별로 JPA adapter를 신설하고 capability slice별 selector로 provider를 골랐다 (2.70-2.77).
5. 일곱 slice 전부를 JPA로 cutover했다.
6. selector를 제거하고, MyBatis를 삭제하고, bounded context의 `infrastructure` package를 없앴다.

schema는 한 번도 바뀌지 않았다. 이것이 이 작업 전체의 제약이었다 -- provider를 바꾸는 일이 schema 변경으로 번지면 되돌릴 수 없는 것이 두 개가 된다. 테이블은 `app-server/data/ddl.sql`이 소유하며, Hibernate의 `ddl-auto`는 `none`이다.

app-runner는 별도로 같은 이동을 했다. 자기 host의 local H2 파일에 runner 설정을 보관하며, `ddl-auto`가 `none`인 이유가 여기서는 더 직접적이다: 그 파일에는 서버가 발급한 token이 들어 있고, Hibernate가 테이블을 다시 만들면 이미 활성화된 runner가 조용히 연결이 끊긴다.

관련 실행 계획은 `~/.gstack/plans/jgitkins-task-2.67-persistence-boundary-placement-20260824.md`에서 관리한다.
