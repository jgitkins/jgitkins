# Current-user caller disposition

Task 2.60 inventories current-user identity callers before migrating only the collaboration inbound slice. `CurrentUserPort` remains because identity policy and repository callers still depend on it. This document is deliberately not a repository-wide migration claim.

| Caller | Context | Inbound path | Current dependency | Desired explicit input | Disposition |
|---|---|---|---|---|---|
| `identity.access.application.port.out.CurrentUserPort.resolveCurrentUserId` | identity | application output port | declares current-user port | retained identity-owned port | retain — identity access owner; follow-up identity migration |
| `identity.access.adapter.out.security.CurrentUserSecurityAdapter` | identity | security adapter | `CurrentUserPort`, `SecurityContextHolder`, `Authentication` | typed inbound identity boundary after Task 2.61 | retain — identity/security owner; Task 2.61 boundary relocation |
| `identity.access.application.service.UserProfileService` | identity | application service | `CurrentUserPort` | explicit requester in identity profile use case | defer — identity access owner; separate identity task |
| `identity.access.adapter.out.policy.ActiveAccountPolicyAdapter` | identity | policy adapter | `CurrentUserPort` | explicit user ID to active-account policy | defer — identity access owner; preserve active-account policy |
| `collaboration.adapter.out.acl.InProcessUserIdentityAdapter` | collaboration compatibility adapter | application/adapter | `UserIdentityPort` backed by `CurrentUserPort` | explicit requester for migrated inbound callers | retain — compatibility adapter still used by retained wiring; remove only with identity-owned callers |
| `collaboration.application.port.out.UserIdentityPort.resolveCurrentActiveUserId` | collaboration compatibility port | application output port | current-active-user lookup | `Long requesterUserId` | retain — compatibility owner for non-migrated callers; no migrated service uses it |
| `collaboration.application.service.OrganizeService.createOrganize` | collaboration | REST inbound → application | formerly `UserIdentityPort` | `OrganizeCreationCommand.requesterUserId` | migrate — REST resolver owns subject-to-Long conversion |
| `collaboration.application.service.OrganizeService.getAccessibleOrganizes` | collaboration | REST/Web inbound → application | formerly `UserIdentityPort` | `Long requesterUserId` (nullable for public empty list) | migrate — explicit use-case input |
| `collaboration.adapter.in.rest.OrganizeController.createOrganize` | collaboration | REST | no principal previously; mapper only | `@AuthenticationPrincipal` subject → `RequesterUserIdResolver` → `Long` | migrate — inbound adapter conversion owner |
| `collaboration.adapter.in.rest.OrganizeController.getAccessibleOrganizes` | collaboration | REST `/api/organizes/me` | ambient current user previously | `Long requesterUserId`, null when absent | migrate — preserve HTTP 200 empty list |
| `collaboration.adapter.in.rest.OrganizeMemberController.addMember` | collaboration | REST | `UserIdentityPort` | explicit requester in `OrganizeMemberAddCommand` | migrate — resolver; empty maps to raw `UNAUTHENTICATED` 401 |
| `collaboration.adapter.in.rest.OrganizeMemberController.removeMember` | collaboration | REST | `UserIdentityPort` | explicit `requesterUserId` argument | migrate — resolver; empty maps to raw `UNAUTHENTICATED` 401 |
| `collaboration.adapter.in.web.WebOrganizeController.getAccessibleOrganizes` | collaboration | Web `/api/internal/organizes` | ambient current user previously | `Long requesterUserId`, null when absent | migrate — preserve HTTP 200 empty list |
| `collaboration.adapter.in.support.RequesterUserIdResolver` | collaboration | REST/Web inbound support | new shared bean | `Optional<Long> resolve(String subject)` | migrate — single subject conversion owner |
| `identity.access.adapter.in.security.JwtAuthenticationFilter` | authentication | security filter | `Authentication`, `SecurityContextHolder` | unchanged numeric principal username | migrated by Task 2.61; common remains composition root only |
| `common.infrastructure.adapter.PushEventRequestAdapter` | common infrastructure | webhook/event adapter | `SecurityContextHolder`, `Authentication` | separate event requester contract | defer — common infrastructure owner; unrelated to collaboration |
| `repository.application.port.out.RepositoryActorPort.resolveCurrentUserId` | repository | application output port | repository actor identity contract | explicit repository actor ID | retain — repository owner; separate ACL migration |
| `repository.application.service.RepositoryLoadService` | repository | application service | `RepositoryActorPort` | explicit repository actor ID | defer — repository owner; separate repository task |
| `repository.application.service.RepositoryOverviewService` | repository | application service | `CurrentUserPort` | explicit repository requester | defer — repository owner; separate ACL migration |
| `repository.application.validate.RepositoryValidator` | repository | application validator | `RepositoryActorPort` | explicit repository actor ID | defer — repository owner; preserve ACL semantics |
| `repository.application.validate.RepositoryAccessValidator` | repository | application validator | `CurrentUserPort` | explicit repository requester | defer — repository owner; separate ACL migration |
| `repository.adapter.out.acl.RepositoryActorAclAdapter` | repository | ACL adapter | delegates `CurrentUserPort` | explicit repository actor ID | retain — repository ACL owner; follow-up ACL migration |
| `identity.access.adapter.out.security.CurrentUserSecurityAdapterTest` | identity test | adapter unit test | `SecurityContextHolder`, `Authentication` | test retained security adapter contract | retain — follows retained identity adapter |
| `identity.access.application.service.UserProfileServiceTest` | identity test | service unit test | `CurrentUserPort` | test explicit identity requester in future task | defer — identity access owner |
| `identity.access.adapter.out.policy.ActiveAccountPolicyAdapterTest` | identity test | policy unit test | `CurrentUserPort` | test explicit policy user ID in future task | defer — identity access owner |
| `collaboration.adapter.out.acl.InProcessUserIdentityAdapterTest` | collaboration compatibility test | adapter unit test | `CurrentUserPort` | explicit collaboration inbound tests | retain — verifies retained compatibility adapter |
| `repository.adapter.out.acl.RepositoryActorAclAdapterTest` | repository test | ACL unit test | `CurrentUserPort` | explicit repository actor in future task | defer — repository ACL owner |
| `repository.application.*` identity tests | repository tests | service/validator unit tests | `RepositoryActorPort` or `CurrentUserPort` | explicit repository requester | defer — repository owner |
| `ArchitecturePackageConventionTest` and repository architecture tests | architecture tests | source/package gates | reference `CurrentUserPort` as retained port | collaboration-only forbidden-import assertions | retain/update only collaboration assertions; do not impose repository-wide zero-import gate |

## Collaboration result

The migrated collaboration application path receives only `Long requesterUserId` (or nullable list requester) and imports neither Spring Security nor `CurrentUserPort`/`UserIdentityPort`. REST and Web controllers share the Spring `RequesterUserIdResolver`; the JWT filter remains unchanged. Missing or malformed subjects are handled at the inbound boundary according to each route's existing contract.
