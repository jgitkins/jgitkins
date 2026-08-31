package io.jgitkins.server.shared.application.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Injects the requester, or null when there is none.
 *
 * <p>A meta-annotation over {@link AuthenticationPrincipal} with no {@code expression}. That absence
 * is the point: {@code expression = "username"} is evaluated by SpEL against whatever object is the
 * principal, and an anonymous request or an OAuth2 session principal has no such property, which
 * produced {@code SpelEvaluationException EL1008E} and a 500 on every route reading the requester
 * ({@code 93294fa}, {@code d74813d}).
 *
 * <h2>Null is the answer, not an error</h2>
 *
 * <p>{@code AuthenticationPrincipalArgumentResolver} returns null when the principal is not
 * assignable to the parameter type, because {@code errorOnInvalidType} defaults to false. So all
 * three cases — no authentication, the anonymous token, an OAuth2 session — arrive as null rather
 * than throwing. Routes that require a requester reject the null themselves and answer 401; routes
 * that read a public repository pass it through, because a public repository is readable anonymously
 * and the visibility rule decides.
 *
 * <p>Leaving {@code errorOnInvalidType} at its default is therefore a decision, not an oversight.
 * Turning it on would restore the class of failure this annotation exists to remove: a principal of
 * an unexpected type would become an exception on a route that had a perfectly good answer for
 * "nobody is logged in".
 */
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
