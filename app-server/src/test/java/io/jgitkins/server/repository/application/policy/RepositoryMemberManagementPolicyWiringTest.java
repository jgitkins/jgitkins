package io.jgitkins.server.repository.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.repository.application.service.RepositoryMemberService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * {@code RepositoryMemberService} must hold the repository-owned member policy.
 *
 * <p>The policy is the authorization task 2.64 added: before it, any authenticated caller who knew a
 * repository id could add themselves as a member, and membership is what the read and commit paths check.
 * A service constructed without it compiles the moment someone adds another constructor overload, and
 * every existing member test would keep passing — the tests that would catch it are the denial tests,
 * which a future refactor could delete alongside the dependency.
 *
 * <p>Asserted by field type rather than by behaviour so the check survives that: it fails on the shape of
 * the service, not on a code path a test can be removed from.
 */
class RepositoryMemberManagementPolicyWiringTest {

    @Test
    void repositoryMemberServiceReceivesRepositoryOwnedPolicy() {
        Field[] fields = RepositoryMemberService.class.getDeclaredFields();

        assertThat(fields)
                .as("RepositoryMemberService must declare a RepositoryMemberManagementPolicy field. "
                        + "Without it, member mutation has no authorization at all.")
                .anyMatch(field -> field.getType().equals(RepositoryMemberManagementPolicy.class));

        assertThat(RepositoryMemberManagementPolicy.class.getPackageName())
                .as("the policy belongs to the repository context that owns the decision; a shared or "
                        + "identity-owned policy would put one context's authorization rule under "
                        + "another's ownership")
                .isEqualTo("io.jgitkins.server.repository.application.policy");
    }

    @Test
    void thePolicyReadsThroughTheQueryPortRatherThanTheAggregateRepository() throws Exception {
        // A read to make a decision, not an aggregate load to mutate. The port it takes says which, and
        // taking RepositoryRepository instead would let a policy hold a mutable aggregate it has no
        // business writing.
        var constructor = RepositoryMemberManagementPolicy.class.getDeclaredConstructors()[0];
        assertThat(constructor.getParameterTypes())
                .containsExactly(
                        io.jgitkins.server.repository.application.port.out.RepositoryQueryPort.class);
    }
}
