# JGitkins Server ERD

## Logical Design

```mermaid
erDiagram
    %% 1. Users (Global User)
    USERS {
        id bigint PK "Auto Increment"
        username varchar "NN, Unique ID"
        email varchar "NN"
        password_hash varchar "NN"
        created_at timestamp "NN"
        updated_at timestamp "NN"
    }

    %% 2. Organization (Group)
    ORGANIZE {
        id bigint PK "Auto Increment"
        name varchar "NN, Group Name"
        path varchar "NN, URL Path (slug)"
        description varchar
        owner_id bigint FK "NN, Creator/Owner"
        created_at timestamp "NN"
        updated_at timestamp "NN"
    }

    %% 3. Organization Member (User -> Organization)
    ORGANIZE_MEMBER {
        id bigint PK
        organize_id bigint FK "NN"
        user_id bigint FK "NN"
        role varchar "NN, OWNER/MAINTAINER/DEVELOPER/GUEST"
        joined_at timestamp "NN"
    }

    %% 4. Repository (Belongs to Organization)
    REPOSITORY {
        id bigint PK "Auto Increment"
        organize_id bigint FK "NN, Parent Organization"
        name varchar "NN, Repository Name"
        path varchar "NN, URL Path"
        description varchar
        default_branch varchar "NN"
        visibility varchar "NN, PUBLIC/PRIVATE/INTERNAL"
        created_at timestamp "NN"
        updated_at timestamp "NN"
    }

    %% 5. Repository Member (User -> Repository)
    REPOSITORY_MEMBER {
        id bigint PK
        repository_id bigint FK "NN"
        user_id bigint FK "NN"
        role varchar "NN, MAINTAINER/DEVELOPER/REPORTER"
        added_at timestamp "NN"
    }

    %% 6. Branch
    BRANCH {
        id bigint PK
        repository_id bigint FK "NN"
        name varchar "NN"
        is_locked boolean "NN"
        locked_by bigint FK "User ID (Audit)"
        locked_at timestamp
        created_at timestamp "NN"
        updated_at timestamp "NN"
    }

    %% 7. Runner (Physical Agent)
    RUNNER {
        id bigint PK
        token varchar "NN"
        description varchar
        status varchar "NN, ONLINE/OFFLINE/PAUSED"
        ip_address varchar
        last_heartbeat_at timestamp
        created_at timestamp "NN"
    }

    %% 8. Runner Assignment (Mapping Table)
    %% Assigns a Runner to a Scope (Global, Organization, or Repository)
    RUNNER_ASSIGNMENT {
        id bigint PK
        runner_id bigint FK "NN"
        target_type varchar "NN, GLOBAL/ORGANIZE/REPOSITORY"
        target_id bigint "Nullable (Null if GLOBAL)"
        assigned_at timestamp "NN"
    }

    %% 9. Job (Pipeline Instance / Request)
    %% Immutable data about the build request
    JOB {
        id bigint PK
        repository_id bigint FK "NN"
        commit_hash varchar "NN"
        branch_name varchar "NN"
        triggered_by bigint FK "User ID (Audit)"
        created_at timestamp "NN"
    }

    %% 10. Job History (Execution Attempt / Mapping)
    %% Dynamic execution data. One Job can have multiple histories (retries).
    %% This also acts as the mapping table between Job and Runner.
    JOB_HISTORY {
        id bigint PK
        job_id bigint FK "NN"
        runner_id bigint FK "Nullable (Pending Assignment)"
        status varchar "NN, PENDING/QUEUED/RUNNING/SUCCESS/FAILED/CANCELED"
        log_path varchar
        started_at timestamp
        finished_at timestamp
        created_at timestamp "NN"
    }

    %% Relationships
    USERS ||--o{ ORGANIZE_MEMBER : "joins"
    USERS ||--o{ REPOSITORY_MEMBER : "assigned to"
    USERS ||--o{ BRANCH : "locks"
    USERS ||--o{ JOB : "triggers"

    ORGANIZE ||--o{ ORGANIZE_MEMBER : "has"
    ORGANIZE ||--o{ REPOSITORY : "owns"

    REPOSITORY ||--o{ REPOSITORY_MEMBER : "has"
    REPOSITORY ||--o{ BRANCH : "contains"
    REPOSITORY ||--o{ JOB : "runs"

    RUNNER ||--o{ RUNNER_ASSIGNMENT : "assigned via"
    RUNNER ||--o{ JOB_HISTORY : "executes"

    JOB ||--o{ JOB_HISTORY : "has attempts"
```

## Constraints & Business Logic
1.  **Runner Assignment**: 
    *   Runners are not directly linked to a single Repo/Org in the `RUNNER` table.
    *   Instead, `RUNNER_ASSIGNMENT` links a `runner_id` to a `target_id` (which can be an Organization ID or Repository ID).
    *   When dispatching a job, the system checks if there is a runner assigned to the Repository OR the parent Organization.
2.  **Audit**: `BRANCH.locked_by` and `JOB.triggered_by` track user actions.
