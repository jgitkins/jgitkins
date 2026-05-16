
## Overview


### Overall Flow Diagram
```mermaid
classDiagram
    direction LR
    class User {
        +Long id
        +String username
        +String email
        +String displayName
        +String avatarUrl
        +String status
        +LocalDateTime lastLoginAt
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
    class UserIdentity {
        +Long id
        +String providerName
        +String providerSub
        +String email
        +boolean emailVerified
        +String name
        +String avatarUrl
    }
    class UserCredential {
        +Long id
        +String provider
        +String passwordHash
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
    class Organize {
        +OrganizeId id
        +OrganizeName name
        +String description
        +UserId ownerId
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
    class Repository {
        +RepositoryId id
        +OwnerType ownerType
        +OrganizeId organizeId
        +RepositoryName name
        +RepositoryPath path
        +BranchName defaultBranch
        +RepositoryVisibility visibility
        +UserId ownerId
        +String credentialId
        +String clonePath
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
    class OwnerType
    class Branch {
        +RepositoryId repositoryId
        +BranchName name
        +boolean locked
        +boolean ciEnabled
        +boolean defaultBranch
    }
    class Runner {
        +Long id
        +String token
        +RunnerStatus status
        +RunnerScopeType scopeType
        +Long scopeTargetId
        +String description
        +String ipAddress
        +LocalDateTime lastHeartbeatAt
        +LocalDateTime createdAt
    }
    class Job {
        +JobId id
        +RepositoryId repositoryId
        +CommitHash commitHash
        +BranchName branchName
        +UserId triggeredBy
        +LocalDateTime createdAt
    }
    class JobHistory {
        +JobStatus status
        +LocalDateTime occurredAt
        +String notes
    }

    User "1" --> "many" UserIdentity
    User "1" --> "many" UserCredential
    User "1" --> "many" Organize
    Organize "1" --> "many" Repository
    User "1" --> "many" Repository
    Repository "1" --> "many" RepositoryMember
    Repository "1" --> "many" Branch
    Repository "1" --> "many" Job
    Job "1" --> "many" JobHistory
    Runner "1" --> "many" JobHistory
    Organize "1" --> "many" OrganizeMember
    User "1" --> "many" OrganizeMember
    User "1" --> "many" RepositoryMember
    class OrganizeMember {
        +OrganizeId organizeId
        +UserId userId
        +OrganizeMemberRole role
        +LocalDateTime joinedAt
    }
    class RepositoryMember {
        +RepositoryId repositoryId
        +UserId userId
        +RepositoryMemberRole role
        +LocalDateTime addedAt
    }
```

### Flow Summary

- A `User` signs in with OAuth and has `UserIdentity` and `UserCredential` for provider and PAT data.
- A `User` owns one or more `Organize` entities as the top-level account boundary.
- A `Repository` is owned by either `Organize` or `User` based on `ownerType`.
- A `Repository` has path, visibility, and default branch. It links to `Branch` and `Job`.
- A `Runner` executes jobs and is tracked by heartbeat.
- Memberships (organize/repository) are separate aggregates used for access control.

## User
```mermaid
classDiagram
    class User {
        +Long id
        +String username
        +String email
        +String displayName
        +String avatarUrl
        +String status
        +LocalDateTime lastLoginAt
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +create(...)
    }
    class UserIdentity {
        +Long id
        +String providerName
        +String providerSub
        +String email
        +boolean emailVerified
        +String name
        +String avatarUrl
    }
    class UserCredential {
        +Long id
        +String provider
        +String passwordHash
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
    User "1" --> "many" UserIdentity
    User "1" --> "many" UserCredential
```

## Membership
```mermaid
classDiagram
    class OrganizeMember {
        +OrganizeId organizeId
        +UserId userId
        +OrganizeMemberRole role
        +LocalDateTime joinedAt
        +create(...)
    }
    class RepositoryMember {
        +RepositoryId repositoryId
        +UserId userId
        +RepositoryMemberRole role
        +LocalDateTime addedAt
        +create(...)
    }
    class OrganizeId
    class RepositoryId
    class UserId
    class OrganizeMemberRole
    class RepositoryMemberRole
```

## Organize
```mermaid
classDiagram
    class Organize {
        +OrganizeId id
        +OrganizeName name
        +String description
        +UserId ownerId
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +create(...)
    }
    class OrganizeId
    class OrganizeName
    class UserId
```

## Repository
```mermaid
classDiagram
    class Repository {
        +RepositoryId id
        +OwnerType ownerType
        +OrganizeId organizeId
        +RepositoryName name
        +RepositoryPath path
        +BranchName defaultBranch
        +RepositoryVisibility visibility
        +UserId ownerId
        +String credentialId
        +String clonePath
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +create(...)
        +markInit(...)
    }
    class RepositoryId
    class OwnerType
    class OrganizeId
    class RepositoryName
    class RepositoryPath
    class BranchName
    class RepositoryVisibility
    class UserId
```

## Branch
```mermaid
classDiagram
    class Branch {
        +RepositoryId repositoryId
        +BranchName name
        +boolean locked
        +boolean ciEnabled
        +boolean defaultBranch
    }
    class RepositoryId
    class BranchName
```

## Runner
```mermaid
classDiagram
    class Runner {
        +Long id
        +String token
        +RunnerStatus status
        +RunnerScopeType scopeType
        +Long scopeTargetId
        +String description
        +String ipAddress
        +LocalDateTime lastHeartbeatAt
        +LocalDateTime createdAt
        +create(...)
        +activate(...)
    }
    class RunnerStatus
    class RunnerScopeType
```

## Job
```mermaid
classDiagram
    class Job {
        +JobId id
        +RepositoryId repositoryId
        +CommitHash commitHash
        +BranchName branchName
        +UserId triggeredBy
        +LocalDateTime createdAt
        +List~JobHistory~ history
        +create(...)
        +publish(...)
        +completeSuccess(...)
        +completeFailure(...)
    }
    class JobHistory {
        +JobStatus status
        +LocalDateTime occurredAt
        +String notes
    }
    Job "1" --> "many" JobHistory
    class JobId
    class RepositoryId
    class CommitHash
    class BranchName
    class UserId
    class JobStatus
```
