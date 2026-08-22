## Collaboration Context

### TOC

- [문제 정의](#문제-정의)
- [책임 범위](#책임-범위)
- [핵심 개념과 유비쿼터스 언어](#핵심-개념과-유비쿼터스-언어)
- [Organization](#organization)
- [Organization Member](#organization-member)
- [Aggregate / Entity / Value Object 경계](#aggregate--entity--value-object-경계)
- [Aggregate Root: Organization](#aggregate-root-organization)
- [Relation Model Candidate: Organization Member](#relation-model-candidate-organization-member)
- [주요 Value Objects](#주요-value-objects)
- [불변식](#불변식)
- [주요 시나리오](#주요-시나리오)
- [1. Organization 생성](#1-organization-생성)
- [2. Organization 조회와 접근 가능 여부](#2-organization-조회와-접근-가능-여부)
- [3. Organization Member 추가와 제거](#3-organization-member-추가와-제거)
- [외부 시스템과의 경계](#외부-시스템과의-경계)
- [다른 Context와의 연결](#다른-context와의-연결)
- [미확정 쟁점](#미확정-쟁점)

### 문제 정의

`Collaboration Context`는 조직 생성과 조직 멤버십을 다룬다. 현재 중심 모델은 `Organize`와 `OrganizeMember`다.

이 문서의 목적은 다음 질문에 답하는 것이다.

- organization은 어떤 상태를 직접 소유하는가
- organization member는 organization 내부 entity인가 별도 관계 모델인가
- organization 접근 가능 여부는 어느 경계에서 판단되는가

### 책임 범위

이 context의 책임은 다음과 같다.

- organization 생성, 조회, 삭제
- organization owner 관리
- organization member 추가, 제거, 조회
- organization 접근 가능 여부 판단의 기초 정보 제공

직접 소유하지 않는 책임은 다음과 같다.

- repository 생성과 repository 멤버십 관리
- Git repository 권한의 최종 계산
- 사용자 인증과 PAT 관리

### 핵심 개념과 유비쿼터스 언어

#### Organization

여러 사용자가 함께 repository를 소유하고 관리하는 협업 단위다. 코드에서는 `Organize`를 사용한다.

#### Organization Member

특정 organization에 속한 사용자와 역할이다.

### Aggregate / Entity / Value Object 경계

#### Aggregate Root: Organization

`Organize`는 이 context의 root다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/aggregate/Organize.java`
- 유스케이스 근거: `OrganizeCreationUseCase`, `OrganizeLoadUseCase`, `OrganizeDeletionUseCase`
- 서비스 근거: `OrganizeService`

`Organization`이 직접 소유하거나 결정하는 값은 다음과 같다.

- `OrganizeId`
- `OrganizeName`
- `description`
- `ownerId`
- `createdAt`
- `updatedAt`

#### Relation Model Candidate: Organization Member

`OrganizeMember`는 현재 organization 내부 entity보다 관계 모델에 가깝다.

- 코드 근거: `server/src/main/java/io/jgitkins/server/domain/model/OrganizeMember.java`
- 서비스 근거: `OrganizeMemberService`
- 식별: `organizeId + userId`

현재 구현은 `OrganizeMemberService`가 멤버십 row를 직접 관리한다.

#### 주요 Value Objects

이 context의 주요 Value Object는 다음과 같다.

- `OrganizeId`
- `OrganizeName`
- `UserId`
- `OrganizeMemberRole`

### 불변식

현재 기준 불변식은 다음과 같다.

1. organization 생성 시 name은 중복될 수 없다.
2. organization 생성 시 creator의 `OrganizeMember(OWNER)` row가 organization row와 같은 transaction에서 생성된다.
3. `OrganizeMember` 생성 시 `organizeId`, `userId`, `role`은 필수다.
4. organization member의 기본 role은 `MEMBER`다.
5. 같은 `organizeId + userId` 조합은 중복 생성할 수 없다.
6. membership mutation은 trusted requester identity를 사용한다.
7. 다른 member의 add/remove는 `OWNER`만 수행할 수 있다.
8. `MEMBER`와 `MAINTAINER`는 자신을 제거할 수 있다.
9. OWNER self-remove와 다른 OWNER 제거는 active OWNER가 하나 이상 남을 때만 허용된다.
10. active membership은 `ORGANIZE_MEMBER` row의 존재로 정의한다. invitation/pending 상태는 없다.

### 주요 시나리오

#### 1. Organization 생성

현재 흐름은 다음과 같다.

1. `OrganizeService`가 생성 요청을 받는다.
2. `OrganizeName`과 `ownerId`를 만든다.
3. `OrganizeValidator.validateCreation(...)`가 name 중복을 검증한다.
4. `OrganizeRepository.save(...)`로 organization을 저장하고 generated `OrganizeId`를 얻는다.
5. 생성자의 `OrganizeMember(OWNER)`를 같은 transaction에서 저장한다.
6. `OrganizeCreatedEvent`를 after-commit delivery로 등록한다.
7. 결과 DTO를 반환한다.

#### 2. Organization 조회와 접근 가능 여부

현재 흐름은 다음과 같다.

1. 전체 조회는 `organizePort.findAll()`로 수행한다.
2. 접근 가능 organization 조회는 현재 사용자 id를 구한다.
3. owner이거나 member이면 접근 가능으로 본다.

#### 3. Organization Member 추가와 제거

현재 흐름은 다음과 같다.

1. inbound adapter가 `UserIdentityPort`로 requester를 resolve한다.
2. organization이 없거나 legacy owner-only 상태면 mutation을 거부한다.
3. add는 OWNER requester만 MEMBER/MAINTAINER/OWNER를 즉시 추가한다.
4. remove는 organization row를 `SELECT ... FOR UPDATE`로 잠근다.
5. requester role과 target을 검증하고, OWNER count가 0이 되지 않을 때만 삭제한다.
6. missing target은 not-found error로 반환하며 성공으로 처리하지 않는다.

### 외부 시스템과의 경계

외부 경계는 다음과 같다.

- `OrganizeRepository`
  - organization aggregate 저장, 수정, 조회, 삭제
- `OrganizeQueryPort`
  - 다른 context가 필요한 organization 조회
- `OrganizeMemberPersistencePort`
  - organization member 저장, 조회, 삭제
- `CurrentUserPort`
  - 현재 사용자 식별

원칙:

- organization 접근 가능 여부 판단은 owner 또는 member 여부에 기반한다.
- organization member는 현재 aggregate 내부 collection보다 persistence 관계 모델에 가깝다.

### 다른 Context와의 연결

- `Identity & Access Context`
  - owner와 member는 user와 연결된다.
- `Repository Context`
  - repository owner가 organization일 수 있다.
- `Shared / Cross-Cutting Topics`
  - namespace, 권한 상속 규칙과 연결된다.

### 미확정 쟁점

1. `Organize`와 `Organization` 명명을 통일할지
   - 현재 코드는 `Organize`, 문서는 `Organization`을 쓴다.
2. `Organization Member`를 aggregate 내부 entity로 올릴지
   - 현재 구현은 관계 모델에 가깝다.
