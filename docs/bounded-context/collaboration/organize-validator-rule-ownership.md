# OrganizeValidator rule ownership

## Verified rule matrix

The live `OrganizeValidator` remains an application-owned collaborator. Its three
rules have different external-state requirements, so no rule is moved into the
`Organize` aggregate or another domain type.

| Rule | Inputs | Exact production callers | Direct constructor callers / tests | Dependency | Exception / return contract | Owner | Why not domain-owned |
|---|---|---|---|---|---|---|---|
| `validateCreation(OrganizeName)` | name value | `OrganizeService.createOrganize(OrganizeCreationCommand)` | `OrganizeServiceTest` constructs `OrganizeValidator`; `OrganizeValidatorRuleContractTest` exercises duplicate and unique paths; `OrganizeValidatorTest` is the existing validator baseline | `OrganizeRepository.findByName(OrganizeName)` | throws `OrganizeAlreadyExistsException` when a duplicate exists; otherwise returns normally | application | Duplicate detection requires repository state and cannot be decided from the aggregate/value object alone. |
| `findByIdOrThrow(Long)` | organization ID | `OrganizeService.getOrganize(Long)` and `OrganizeService.deleteOrganize(Long)` | `OrganizeServiceTest` constructs `OrganizeValidator`; `OrganizeValidatorRuleContractTest` exercises present and absent paths; service/application tests cover its callers | `OrganizeRepository.findById(OrganizeId.of(id))` | returns the found `Organize`; throws `OrganizeNotFoundException` when absent | application | Existence lookup and not-found mapping require repository state and application exception policy. |
| `isAccessible(Organize, Long)` | aggregate, requester ID | `OrganizeService.getAccessibleOrganizes(Long)` | `OrganizeValidatorTest` directly constructs the validator for owner/member/non-member/null cases; `OrganizeValidatorRuleContractTest` repeats the direct owner/member/non-member/null contract; `OrganizeServiceTest` constructs the validator for service-level caller coverage | retained `Organize.ownerId` compatibility projection plus `OrganizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(organizeId, requesterUserId)` | returns `boolean`; null/invalid input is `false`; owner or member access is `true`; non-member access is `false` | application | Authorization depends on requester identity, membership lookup, and the retained owner projection. Those are external/application concerns, not a pure aggregate invariant. |

## Caller and wiring inventory

`OrganizeService` is the only production caller of all three validator methods and
retains its existing `OrganizeValidator` constructor dependency. The explicit
`CollaborationApplicationConfiguration` bean creates the validator from
`OrganizeRepository` and `OrganizeMembershipQueryPort`; no constructor, service,
Spring bean, or configuration change is needed. Direct test construction is limited
to `OrganizeValidatorTest`, `OrganizeValidatorRuleContractTest`, and
`OrganizeServiceTest`; bootstrap tests use a validator mock and configuration tests
verify the explicit bean.

The aggregate and value objects continue to own description normalization and name/
value validation. No duplicate validator or one-method policy class is introduced.

## Explicit no-move closure

Inventory evidence identifies **no current pure invariant** among the three rules.
Therefore all three remain application-owned and there is intentionally **no
production move** in this task: `OrganizeValidator`, `Organize`, `OrganizeService`,
and collaboration configuration wiring remain unchanged. The direct contract test
and the strengthened domain purity gate provide verification only.

Task 2.41 remains the owner-projection, membership-authorization, REST-error, and
transaction/event compatibility boundary. Task 2.59 remains the separate strict
owner migration; this document does not remove `Organize.ownerId`, change membership
lifecycle or owner transfer, or make membership `OWNER` the sole source of truth.
There are no API, schema, persistence, or wire changes.
