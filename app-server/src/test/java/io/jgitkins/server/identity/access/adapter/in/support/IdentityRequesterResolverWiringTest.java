package io.jgitkins.server.identity.access.adapter.in.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.identity.access.adapter.in.rest.SignupController;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The two resolvers are distinct types, and they disagree about malformed input.
 *
 * <p>This class used to claim that a {@code @Qualifier} was load-bearing because "two beans of type
 * {@code RequesterUserIdResolver} exist". That was wrong. Two beans share the simple NAME, but they are
 * unrelated classes with no common supertype: {@code identity.access...RequesterUserIdResolver} and
 * {@code collaboration...RequesterUserIdResolver}. Spring resolves by type, and a field declared as the
 * identity type has exactly one candidate, so the qualifier never disambiguated anything. It has been
 * removed from all seven injection points and every one now uses {@code @RequiredArgsConstructor}.
 *
 * <p>Proof, not assertion: this runner registers BOTH beans, which is the exact condition the qualifier
 * was supposed to handle, and the unqualified controller still wires correctly.
 *
 * <p>Note that {@link #identityResolverHoldsTheIdentityType()} cannot fail while the field keeps its
 * declared type -- it documents the guarantee rather than testing it. The assertion that carries real
 * weight is {@link #theIdentityAndCollaborationResolversDisagreeOnMalformedInput()}: it pins the
 * behavioural difference that makes picking the right one matter at all. If those two ever agreed, the
 * name collision would be harmless and this file could go.
 *
 * <p>The remaining smell is the collision itself -- two classes with one simple name across contexts,
 * the same shape as the duplicated {@code OrganizeAlreadyExistsException}. Renaming one is tracked
 * separately; it is not this test's job.
 */
class IdentityRequesterResolverWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(SignupUseCase.class, () -> Mockito.mock(SignupUseCase.class))
            .withBean("identityRequesterUserIdResolver", RequesterUserIdResolver.class,
                    RequesterUserIdResolver::new)
            .withBean("requesterUserIdResolver",
                    io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver.class,
                    io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver::new)
            .withBean(SignupController.class);

    @Test
    void identityResolverHoldsTheIdentityType() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("identityRequesterUserIdResolver");
            assertThat(context)
                    .as("the collaboration bean keeps the default name; the two are distinct and are "
                            + "not reused across contexts")
                    .hasBean("requesterUserIdResolver");

            SignupController controller = context.getBean(SignupController.class);
            Object injected = injectedResolver(controller);

            assertThat(injected)
                    .as("the controller must hold the identity-owned resolver. Wired to the "
                            + "collaboration bean instead, a malformed principal would resolve to empty "
                            + "and surface as the same 401 a client already sees, so no HTTP test could "
                            + "detect the swap.")
                    .isInstanceOf(RequesterUserIdResolver.class);
        });
    }

    @Test
    void theIdentityAndCollaborationResolversDisagreeOnMalformedInput() {
        // The fact that makes the wiring above worth stating at all. If these two ever agreed, the
        // name collision would carry no risk.
        var identity = new RequesterUserIdResolver();
        var collaboration = new io.jgitkins.server.collaboration.adapter.in.support
                .RequesterUserIdResolver();

        assertThat(collaboration.resolve("0"))
                .as("collaboration accepts zero as an id")
                .contains(0L);
        assertThat(collaboration.resolve("abc"))
                .as("and reports a non-numeric subject as simply absent")
                .isEmpty();

        assertThat(catchThrowableOf(() -> identity.resolve("0")))
                .as("identity rejects both, because for this endpoint a present-but-broken credential "
                        + "is a different event from an absent one")
                .isNotNull();
        assertThat(catchThrowableOf(() -> identity.resolve("abc"))).isNotNull();
    }

    private static Throwable catchThrowableOf(Runnable action) {
        try {
            action.run();
            return null;
        } catch (Throwable thrown) {
            return thrown;
        }
    }

    private static Object injectedResolver(SignupController controller) throws Exception {
        for (Field field : SignupController.class.getDeclaredFields()) {
            if (field.getName().contains("requesterUserIdResolver")) {
                field.setAccessible(true);
                return field.get(controller);
            }
        }
        throw new AssertionError("SignupController holds no requesterUserIdResolver field");
    }
}
