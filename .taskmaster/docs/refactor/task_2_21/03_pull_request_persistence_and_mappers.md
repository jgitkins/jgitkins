# 03. Pull Request Persistence and Mappers

## 목적

`PullRequest` aggregate의 persisted state와 mapper/persistence adapter의 책임을 정리한다.

이 문서는 `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/**` 아래로 저장/조회 경계를 고정하는 계획이다.

## 핵심 결론

- MBG entity는 persisted state만 담는다.
- `MergeabilityAssessment`는 entity에 저장하지 않는다.
- `TargetDrift`는 optional persisted snapshot으로 유지할 수 있지만, 상태 진실은 아니다.
- save/load adapter는 aggregate의 의미를 바꾸지 않고 identity와 snapshot만 왕복시킨다.

## 대상 파일

- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/mapper/PullRequestDomainMapper.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/adapter/persistence/PullRequestPersistenceAdapter.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/persistence/model/PullRequestEntity.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/persistence/model/PullRequestEntityCondition.java`
- `app-server/src/main/java/io/jgitkins/server/change/review/domain/repository/PullRequestRepository.java`

## TO-BE 패키지

```text
app-server/src/main/java/io/jgitkins/server/change/review/infrastructure/
  mapper/
    PullRequestDomainMapper.java
  adapter/persistence/
    PullRequestPersistenceAdapter.java
  persistence/model/
    PullRequestEntity.java
    PullRequestEntityCondition.java
```

## mapper 정책

현재 mapper는 rehydrate/save를 모두 담당한다.

```java
public PullRequest toDomain(PullRequestEntity entity) {
    return PullRequest.rehydrate(
            PullRequestId.of(entity.getId()),
            RepositoryId.of(entity.getRepositoryId()),
            BranchHeadSnapshot.of(entity.getSourceBranch(), entity.getSourceHead()),
            BranchHeadSnapshot.of(entity.getTargetBranch(), entity.getTargetHead()),
            PullRequestStatus.valueOf(entity.getStatus()),
            null,
            toTargetDrift(entity),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
}
```

정리 방향:

- `lastAssessmentSnapshot`은 rehydrate 시에도 null을 유지한다.
- `TargetDrift`는 entity 컬럼이 있으면 optional snapshot으로 역직렬화한다.
- entity가 future snapshot을 저장하지 않도록 mapper가 단일 기준을 가진다.

### toEntity

```java
public PullRequestEntity toEntity(PullRequest pullRequest) {
    PullRequestEntity entity = new PullRequestEntity();
    if (pullRequest.getId() != null) {
        entity.setId(pullRequest.getId().value());
    }
    entity.setRepositoryId(pullRequest.getRepositoryId().getValue());
    entity.setSourceBranch(pullRequest.getSource().branchName().getValue());
    entity.setSourceHead(pullRequest.getSource().commitHash().getValue());
    entity.setTargetBranch(pullRequest.getTarget().branchName().getValue());
    entity.setTargetHead(pullRequest.getTarget().commitHash().getValue());
    entity.setStatus(pullRequest.getStatus().name());
    entity.setCreatedAt(pullRequest.getCreatedAt());
    entity.setUpdatedAt(pullRequest.getUpdatedAt());
    applyTargetDrift(entity, pullRequest.getTargetDrift());
    return entity;
}
```

이 단계에서 추가하지 않는 것:

- assessment snapshot persistence
- PR merge result persistence
- repository current head persistence

## adapter 정책

### save

```java
@Override
public PullRequest save(PullRequest pullRequest) {
    try {
        PullRequestEntity entity = domainMapper.toEntity(pullRequest);
        if (pullRequest.getId() == null) {
            LocalDateTime now = LocalDateTime.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            mapper.insertSelective(entity);
            return pullRequest.withIdentity(PullRequestId.of(entity.getId()), entity.getCreatedAt(), entity.getUpdatedAt());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateByPrimaryKeySelective(entity);
        return pullRequest.withIdentity(pullRequest.getId(), pullRequest.getCreatedAt(), entity.getUpdatedAt());
    } catch (Exception e) {
        throw new InfrastructureException(
                InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                "Database operation failed during save pull request",
                e);
    }
}
```

정리 포인트:

- insert/update 분기를 유지하되 `updatedAt` 정책을 일관되게 유지한다.
- `withIdentity(...)`는 신규 저장 후 identity 부여용으로만 사용한다.
- mergeability나 current Git state를 save 단계에서 재계산하지 않는다.

### findById

```java
@Override
public Optional<PullRequest> findById(PullRequestId id) {
    try {
        PullRequestEntityCondition condition = new PullRequestEntityCondition();
        condition.createCriteria().andIdEqualTo(id.value());
        List<PullRequestEntity> entities = mapper.selectByCondition(condition);
        return entities.stream().findFirst().map(domainMapper::toDomain);
    } catch (Exception e) {
        throw new InfrastructureException(
                InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                "Database operation failed during find pull request by id",
                e);
    }
}
```

정리 포인트:

- 조회는 aggregate snapshot 복원까지만 맡는다.
- current source/current target/mergeability는 application layer에서 다시 계산한다.

## 테스트 기준

- `PullRequestDomainMapperTest`
  - entity -> domain snapshot 복원
  - domain -> entity persisted field only mapping
  - optional target drift round-trip
- `PullRequestPersistenceAdapterTest`
  - save insert/update 분기
  - findById load path
  - persistence exception wrapping

## 완료 기준

- entity와 aggregate의 persisted field가 일치한다.
- mergeability는 persistence layer를 통과하지 않는다.
- `TargetDrift`는 optional snapshot semantics로만 남는다.
