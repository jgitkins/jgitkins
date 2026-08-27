package io.jgitkins.server.identity.access.adapter.in.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.identity.access.adapter.in.rest.SignupController;
import io.jgitkins.server.identity.access.application.port.in.SignupUseCase;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The controller must hold the identity resolver, not merely a resolver.
 *
 * <p>Two beans of type {@code RequesterUserIdResolver} exist, one per bounded context, with deliberately
 * different error semantics: collaboration's swallows every parse failure into {@code Optional.empty()},
 * and this context's throws. If the controller were wired to the collaboration bean, a malformed
 * principal would produce {@code empty()} and the request would be reported as unauthenticated — the
 * same status the client sees today, so no HTTP test could tell the difference. The distinction is only
 * visible at the injection point, which is why this test looks there.
 *
 * <p>Asserting both bean names exist would not catch it. This resolves the field the controller actually
 * holds.
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
    void identityResolverLoadsWithIdentityBeanName() {
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
        // The reason the wiring assertion above matters, stated as an executable fact rather than as a
        // claim in a comment. If these two ever agreed, the qualifier would be cosmetic.
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
